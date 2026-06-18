package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.TrayManager;
import qz.utils.FileUtilities;
import qz.ws.PrintSocketServer;

import javax.swing.*;

public class LinuxTrayExitAction {

    private static final Logger log = LogManager.getLogger(LinuxTrayExitAction.class);

    public void exit() {
        // D-Bus calls arrive off the Swing event thread. When running inside the
        // real app, reuse TrayManager's normal confirmation and shutdown path.
        SwingUtilities.invokeLater(() -> {
            try {
                TrayManager trayManager = PrintSocketServer.getTrayManager();
                if(trayManager != null) {
                    trayManager.confirmAndExit();
                } else {
                    FileUtilities.cleanup();
                    System.exit(0);
                }
            }
            catch(Exception e) {
                log.warn("Unable to run Linux StatusNotifier Exit action", e);
            }
        });
    }
}