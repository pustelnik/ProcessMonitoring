package com.monitor.connection;

/**
 * Created by jakub on 19.08.16.
 */
public class RemoteCommandNotSendException extends Exception {

    public RemoteCommandNotSendException(String message) {
        super(message);
    }

    public RemoteCommandNotSendException(Throwable cause) {
        super(cause);
    }
}
