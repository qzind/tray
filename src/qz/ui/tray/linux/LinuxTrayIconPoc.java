package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;

import java.util.concurrent.CountDownLatch;

public class LinuxTrayIconPoc {

    private static final Logger log = LogManager.getLogger(LinuxTrayIconPoc.class);

    public static void main(String[] args) {
        LinuxSniProbe probe = LinuxSniProbe.inspect();
        log.info("{}{}", System.lineSeparator(), probe.toString());

        if (!probe.canAttemptStatusNotifierPoc()) {
            log.warn("Cannot attempt Linux StatusNotifier tray POC");
            return;
        }

        try (DBusConnection connection = DBusConnectionBuilder.forSessionBus().build()) {
            // The item bus name must match the watcher
            // namespace that is actually present at runtime
            // The PID keeps the POC service unique
            String itemService = getItemServicePrefix(probe.getStatusNotifierWatcher())
                    + ProcessHandle.current().pid();
            LinuxStatusNotifierItem item = new LinuxStatusNotifierItem();

            // Own the item service name and export
            // the object that the watcher/tray host will inspect
            connection.requestBusName(itemService);
            connection.exportObject(item.getObjectPath(), item);

            // Register against the watcher detected
            // by LinuxSniProbe instead of assuming a desktop environment
            StatusNotifierWatcher watcher = connection.getRemoteObject(
                    probe.getStatusNotifierWatcher(),
                    "/StatusNotifierWatcher",
                    StatusNotifierWatcher.class,
                    false
            );
            watcher.registerStatusNotifierItem(itemService);

            log.info("Registered StatusNotifier item {} at {}", itemService, item.getObjectPath());
            // Keep the POC alive
            // the watcher removes the item when
            // this bus name disappears
            new CountDownLatch(1).await();
        }
        catch(Exception e) {
            log.warn("Unable to register Linux StatusNotifier tray POC", e);
        }
    }

    private static String getItemServicePrefix(String watcher) {
        // Most deployed hosts use org.kde.*,
        // but this keeps the POC aligned with a
        // freedesktop watcher if one is found
        return watcher.startsWith("org.freedesktop.")
                ? "org.freedesktop.StatusNotifierItem-"
                : "org.kde.StatusNotifierItem-";
    }
}
