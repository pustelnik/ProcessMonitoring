package com.monitor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jakub on 14.08.16.
 */
@XmlRootElement
class Processes implements Serializable {
    static class ProcessStats {
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

    static Marshaller ProcessesMarshallel() throws JAXBException {
        Map<String, Object> properties = new HashMap<String, Object>(2);
        properties.put("eclipselink.media-type", "application/json");
        properties.put("eclipselink.json.include-root", false);
        JAXBContext jaxbContext = JAXBContext.newInstance(new Class[]{ProcessStats.class,Processes.class},properties);
        return jaxbContext.createMarshaller();
    }
}
