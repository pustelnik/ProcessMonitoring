package com.monitor.connection;

/**
 * @author jakub on 12.08.16.
 */
public interface RemoteClient {
    String sendCommand(String command) throws RemoteCommandNotSendException;
    void connect() throws ConnectionException;
}
