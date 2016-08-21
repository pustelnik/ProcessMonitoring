package com.monitor.connection;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * @author jakub on 17.08.16.
 */
public class TelnetClientImpl implements RemoteClient, Closeable {

    private static final Logger LOGGER = LogManager.getLogger(TelnetClientImpl.class);
    private static final String PROMPT = ">";
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final TelnetClient telnetClient = new TelnetClient("");
    private InputStream in;
    private OutputStream out;

    public TelnetClientImpl(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void connect() throws TelnetConnectionException {
        try {
            LOGGER.debug("Connecting to host {} at port {}",host,port);
            telnetClient.connect(host,port);
            LOGGER.debug("Telnet connected");
            in = telnetClient.getInputStream();
            out = telnetClient.getOutputStream();
            LOGGER.debug("Writing login");
            writeLogin();
            LOGGER.debug("Writing password");
            writePassword();
            LOGGER.debug("Checking authentication status");
            checkAuthenticationStatus();
            LOGGER.debug("Telnet authentication succeded");
        } catch (IOException e) {
            throw new TelnetConnectionException(e);
        }
    }

    private void checkAuthenticationStatus() throws IOException, TelnetConnectionException {
        String result = isOutputEndingWith(new String[]{": ",">"});
        if(result.contains("Login Failed")) {
            throw new TelnetLoginFailedException();
        } else if(result.contains("has closed the connection")){
            throw new TelnetConnectionClosed();
        }

    }

    private void writePassword() throws IOException, TelnetConnectionException {
        isOutputEndingWith(new String[]{"password: "});
        write(password);
    }

    private void writeLogin() throws IOException, TelnetConnectionException {
        isOutputEndingWith(new String[]{"login: "});
        write(username);
    }

    @Override
    public String sendCommand(String command) throws RemoteCommandNotSendException {
        LOGGER.debug("Executing command {}",command);
        try {
            write(command);
            String output = isOutputEndingWith(new String[]{PROMPT});
            output = StringUtils.removePattern(output,"\n[A-Z]:\\\\.*>");
            output = StringUtils.remove(output, command);
            // convert windows newline for linux newline, remove unnecessary new lines
            output = output.
                    replaceAll("\r\n","\n").
                    replaceAll("\r","\n").
                    replaceAll("\n\n","\n").
                    replaceAll("\n\n","").
                    replaceAll("\r\r","\n");
            LOGGER.debug("Output {}",output);
            return output;
        } catch (IOException | TelnetConnectionException e) {
            throw new RemoteCommandNotSendException(e);
        }
    }

    private String isOutputEndingWith(String[] outputs) throws IOException, TelnetConnectionException {
        int timeoutSec = 60;
        long start = System.currentTimeMillis()/1000;
        String result;
        while((System.currentTimeMillis()/1000 - start) < timeoutSec) {
            result = read();
            for (String output : outputs) {
                if(result.endsWith(output)) {
                    return result;
                }
            }
            sleep(500);
        }
        throw new TelnetConnectionException("Timeout occurred while waiting for output");
    }

    private void write(String value) throws IOException {
        out.write((value+"\n").getBytes());
        out.flush();
    }

    private String read() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        StringBuilder result = new StringBuilder();
        while(bufferedReader.ready())
            result.append(((char) bufferedReader.read()));
        return new String(result.toString().getBytes(),"UTF-8");
    }

    private void sleep(int ms) {
//        LOGGER.debug("Sleeping for {} ms",ms);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LOGGER.debug(e);
        }
    }

    @Override
    public void close() throws IOException {
        telnetClient.disconnect();
    }

    public class TelnetConnectionException extends ConnectionException {
        TelnetConnectionException(Object e) { }

        TelnetConnectionException(String message) {
            super(message);
        }

        TelnetConnectionException() {
            super();
        }
    }

    private class TelnetLoginFailedException extends TelnetConnectionException {
        public TelnetLoginFailedException(IOException e) {
            super(e);
        }

        public TelnetLoginFailedException() {
            super();
        }
    }

    private class TelnetConnectionClosed extends TelnetConnectionException {
    }
}
