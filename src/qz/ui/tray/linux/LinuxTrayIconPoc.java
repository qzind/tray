package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinuxTrayIconPoc {

    private static final Logger log = LogManager.getLogger(LinuxTrayIconPoc.class);

    public static void main(String[] args) {
        LinuxSniProbe probe = LinuxSniProbe.inspect();
        log.info("{}{}", System.lineSeparator(), probe.toString());

        if (!probe.canAttemptStatusNotifierPoc()) {
            log.warn("Cannot attempt Linux StatusNotifier tray POC");
            return;
        }

        LinuxStatusNotifierItem item = new LinuxStatusNotifierItem();
        log.info("Prepared StatusNotifier item at {}", item.getObjectPath());
    }
}
