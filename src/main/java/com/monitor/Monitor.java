package com.monitor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author jakub on 12.08.16.
 */
public interface Monitor {

    /**
     * Starts monitoring system process status for given processes to monitor list.
     * Ends only when stopMonitoring() is called.
     *
     * As a result html line diagram is generated with all gathered process data.
     *
     * @param processToMonitor process names to monitor
     * @param length (request interval) interval between process status requests
     * @param timeUnit time unit for request interval
     */
    void startMonitoring(String[] processToMonitor, int length, TimeUnit timeUnit);
    void stopMonitoring();

    /**
     * Copy html charts diagrams generated by startMonitoring() and stopMonitoring() to
     * the given targetPath.
     * @param targetPath path where report files should be copied. Should not include any filename
     * @throws IOException If targetPath is wrong or other IO exception occur
     */
    void copyMonitoringResults(String targetPath) throws IOException;
}