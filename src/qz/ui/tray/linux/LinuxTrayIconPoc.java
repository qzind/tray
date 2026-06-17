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
        log.info("\n{}", probe.toString());

        if (!probe.canAttemptStatusNotifierPoc()) {
            log.warn("Cannot attempt Linux StatusNotifier tray POC");
            return;
        }

        // dbus-java's exported-object flow starts with the user's session bus:
        // https://hypfvieh.github.io/dbus-java/exporting-objects.html
        try (DBusConnection connection = DBusConnectionBuilder.forSessionBus().build()) {
            // The item bus name must match the watcher
            // namespace that is actually present at runtime
            // The PID keeps the POC service unique
            String statusNotifierWatcher = probe.getStatusNotifierWatcher();
            String itemService = getItemServicePrefix(statusNotifierWatcher)
                    + ProcessHandle.current().pid();
            String iconThemePath = LinuxSniIconTheme.prepare();
            LinuxStatusNotifierItem item = new LinuxStatusNotifierItem(iconThemePath);
            LinuxDbusMenu menu = new LinuxDbusMenu(new LinuxTrayAboutAction());

            // Own the item service name before registration so the watcher can resolve
            // the service name back to this process.
            connection.requestBusName(itemService);
            // Export the StatusNotifierItem object that the
            // watcher/tray host will inspect
            connection.exportObject(item.getObjectPath(), item);
            // The StatusNotifierItem Menu property points here
            // so exporting it before registration lets tray hosts
            // fetch the menu immediately
            connection.exportObject(menu.getObjectPath(), menu);

            // Register against the watcher detected
            // by LinuxSniProbe instead of assuming a desktop environment
            registerStatusNotifierItem(connection, statusNotifierWatcher, itemService);

            log.info("Registered StatusNotifier item {} at {}", itemService, item.getObjectPath());
            log.info("Published StatusNotifier icon theme path {}", iconThemePath);
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

    private static void registerStatusNotifierItem(DBusConnection connection, String watcherService, String itemService) throws Exception {
        if (watcherService.startsWith("org.freedesktop.")) {
            FreedesktopStatusNotifierWatcher watcher = connection.getRemoteObject(
                    watcherService,
                    "/StatusNotifierWatcher",
                    FreedesktopStatusNotifierWatcher.class,
                    false
            );
            watcher.registerStatusNotifierItem(itemService);
        } else {
            KdeStatusNotifierWatcher watcher = connection.getRemoteObject(
                    watcherService,
                    "/StatusNotifierWatcher",
                    KdeStatusNotifierWatcher.class,
                    false
            );
            watcher.registerStatusNotifierItem(itemService);
        }
    }
}
