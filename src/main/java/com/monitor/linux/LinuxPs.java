package com.monitor.linux;

import com.monitor.common.ProcessIsNotRunningException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;

import static com.monitor.linux.LinuxMonitorImpl.START_TIME;

/**
 * linux command ps aux object representation
 */
class LinuxPs implements Comparable<LinuxPs>, Serializable {
    private static final Logger LOGGER = LogManager.getLogger(LinuxPs.class);
    private final String user, pid, cpu, mem, vsz, rss, tty, stat, start, time, command;

    private LinuxPs(String user, String pid, String cpu, String mem, String vsz, String rss, String tty, String stat, String start, String time, String command) {
        this.user = user;
        this.pid = pid;
        this.cpu = cpu;
        this.mem = mem;
        this.vsz = vsz;
        this.rss = rss;
        this.tty = tty;
        this.stat = stat;
        this.start = start;
        this.time = time;
        this.command = command;
    }

    public String getUser() {
        return user;
    }

    private int getPid() {
        return Integer.parseInt(pid);
    }

    String getCpu() {
        return cpu;
    }

    String getMem() {
        return mem;
    }

    String getVsz() {
        return vsz;
    }

    String getRss() {
        return rss;
    }

    String getTty() {
        return tty;
    }

    String getStat() {
        return stat;
    }

    String getStart() {
        return start;
    }

    String getTime() {
        return time;
    }

    String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return String.format("%s\t|%s\t|%s\t|%s\t|%s\t|%s\t|%s\t|%s\t|%s\t|%s\t|%s",
                user,pid,cpu,mem,vsz,rss,tty,stat,start,time,command);
    }

    static LinuxPs GetPs(String psAux) throws ProcessIsNotRunningException {
        StringBuilder result = new StringBuilder();
        removeResultsWithGrep(psAux, result);
        result = removeAdditionalLines(result);
        if(StringUtils.isBlank(result.toString()) || result.toString().contains("grep")) {
            throw new ProcessIsNotRunningException(psAux);
        } else {
            psAux = result.toString();
        }
        String[] process = psAux.replaceAll("  *"," ").split(" ",11);
        String command = (process[10].replaceAll("\n", "")).split(" ")[0];
        if(command.contains(process[0])) {
            LOGGER.error("Command before changes {} Command after changes {}",process[10],command);
        }
        return new LinuxPs(
                process[0], // user
                process[1], // pid
                process[2], // cpu
                process[3], // mem
                process[4], // vsz
                process[5], // rss
                process[6], // tty
                process[7], // stat
                process[8], // start
                String.valueOf((System.currentTimeMillis()/1000)-START_TIME), // time in seconds
                command); //command
    }

    /**
     * leaves only first line of results in case if there are more then one results
     * @param result ps aux ... | grep ... command result
     * @return Returns first line of given String. Lines > 1 will be removed from result parameter.
     */
    private static StringBuilder removeAdditionalLines(StringBuilder result) {
        if(result.toString().matches("(.*\n)*")) {
            String temp = result.toString();
            String[] temp2 = temp.split("\n");
            result = new StringBuilder();
            if(temp2.length > 0) {
                result.append(temp2[0]);
            }
        }
        return result;
    }

    private static void removeResultsWithGrep(String psAux, StringBuilder result) {
        if(psAux.matches(".*\n.*\n.*\n")) {
            String[] temp = psAux.split("\n");
            for (String s : temp) {
                if(!s.contains("grep")) {
                    result.append(s).append("\n");
                }
            }
        }
    }

    @Override
    public int compareTo(LinuxPs o) {
        if(getPid() == o.getPid()) {
            return 0;
        } else if(getPid() > o.getPid()) {
            return 1;
        } else {
            return -1;
        }
    }
}
