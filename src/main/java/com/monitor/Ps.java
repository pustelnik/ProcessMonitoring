package com.monitor;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * linux command ps aux object representation
 */
class Ps implements Comparable<Ps>, Serializable {
    private final String user, pid, cpu, mem, vsz, rss, tty, stat, start, time, command;

    private Ps(String user, String pid, String cpu, String mem, String vsz, String rss, String tty, String stat, String start, String time, String command) {
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

    static Ps GetPs(String psAux) {
        String[] split = psAux.replaceAll("  *"," ").split(" ",11);
        return new Ps(
                split[0], // user
                split[1], // pid
                split[2], // cpu
                split[3], // mem
                split[4], // vsz
                split[5], // rss
                split[6], // tty
                split[7], // stat
                split[8], // start
                LocalTime.now().toString(), // time
                split[10].replaceAll("\n","")); //command
    }

    @Override
    public int compareTo(Ps o) {
        if(getPid() == o.getPid()) {
            return 0;
        } else if(getPid() > o.getPid()) {
            return 1;
        } else {
            return -1;
        }
    }
}
