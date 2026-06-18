package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ui.tray.linux.menu.LinuxDbusMenu;

import java.util.concurrent.CountDownLatch;

public class LinuxTrayIconPoc {

    private static final Logger log = LogManager.getLogger(LinuxTrayIconPoc.class);

    public static void main(String[] args) {
        LinuxSniProbe probe = LinuxSniProbe.inspect();
        log.info("\n{}", probe.toString());

        if (!probe.canAttemptStatusNotifier()) {
            log.warn("Cannot attempt Linux StatusNotifier tray POC");
            return;
        }

        try {
            LinuxTrayPocActions actions = new LinuxTrayPocActions();
            LinuxDbusMenu menu = new LinuxDbusMenu(actions::showAbout, actions::exit);

            // Keep the POC alive; closing the tray connection removes the item.
            try (LinuxStatusNotifierTray tray = new LinuxStatusNotifierTray(probe, menu)) {
                new CountDownLatch(1).await();
            }
        }
        catch(Exception e) {
            log.warn("Unable to register Linux StatusNotifier tray POC", e);
        }
    }
}
