package net.sourceforge.subsonic.booter.agent;

import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import net.sourceforge.subsonic.booter.deployer.DeploymentStatus;
import net.sourceforge.subsonic.booter.deployer.SubsonicDeployerService;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for deploying the Subsonic web app in
 * the embedded Jetty container.
 *
 * @author Sindre Mehus
 */
public class SubsonicAgent {

    private final List<SubsonicListener> listeners = new ArrayList<SubsonicListener>();
    private final TrayController trayController;
    private final SubsonicFrame frame;
    private final SubsonicDeployerService service;
    private static final int POLL_INTERVAL_DEPLOYMENT_INFO_SECONDS = 5;
    private static final int POLL_INTERVAL_SERVICE_STATUS_SECONDS = 5;
    private String url;
    private boolean serviceStatusPollingEnabled;

    public SubsonicAgent(SubsonicDeployerService service) {
        setLookAndFeel();
        this.service = service;
        trayController = new TrayController(this);
        frame = new SubsonicFrame(this);
        startPolling();
    }

    private void setLookAndFeel() {
        // Set look-and-feel.
        try {
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (Throwable x) {
            System.err.println("Failed to set look-and-feel.\n" + x);
        }
    }

    private void startPolling() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    notifyDeploymentInfo(service.getDeploymentInfo());
                } catch (Throwable x) {
                    notifyDeploymentInfo(null);
                }
            }
        };
        executor.scheduleWithFixedDelay(runnable, 0, POLL_INTERVAL_DEPLOYMENT_INFO_SECONDS, TimeUnit.SECONDS);

        runnable = new Runnable() {
            public void run() {
                if (serviceStatusPollingEnabled) {
                    try {
                        notifyServiceStatus(getServiceStatus());
                    } catch (Throwable x) {
                        notifyServiceStatus(null);
                    }
                }
            }
        };
        executor.scheduleWithFixedDelay(runnable, 0, POLL_INTERVAL_SERVICE_STATUS_SECONDS, TimeUnit.SECONDS);
    }

    private String getServiceStatus() throws Exception {
        Process process = Runtime.getRuntime().exec("subsonic-service.exe -status");
        return IOUtils.toString(process.getInputStream());
    }

    public void setServiceStatusPollingEnabled(boolean enabled) {
        serviceStatusPollingEnabled = enabled;
    }

    public void startOrStopService(boolean start) {
        try {
            List<String> cmd = new ArrayList<String>();
            if (isElevationNeeded()) {
                cmd.add("elevate.exe");
            }
            cmd.add("subsonic-service.exe");
            cmd.add(start ? "-start" : "-stop");

            System.err.println("Executing: " + cmd);

            Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    /**
     * Returns whether UAC elevation is necessary (to start/stop services etc).
     */
    private boolean isElevationNeeded() {
        String osVersion = System.getProperty("os.version");
        try {
            int majorVersion = Integer.parseInt(osVersion.substring(0, osVersion.indexOf(".")));

            // Elevation is necessary in Windows Vista (os.version=6.1) and later.
            return majorVersion >= 6;
        } catch (Exception x) {
            System.err.println("Failed to resolve OS version from '" + osVersion + "'\n" + x);
            return false;
        }
    }

    public void addListener(SubsonicListener listener) {
        listeners.add(listener);
    }

    private void notifyDeploymentInfo(DeploymentStatus status) {
        if (status != null) {
            url = status.getURL();
        }

        for (SubsonicListener listener : listeners) {
            listener.notifyDeploymentStatus(status);
        }
    }

    private void notifyServiceStatus(String status) {
        for (SubsonicListener listener : listeners) {
            listener.notifyServiceStatus(status);
        }
    }

    public void showControlPanel() {
        frame.showControlPanel();
    }

    public void exit() {
        trayController.uninstallComponents();
        System.exit(0);
    }

    public void openBrowser() {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable x) {
            x.printStackTrace();
        }
    }
}
