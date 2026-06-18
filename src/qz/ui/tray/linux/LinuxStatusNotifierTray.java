package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import qz.ui.tray.linux.menu.LinuxDbusMenu;

import java.io.IOException;

public class LinuxStatusNotifierTray implements AutoCloseable {

    private static final Logger log = LogManager.getLogger(LinuxStatusNotifierTray.class);

    private final DBusConnection connection;

    public LinuxStatusNotifierTray(LinuxSniProbe probe, LinuxDbusMenu menu) throws Exception {
        String statusNotifierWatcher = probe.getStatusNotifierWatcher();
        String itemService = getItemServicePrefix(statusNotifierWatcher) + ProcessHandle.current().pid();
        String iconThemePath = LinuxSniIconTheme.prepare();
        LinuxStatusNotifierItem item = new LinuxStatusNotifierItem(iconThemePath);

        // Export the complete item before registration so the watcher can
        // resolve the service, item properties, and menu immediately.
        DBusConnection newConnection = DBusConnectionBuilder.forSessionBus().build();
        try {
            newConnection.requestBusName(itemService);
            newConnection.exportObject(item.getObjectPath(), item);
            newConnection.exportObject(menu.getObjectPath(), menu);
            registerStatusNotifierItem(newConnection, statusNotifierWatcher, itemService);
        }
        catch(Exception e) {
            newConnection.close();
            throw e;
        }
        connection = newConnection;

        log.info("Registered StatusNotifier item {} at {}", itemService, item.getObjectPath());
        log.info("Published StatusNotifier icon theme path {}", iconThemePath);
    }

    private static String getItemServicePrefix(String watcher) {
        // Most deployed hosts use org.kde.*, but this keeps registration
        // aligned with a freedesktop watcher when one is present.
        return watcher.startsWith("org.freedesktop.")
                ? "org.freedesktop.StatusNotifierItem-"
                : "org.kde.StatusNotifierItem-";
    }

    private static void registerStatusNotifierItem(DBusConnection connection, String watcherService,
                                                   String itemService) throws Exception {
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

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
