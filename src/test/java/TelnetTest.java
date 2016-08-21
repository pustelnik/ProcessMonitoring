import com.monitor.connection.RemoteClient;
import com.monitor.connection.RemoteCommandNotSendException;
import com.monitor.connection.TelnetClientImpl;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author jakub on 17.08.16.
 */
public class TelnetTest extends TestConfig{


    @Test
    public void shouldSendCommand() throws TelnetClientImpl.TelnetConnectionException, RemoteCommandNotSendException {
        RemoteClient client = new TelnetClientImpl(windowsHost, windowsPort, windowsUsr, windowsPwd);
        ((TelnetClientImpl)client).connect();
        String result = client.sendCommand("ipconfig");
        assertTrue(result.contains(windowsHost));
    }

    @Test
    public void shouldSendWmicCommand() throws TelnetClientImpl.TelnetConnectionException, IOException, RemoteCommandNotSendException {
        RemoteClient client = new TelnetClientImpl(windowsHost, windowsPort, windowsUsr, windowsPwd);
        ((TelnetClientImpl)client).connect();
        String result = client.sendCommand("wmic process where name='notepad.exe' get name, WorkingSetSize");
        client.sendCommand("wmic cpu get loadpercentage /format:value");
        client.sendCommand("wmic os get freephysicalmemory /format:value");
        assertTrue(result.contains("notepad.exe"));
        ((TelnetClientImpl) client).close();
    }

    @Test
    public void shouldNotSendCommandInvalidHost() {
        TelnetClientImpl telnetClient = new TelnetClientImpl("192.168.0.108",32,"test","test");
        try {
            telnetClient.connect();
            telnetClient.sendCommand("ipconfig");
        } catch (TelnetClientImpl.TelnetConnectionException | RemoteCommandNotSendException e) {
            e.printStackTrace();
            return;
        }
        throw new AssertionError("Test failed");
    }

    @Test
    public void shouldNotSendCommandInvalidCredentials() {
        TelnetClientImpl telnetClient = new TelnetClientImpl(windowsHost, windowsPort,"test","test");
        try {
            telnetClient.connect();
            telnetClient.sendCommand("ipconfig");
        } catch (TelnetClientImpl.TelnetConnectionException | RemoteCommandNotSendException e) {
            Assert.assertTrue(e instanceof TelnetClientImpl.TelnetConnectionException);
            return;
        }
        throw new AssertionError("Test failed");
    }
}
