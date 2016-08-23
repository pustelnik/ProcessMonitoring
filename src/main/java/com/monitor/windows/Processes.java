package com.monitor.windows;

import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.List;

/**
 * @author jakub on 19.08.16.
 */
public class Processes implements Serializable {
    @XmlType(propOrder = {"processName","time","npm","pm","ws","vm","cpu"})
    static class ProcessStats implements Comparable<ProcessStats> {
        private String processName;
        private List<String> npm;
        private List<String> pm;
        private List<String> ws;
        private List<String> vm;
        private List<String> cpu;
        private List<String> time;

        public ProcessStats() {
        }

        public ProcessStats(String processName, List<String> npm, List<String> pm, List<String> ws, List<String> vm, List<String> cpu, List<String> time) {
            this.processName = processName;
            this.npm = npm;
            this.pm = pm;
            this.ws = ws;
            this.vm = vm;
            this.cpu = cpu;
            this.time = time;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public List<String> getNpm() {
            return npm;
        }

        public void setNpm(List<String> npm) {
            this.npm = npm;
        }

        public List<String> getPm() {
            return pm;
        }

        public void setPm(List<String> pm) {
            this.pm = pm;
        }

        public List<String> getWs() {
            return ws;
        }

        public void setWs(List<String> ws) {
            this.ws = ws;
        }

        public List<String> getVm() {
            return vm;
        }

        public void setVm(List<String> vm) {
            this.vm = vm;
        }

        public List<String> getCpu() {
            return cpu;
        }

        public void setCpu(List<String> cpu) {
            this.cpu = cpu;
        }

        public List<String> getTime() {
            return time;
        }

        public void setTime(List<String> time) {
            this.time = time;
        }

        @Override
        public int compareTo(ProcessStats o) {
            if(o.getTime().isEmpty() && getTime().isEmpty()) {
                return 0;
            } else if(!o.getTime().isEmpty() && o.getTime().isEmpty()) {
                return 1;
            } else if(o.getTime().isEmpty() && !o.getTime().isEmpty()) {
                return -1;
            } else {
                long a = Long.parseLong(o.getTime().get(0));
                long b = Long.parseLong(getTime().get(0));
                if(a == b) {
                    return 0;
                } else if(a > b) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }

    private List<ProcessStats> processes;

    public Processes() {
    }

    public Processes(List<ProcessStats> processes) {
        this.processes = processes;
    }

    public List<ProcessStats> getProcesses() {
        return processes;
    }

    public void setProcesses(List<ProcessStats> processes) {
        this.processes = processes;
    }

}
