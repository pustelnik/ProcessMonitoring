package com.monitor;

import akka.actor.*;
import com.monitor.connection.SSHClient;
import com.monitor.connection.SSHClientImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author jakub on 12.08.16.
 */
public class MonitorImpl implements Monitor {
    private static Logger LOGGER = LogManager.getLogger(MonitorImpl.class);
    private SSHClientImpl ssh;
    private final ActorSystem system = ActorSystem.create();
    private final ActorRef monitor = system.actorOf(Props.create(Monitoring.class),"linuxMonitor");

    /**
     * Linux process monitor implementation using SSH and ps command
     * @param targetIp Linux IP or hostname
     * @param user username
     * @param password password
     */
    public MonitorImpl(String targetIp, String user, String password) {
        this.ssh = new SSHClientImpl(targetIp, user, password);
    }

    private class StartMonitoring {
        private final SSHClient sshClient;
        private final String[] processesToMonitor;

        StartMonitoring(SSHClient sshClient, String[] processesToMonitor) {
            this.sshClient = sshClient;
            this.processesToMonitor = processesToMonitor;
        }

        SSHClient getSshClient() {
            return sshClient;
        }

        String[] getProcessesToMonitor() {
            return processesToMonitor;
        }
    }
    private static class SaveStatistics {}

    @Override
    public void startMonitoring(String[] processToMonitor, int length, TimeUnit timeUnit) {
        system.scheduler().schedule(Duration.Zero(), Duration.create(length, timeUnit), monitor, new StartMonitoring(ssh,processToMonitor), system.dispatcher(), ActorRef.noSender());
    }

    @Override
    public void stopMonitoring() {
        monitor.tell(new SaveStatistics(),ActorRef.noSender());
        Future<Terminated> terminate = system.terminate();
        try {
            Await.result(terminate,Duration.Inf());
        } catch (Exception e) {
            LOGGER.warn(e);
        }
        LOGGER.debug("terminate.isCompleted() = {}", terminate.isCompleted());
        try {
            ssh.close();
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public void copyMonitoringResults(String targetPath) throws IOException {
        if(targetPath == null || targetPath.isEmpty() || targetPath.equals(" ")) {
            throw new IOException("Invalid path");
        }
        if (!targetPath.endsWith("/")) {
            targetPath += "/";
        }
        try {
            Files.createDirectories(new File(targetPath+"charts").toPath());
        } catch (FileAlreadyExistsException e) {
            LOGGER.debug(e);
        }
        File file1target = new File(targetPath+"charts/processStats.html");
        File file2target = new File(targetPath+"charts/Chart.bundle.js");
        File file3target = new File(targetPath+"charts/data.js");

        File file1source = new File("target/classes/processCharts/processStats.html");
        File file2source = new File("target/classes/processCharts/Chart.bundle.js");

        if(Files.notExists(file1source.toPath()) || Files.notExists(file2source.toPath())) {
            copyTemplateResourcesFromJar(file1source, file2source);
        }
        Files.copy(file1source.toPath(), file1target.toPath(), REPLACE_EXISTING);
        Files.copy(file2source.toPath(), file2target.toPath(), REPLACE_EXISTING);
        Files.copy(new File("target/classes/processCharts/data.js").toPath(), file3target.toPath(), REPLACE_EXISTING);
    }

    /**
     * Implementation:
     * http://docs.oracle.com/javase/8/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
     *
     * @param file1target Target file to copy first resource
     * @param file2target Target file to copy second resource
     * @throws IOException If file does not exists
     */
    private void copyTemplateResourcesFromJar(File file1target, File file2target) throws IOException {
        try {
            Files.createDirectory(new File("target/classes/processCharts").toPath());
        } catch (FileAlreadyExistsException e) {
            LOGGER.debug(e);
        }
        String pathToResource = getClass().getResource("/processCharts/processStats.html").getPath();
        URI uri1 = URI.create("jar:" + pathToResource);
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        final FileSystem zipFs = FileSystems.newFileSystem(uri1,env);
        try {
            Path pathInZipfile1 = zipFs.getPath("/processCharts/processStats.html");
            Path pathInZipfile2 = zipFs.getPath("/processCharts/Chart.bundle.js");
            Files.copy(pathInZipfile1,file1target.toPath(),REPLACE_EXISTING);
            Files.copy(pathInZipfile2,file2target.toPath(),REPLACE_EXISTING);
        } catch (FileNotFoundException e) {
            LOGGER.debug(e);
        }
    }

    private static class RemoteLinuxCommandResult {
        private final String result;

        RemoteLinuxCommandResult(String result) {
            this.result = result;
        }

        String getResult() {
            return result;
        }
    }

    static class Monitoring extends UntypedActor {
        final ActorRef resultPrinter = getContext().actorOf(Props.create(MonitorResultPrinter.class),"resultPrinter");
        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof StartMonitoring) {
                for (String process : ((StartMonitoring) message).getProcessesToMonitor()) {
                    String results = ((StartMonitoring) message).getSshClient().sendCommand(String.format("ps aux | grep %s | head -n 1",process));
                    resultPrinter.tell(new RemoteLinuxCommandResult(results),getSelf());
                }
            } else if(message instanceof SaveStatistics) {
                resultPrinter.forward(message,getContext());
            } else {
                unhandled(message);
            }
        }
    }

