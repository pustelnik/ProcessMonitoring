package com.monitor.linux;

import akka.actor.*;
import akka.japi.Creator;
import akka.util.Timeout;
import com.jcraft.jsch.JSchException;
import com.monitor.BaseMonitor;
import com.monitor.Monitor;
import com.monitor.common.FileHandler;
import com.monitor.common.MonitorMessages.*;
import com.monitor.common.ProcessIsNotRunningException;
import com.monitor.common.ProcessesMarshaller;
import com.monitor.connection.RemoteClient;
import com.monitor.connection.SshClientImpl;
import com.sun.xml.internal.ws.Closeable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 * @author jakub on 12.08.16.
 */
public class LinuxMonitorImpl extends BaseMonitor implements Monitor {
    static final long START_TIME = System.currentTimeMillis()/1000;
    private static final Logger LOGGER = LogManager.getLogger(LinuxMonitorImpl.class);
    private final ActorSystem system = ActorSystem.create();
    private final ActorRef monitor;

    /**
     * Linux process monitor implementation using SSH and ps command
     * @param targetIp Linux IP or hostname
     * @param user username
     * @param password password
     */
    public LinuxMonitorImpl(String targetIp, String user, String password) {
        monitor = system.actorOf(Props.create(Monitoring.class, targetIp, user, password),"linuxMonitor");
    }

    private class StartMonitoring {
        private final String[] processesToMonitor;

        StartMonitoring(String[] processesToMonitor) {
            this.processesToMonitor = processesToMonitor;
        }

        String[] getProcessesToMonitor() {
            return processesToMonitor;
        }
    }


    @Override
    public void startMonitoring(String[] processToMonitor, int length, TimeUnit timeUnit) {
        system.scheduler().schedule(Duration.Zero(), Duration.create(length, timeUnit), monitor, new StartMonitoring(processToMonitor), system.dispatcher(), ActorRef.noSender());
    }

    @Override
    public void stopMonitoring() {
        saveMonitoringData();
        terminateActorSystem();
    }

    private void terminateActorSystem() {
        Future<Terminated> terminate = system.terminate();
        try {
            Await.result(terminate, Duration.Inf());
        } catch (Exception e) {
            LOGGER.warn(e);
        }
        LOGGER.debug("terminate.isCompleted() = {}", terminate.isCompleted());
    }

    private void saveMonitoringData() {
        Future<Object> future = ask(monitor, new SaveStatistics(), Timeout.apply(Duration.apply(500, TimeUnit.SECONDS)));
        try {
            Await.result(future,Duration.Inf());
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }

    @Override
    public void copyMonitoringResults(String targetPath) throws IOException {
        new FileHandler("processStats.html").copyMonitoringResults(targetPath);
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

    private static class Monitoring extends UntypedActor {
        final ActorRef resultPrinter = getContext().actorOf(Props.create(MonitorResultPrinter.class),"resultPrinter");
        final ActorRef remoteClient;

        @Override
        public SupervisorStrategy supervisorStrategy() {
            return strategy;
        }

        public Monitoring(String targetIp, String user, String password) {
            remoteClient = getContext().actorOf(SshActor.props(targetIp,user,password));
        }

        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof StartMonitoring) {
                for (String process : ((StartMonitoring) message).getProcessesToMonitor()) {
                    remoteClient.tell(new SendRemoteCommand(String.format("ps aux | grep %s | head -n 3",process)),getSelf());
                }
            } else if(message instanceof Response) {
                resultPrinter.tell(new RemoteLinuxCommandResult(((Response) message).getResponse()),getSelf());
            } else if(message instanceof SaveStatistics) {
                resultPrinter.forward(message,getContext());
            } else {
                unhandled(message);
            }
        }
    }

    private static class MonitorResultPrinter extends UntypedActor {
        private final Map<String, List<LinuxPs>> psMap = new HashMap<>();

        @Override
        public void onReceive(Object message) throws Throwable {
            if (message instanceof RemoteLinuxCommandResult) {
                try {
                    String val = ((RemoteLinuxCommandResult) message).getResult();
                    LinuxPs linuxPs = LinuxPs.GetPs(val);
                    if(!psMap.containsKey(linuxPs.getCommand())) {
                        List<LinuxPs> processList = new ArrayList<>();
                        processList.add(linuxPs);
                        psMap.put(linuxPs.getCommand(),processList);
                    } else {
                        psMap.get(linuxPs.getCommand()).add(linuxPs);
                    }
                    LOGGER.debug(linuxPs);
                } catch (ProcessIsNotRunningException e) {
                    LOGGER.warn(e);
                }

            } else if(message instanceof SaveStatistics) {
                Processes processes = new Processes();
                List<Processes.ProcessStats> processStatsList = new LinkedList<>();
                psMap.keySet().forEach(processName -> {
                    final Processes.ProcessStats processStats = new Processes.ProcessStats();
                    final List<String> time = new ArrayList<>();
                    final List<String> cpu = new ArrayList<>();
                    final List<String> mem = new ArrayList<>();
                    final List<String> vsz = new ArrayList<>();
                    final List<String> rss = new ArrayList<>();
                    processStats.setProcessName(processName);

                    psMap.get(processName).forEach(linuxPs -> {
                        time.add(linuxPs.getTime());
                        cpu.add(linuxPs.getCpu());
                        mem.add(linuxPs.getMem());
                        vsz.add(linuxPs.getVsz());
                        rss.add(linuxPs.getRss());
                    });
                    processStats.setCpu(cpu);
                    processStats.setMem(mem);
                    processStats.setTime(time);
                    processStats.setVsz(vsz);
                    processStats.setRss(rss);
                    processStatsList.add(processStats);
                });
                Collections.sort(processStatsList);
                processes.setProcesses(processStatsList);
                ProcessesMarshaller.SaveProcessesAsJson(new Class[]{Processes.class, Processes.ProcessStats.class},processes);
                getSender().tell(new ResultsSaved(),getSelf());
            }
        }
    }

    private static class SshActor extends UntypedActor {
        private final RemoteClient sshClient;
        private Connected connected = null;

        SshActor(String targetIp, String user, String password) throws JSchException {
            sshClient = new SshClientImpl(targetIp,user,password);
        }

        static Props props(final String targetIp, final String user, final String password) {
            return Props.create(new Creator<Actor>() {
                private static final long serialVersionUID = 1L;
                @Override
                public Actor create() throws Exception {
                    return new SshActor(targetIp,user,password);
                }
            });
        }

        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof SendRemoteCommand) {
                if(connected == null) {
                    sshClient.connect();
                    connected = new Connected();
                }
                Response response = new Response(sshClient.sendCommand(((SendRemoteCommand) message).getCommand()));
                getSender().tell(response,getSelf());
            } else if(message instanceof PoisonPill) {
                if(sshClient instanceof Closeable) {
                    ((Closeable) sshClient).close();
                }
            } else {
                unhandled(message);
            }
        }
    }

}
