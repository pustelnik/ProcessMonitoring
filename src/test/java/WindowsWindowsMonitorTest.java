import com.monitor.Monitor;
import com.monitor.windows.WindowsMonitorImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author jakub on 19.08.16.
 */
public class WindowsWindowsMonitorTest extends TestConfig
{

    @Test
    public void shouldMonitorWindows() {
        Monitor windowsMonitor = new WindowsMonitorImpl(windowsHost,windowsPort,windowsUsr,windowsPwd);
        windowsMonitor.startMonitoring(new String[]{"prime95","iexplore"},15, TimeUnit.SECONDS);
        try {
            Thread.sleep(300000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        windowsMonitor.stopMonitoring();
        try {
            windowsMonitor.copyMonitoringResults("reports/html-report/windows");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
