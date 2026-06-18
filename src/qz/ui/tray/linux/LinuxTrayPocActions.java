package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ui.AboutDialog;
import qz.ui.component.IconCache;
import qz.utils.FileUtilities;

import javax.swing.*;

class LinuxTrayPocActions {

    private static final Logger log = LogManager.getLogger(LinuxTrayPocActions.class);
    private AboutDialog aboutDialog;

    void showAbout() {
        SwingUtilities.invokeLater(() -> {
            try {
                getAboutDialog().setVisible(true);
            }
            catch(Exception e) {
                log.warn("Unable to show Linux StatusNotifier About dialog", e);
            }
        });
    }

    void exit() {
        FileUtilities.cleanup();
        System.exit(0);
    }

    private AboutDialog getAboutDialog() {
        if(aboutDialog == null) {
            JMenuItem aboutItem = new JMenuItem("About...");
            aboutDialog = new AboutDialog(aboutItem, new IconCache());
            aboutDialog.initComponents();
        }
        return aboutDialog;
    }
}
