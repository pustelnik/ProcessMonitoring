package com.monitor.connection;

/**
 * @author jakub on 21.08.16.
 */
public class ConnectionException extends Exception {

    public ConnectionException() {
        super();
    }

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(Throwable cause) {
        super(cause);
    }
}
