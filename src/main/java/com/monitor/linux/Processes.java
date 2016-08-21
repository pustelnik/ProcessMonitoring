package com.monitor.linux;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jakub on 14.08.16.
 */
@XmlRootElement
class Processes implements Serializable {
    @XmlType(propOrder = {"processName","time","cpu","mem","vsz","rss"})
    static class ProcessStats implements Comparable<ProcessStats> {
        private String processName;
        private List<String> time;
        private List<String> cpu;
        private List<String> mem;
        private List<String> vsz;
        private List<String> rss;

        public ProcessStats(String processName, List<String> time, List<String> cpu, List<String> mem, List<String> vsz, List<String> rss) {
            this.processName = processName;
            this.time = time;
            this.cpu = cpu;
            this.mem = mem;
            this.vsz = vsz;
            this.rss = rss;
        }

        ProcessStats() {
        }

        public String getProcessName() {
            return processName;
        }

        void setProcessName(String processName) {
            this.processName = processName;
        }

        public List<String> getCpu() {
            return cpu;
        }

        void setCpu(List<String> cpu) {
            this.cpu = cpu;
        }

        public List<String> getMem() {
            return mem;
        }

        void setMem(List<String> mem) {
            this.mem = mem;
        }

        public List<String> getVsz() {
            return vsz;
        }

        void setVsz(List<String> vsz) {
            this.vsz = vsz;
        }

        public List<String> getRss() {
            return rss;
        }

        void setRss(List<String> rss) {
            this.rss = rss;
        }

        public List<String> getTime() {
            return time;
        }

        public void setTime(List<String> time) {
            this.time = time;
        }

        @Override
        public int compareTo(ProcessStats o) {
            if(o.getTime().size() == getTime().size()) {
                return 0;
            } else if(o.getTime().size() > getTime().size()) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    private List<ProcessStats> processes = new ArrayList<>();

    Processes() {
    }

    public List<ProcessStats> getProcesses() {
        return processes;
    }

    void setProcesses(List<ProcessStats> processes) {
        this.processes = processes;
    }

}
