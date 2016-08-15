package com.monitor.connection;

/**
 * @author jakub on 12.08.16.
 */
public interface SSHClient {
    String sendCommand(String command);
}
