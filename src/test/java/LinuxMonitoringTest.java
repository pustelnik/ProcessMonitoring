import com.monitor.Monitor;
import com.monitor.linux.LinuxMonitorImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Jakub on 15.08.16.
 */
public class LinuxMonitoringTest extends TestConfig {

    @Test
    public void shouldNotExecuteMonitor() {
        Monitor linMonit = new LinuxMonitorImpl("xxx",linuxUsr,linuxPwd); // fill this with test data
        linMonit.startMonitoring(new String[]{"firefox","chromium","docker","xorg"},5, TimeUnit.SECONDS);
        try {
            Thread.sleep(60000);
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

    @Test
    public void shouldExecuteMonitor() {
        Monitor linMonit = new LinuxMonitorImpl(linuxHost,linuxUsr,linuxPwd); // fill this with test data
        linMonit.startMonitoring(new String[]{"firefox","docker","xorg","bash"},15, TimeUnit.SECONDS);
        try {
            Thread.sleep(1200000);
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