    private static class MonitorResultPrinter extends UntypedActor {
        private final Map<String, List<Ps>> psMap = new HashMap<>();

        @Override
        public void onReceive(Object message) throws Throwable {
            if (message instanceof RemoteLinuxCommandResult) {
                String val = ((RemoteLinuxCommandResult) message).getResult();
                Ps ps = Ps.GetPs(val);
                if(!psMap.containsKey(ps.getCommand())) {
                    List<Ps> processList = new ArrayList<>();
                    processList.add(ps);
                    psMap.put(ps.getCommand(),processList);
                } else {
                    psMap.get(ps.getCommand()).add(ps);
                }
                LOGGER.debug(ps);
            } else if(message instanceof SaveStatistics) {
                Processes processes = new Processes();
                List<Processes.ProcessStats> processStatsList = new ArrayList<>();
                psMap.keySet().forEach(processName -> {
                    final Processes.ProcessStats processStats = new Processes.ProcessStats();
                    final List<String> time = new ArrayList<>();
                    final List<String> cpu = new ArrayList<>();
                    final List<String> mem = new ArrayList<>();
                    final List<String> vsz = new ArrayList<>();
                    final List<String> rss = new ArrayList<>();
                    processStats.setProcessName(processName);

                    psMap.get(processName).forEach(ps -> {
                        time.add(ps.getTime());
                        cpu.add(ps.getCpu());
                        mem.add(ps.getMem());
                        vsz.add(ps.getVsz());
                        rss.add(ps.getRss());
                    });
                    processStats.setCpu(cpu);
                    processStats.setMem(mem);
                    processStats.setTime(time);
                    processStats.setVsz(vsz);
                    processStats.setRss(rss);
                    processStatsList.add(processStats);
                });
                processes.setProcesses(processStatsList);
                Files.createDirectories(new File("target/classes/processCharts/").toPath());
                File file = new File(String.format("target/processes_%d.json", System.currentTimeMillis() / 1000));
                Processes.ProcessesMarshallel().marshal(processes,new FileWriter(file));
                FileWriter fileWriter = new FileWriter(new File("target/classes/processCharts/data.js"));
                StringBuilder json = new StringBuilder();
                Files.readAllLines(file.toPath(), Charset.defaultCharset()).forEach(json::append);
                fileWriter.append("var processStat = ");
                fileWriter.append(json.toString());
                fileWriter.append(";");
                fileWriter.close();
            }
        }
    }

}
