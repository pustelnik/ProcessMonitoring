package com.monitor.connection;

import com.jcraft.jsch.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;

/**
 * @author  jakub on 12.08.16.
 */
public class SshClientImpl implements RemoteClient, Closeable {
    private final JSch jSch = new JSch();
    private final String ip;
    private final String user;
    private final String password;
    private Session session;

    public SshClientImpl(String ip, String user, String password) throws JSchException {
        this.ip = ip;
        this.user = user;
        this.password = password;
    }

    @Override
    public void connect() throws ConnectionException {
        try {
            session = jSch.getSession(user,ip,22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
        } catch (JSchException e) {
            throw new ConnectionException(e);
        }
    }

    @Override
    public String sendCommand(String command) {
        Channel channel = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            PipedOutputStream pop = new PipedOutputStream();
            channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            ((ChannelExec) channel).setErrStream(System.err);
            channel.setOutputStream(pop);
            InputStream in = channel.getInputStream();
            channel.connect();
            byte[] tmp=new byte[1024];
            while(true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    String str = new String(tmp, 0, i);
                    stringBuilder.append(str);
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    if(channel.getExitStatus() != 0) {
                        System.out.println("channel.getExitStatus() = " + channel.getExitStatus());
                    }
                    break;
                }
            }
        } catch (JSchException | IOException e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public void close() throws IOException {
        session.disconnect();
    }
}
