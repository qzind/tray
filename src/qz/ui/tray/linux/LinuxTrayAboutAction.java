package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ui.AboutDialog;
import qz.ui.component.IconCache;

import javax.swing.*;

class LinuxTrayAboutAction {

    private static final Logger log = LogManager.getLogger(LinuxTrayAboutAction.class);
    private AboutDialog aboutDialog;

    void show() {
        // D-Bus calls arrive off the Swing event thread
        // so, the existing QZ UI must be created and shown on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                getAboutDialog().setVisible(true);
            }
            catch(Exception e) {
                log.warn("Unable to show Linux StatusNotifier About dialog", e);
            }
        });
    }

    private AboutDialog getAboutDialog() {
        if(aboutDialog == null) {
            // Reuse the existing AboutDialog implementation
            JMenuItem aboutItem = new JMenuItem("About...");
            aboutDialog = new AboutDialog(aboutItem, new IconCache());
            aboutDialog.initComponents();
        }
        return aboutDialog;
    }
}