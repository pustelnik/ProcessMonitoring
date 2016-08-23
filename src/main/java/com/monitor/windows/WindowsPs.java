package com.monitor.windows;

import com.monitor.common.ProcessIsNotRunningException;

import java.io.Serializable;

import static com.monitor.windows.WindowsMonitorImpl.START_TIME;

/**
 * @author jakub on 19.08.16.
 *
 * Windows PowerShell Get-Processes (ps) command object representation
 * https://technet.microsoft.com/en-us/library/hh849832.aspx
 */
public class WindowsPs implements Comparable<WindowsPs>, Serializable{
    private final String npm, pm, ws, vm, cpu, processName, time;
    private final long id;

    /**
     *
     * @param npm The amount of non-paged memory that the process is using, in kilobytes.
     * @param pm The amount of pageable memory that the process is using, in kilobytes.
     * @param ws The size of the working set of the process, in kilobytes. The working set consists of the pages of memory that were recently referenced by the process.
     * @param vm The amount of virtual memory that the process is using, in megabytes. Virtual memory includes storage in the paging files on disk.
     * @param cpu The amount of processor time that the process has used on all processors, in seconds.
     * @param processName The name of the process.
     * @param id The process ID (PID) of the process.
     */
    private WindowsPs(String npm, String pm, String ws, String vm, String cpu, String processName, long id) {
        this.npm = npm;
        this.pm = pm;
        this.ws = ws;
        this.vm = vm;
        this.cpu = cpu;
        this.processName = processName;
        this.id = id;
        this.time = String.valueOf((System.currentTimeMillis()/1000)-START_TIME); // time in seconds
    }

    /**
     * Takes ps param as a String representation of one row of ps -Name 'name' coommand
     * Handles  NPM(K)    PM(K)      WS(K) VM(M)   CPU(s)     Id ProcessName
     * -------  ------    -----      ----- -----   ------     -- -----------
     *       9       2     1424       1544     9     0,00   1140 cmd
     * @param ps ps command results, should look like described before
     * @return WindowsPs
     */
    public static WindowsPs GetPs(String ps) throws ProcessIsNotRunningException {
        if(ps.contains("Cannot find a process with the name")) {
            throw new ProcessIsNotRunningException(ps);
        }
        ps = ps.replaceAll("�","");
        if(ps.contains("Handles")) {
            // remove first two lines
            ps = ps.substring(ps.lastIndexOf("-")+1).replaceAll("\n","");
        }
        String[] result = ps.replaceAll("  *", " ").replaceAll("^ ","").split(" ",9);
        return new WindowsPs(
                result[1], // NPM
                result[2], // PM
                result[3], // WS
                result[4], // VM
                // replaceAll is necessary to correctly covert ps output to JavaScript float format
                result[5].replaceAll(",","."), // CPU(s)
                result[7], // ProcessName
                Long.valueOf(result[6]) // ID
        );
    }

    public String getNpm() {
        return npm;
    }

    public String getPm() {
        return pm;
    }

    public String getWs() {
        return ws;
    }

    public String getVm() {
        return vm;
    }

    public String getCpu() {
        return cpu;
    }

    public String getTime() {
        return time;
    }

    public String getProcessName() {
        return processName;
    }

    public long getId() {
        return id;
    }

    @Override
    public int compareTo(WindowsPs o) {
        if(o.getId() == getId()) {
            return 0;
        } else if(o.getId() > getId()) {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s %s %s %s",npm, pm, ws, vm, cpu, processName,id);
    }
}
