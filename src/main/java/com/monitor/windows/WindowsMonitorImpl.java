package com.monitor.windows;

import akka.actor.*;
import akka.japi.Creator;
import akka.util.Timeout;
import com.monitor.Monitor;
import com.monitor.common.FileHandler;
import com.monitor.common.MonitorMessages.Response;
import com.monitor.common.MonitorMessages.ResultsSaved;
import com.monitor.common.MonitorMessages.SaveStatistics;
import com.monitor.common.MonitorMessages.SendRemoteCommand;
import com.monitor.common.ProcessIsNotRunningException;
import com.monitor.common.ProcessesMarshaller;
import com.monitor.connection.RemoteClient;
import com.monitor.connection.TelnetClientImpl;
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
 * @author jakub on 19.08.16.
 */
public class WindowsMonitorImpl implements Monitor {
    private static final Logger LOGGER = LogManager.getLogger(WindowsMonitorImpl.class);
    private final ActorSystem system = ActorSystem.create();
    private final ActorRef monitor;

    public WindowsMonitorImpl(String host, int port, String usr, String pwd) {
        monitor = system.actorOf(Props.create(WindowsMonitor.class, host, port, usr, pwd),"windowsMonitor");
    }

    private static class Results {
        private final Map<String, List<WindowsPs>> psMap = new HashMap<>();

        public Map<String, List<WindowsPs>> getPsMap() {
            return psMap;
        }
    }

    private static class StartMonitoring {
        private final String[] processToMonitor;

        public StartMonitoring(String[] processToMonitor) {
            this.processToMonitor = processToMonitor;
        }

        public String[] getProcessToMonitor() {
            return processToMonitor;
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
        LOGGER.debug("Started to terminate actor system");
        Future<Terminated> terminate = system.terminate();
        try {
            Await.result(terminate, Duration.Inf());
        } catch (Exception e) {
            LOGGER.warn(e);
        }
        LOGGER.debug("terminate.isCompleted() = {}", terminate.isCompleted());
    }

    private void saveMonitoringData() {
        Future<Object> saveStatistics = ask(monitor, new SaveStatistics(), Timeout.apply(500, TimeUnit.SECONDS));
        try {
            Await.result(saveStatistics, Duration.Inf());
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }

    @Override
    public void copyMonitoringResults(String targetPath) throws IOException {
        new FileHandler("windowsProcessStats.html").copyMonitoringResults(targetPath);
    }

    // todo add fault strategy
    private static class WindowsMonitor extends UntypedActor {
        private final ActorRef results = getContext().actorOf(Props.create(MonitorResultKeeper.class),"resultsKeeper");
        private final ActorRef telnet;

        public WindowsMonitor(String host, int port, String usr, String pwd) {
            telnet = getContext().actorOf(TelnetActor.props(host,port,usr,pwd),"telnetClient");
        }

        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof StartMonitoring) {
                for (String process : ((StartMonitoring) message).getProcessToMonitor()) {
                    telnet.tell(new SendRemoteCommand(String.format("powershell.exe ps -Name '%s'",process)),getSelf());
                }
            } else if(message instanceof Response) {
                results.tell(message,getSelf());
            } else if(message instanceof SaveStatistics) {
                results.forward(message,getContext());
            } else {
                unhandled(message);
            }
        }
    }

    private static class MonitorResultKeeper extends UntypedActor {
        private final ActorRef resultWriter = getContext().actorOf(Props.create(MonitorResultWriter.class),"resultWriter");
        private final Results results = new Results();

        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof Response) {
                try {
                    WindowsPs ps = WindowsPs.GetPs(((Response) message).getResponse());
                    Map<String, List<WindowsPs>> psMap = results.getPsMap();
                    if(!psMap.containsKey(ps.getProcessName())) {
                        List<WindowsPs> processList = new ArrayList<>();
                        processList.add(ps);
                        psMap.put(ps.getProcessName(),processList);
                    } else {
                        psMap.get(ps.getProcessName()).add(ps);
                    }
                } catch (ProcessIsNotRunningException e) {
                    LOGGER.warn(e);
                }

            } else if(message instanceof SaveStatistics) {
                resultWriter.forward(results,getContext());
            } else {
                unhandled(message);
            }
        }
    }

    private static class MonitorResultWriter extends UntypedActor {

        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof Results) {
                LOGGER.debug("Saving results");
                Processes processes = new Processes();
                List<Processes.ProcessStats> processStatsList = new LinkedList<>();
                Map<String, List<WindowsPs>> psMap = ((Results) message).getPsMap();
                psMap.keySet().forEach(processName -> {
                    final Processes.ProcessStats processStats = new Processes.ProcessStats();
                    final List<String> npm = new ArrayList<>();
                    final List<String> pm = new ArrayList<>();
                    final List<String> ws = new ArrayList<>();
                    final List<String> vm = new ArrayList<>();
                    final List<String> cpu = new ArrayList<>();
                    final List<String> time = new ArrayList<>();
                    processStats.setProcessName(processName);

                    psMap.get(processName).forEach(windowsPs -> {
                        npm.add(windowsPs.getNpm());
                        pm.add(windowsPs.getPm());
                        ws.add(windowsPs.getWs());
                        vm.add(windowsPs.getVm());
                        cpu.add(windowsPs.getCpu());
                        time.add(windowsPs.getTime());
                    });
                    processStats.setCpu(cpu);
                    processStats.setNpm(npm);
                    processStats.setPm(pm);
                    processStats.setWs(ws);
                    processStats.setVm(vm);
                    processStats.setTime(time);
                    processStatsList.add(processStats);
                });
                Collections.sort(processStatsList);
                processes.setProcesses(processStatsList);
                ProcessesMarshaller.SaveProcessesAsJson(new Class[]{Processes.class, Processes.ProcessStats.class},processes);
                sender().tell(new ResultsSaved(),getSelf());
            } else {
                unhandled(message);
            }
        }
    }

    private static class TelnetActor extends UntypedActor {
        private final RemoteClient client;

        TelnetActor(String host, int port, String usr, String pwd) throws Exception {
            client = new TelnetClientImpl(host,port,usr,pwd);
            try {
                ((TelnetClientImpl)client).connect();
            } catch (TelnetClientImpl.TelnetConnectionException e) {
                throw new Exception(e);
            }

        }

        static Props props(final String host, final int port, final String usr, final String pwd) {
            return Props.create(new Creator<TelnetActor>() {
                private static final long serialVersionUID = 1L;
                @Override
                public TelnetActor create() throws Exception {
                    return new TelnetActor(host,port,usr,pwd);
                }
            });
        }

        @Override
        public void onReceive(Object message) throws Throwable {
            if(message instanceof SendRemoteCommand) {
                Response response = new Response(client.sendCommand(((SendRemoteCommand) message).getCommand()));
                getSender().tell(response,getSelf());
            } else if(message instanceof PoisonPill) {
                if(client instanceof Closeable) {
                    ((Closeable) client).close();
                }
            } else {
                unhandled(message);
            }
        }
    }

}
