import com.monitor.Monitor;
import com.monitor.MonitorImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Jakub Pustelnik on 15.08.16.
 */
public class LinuxMonitorTest {

    @Test
    public void shouldExecuteMonitor() {
        Monitor linMonit = new MonitorImpl("xxx.xxx.xxx.xxx","xxx","xxx"); // fill this with test data
        linMonit.startMonitoring(new String[]{"firefox","chromium","docker","xorg"},5, TimeUnit.SECONDS);
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        linMonit.stopMonitoring();
        try {
            linMonit.copyMonitoringResults("reports/html-report/");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
