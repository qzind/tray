package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBus;
import qz.ui.tray.linux.menu.LinuxDbusMenu;

import java.io.IOException;

public class LinuxStatusNotifierTray implements AutoCloseable {

    private static final Logger log = LogManager.getLogger(LinuxStatusNotifierTray.class);

    private final DBusConnection connection;
    // Closing this registration removes the NameOwnerChanged listener before disconnecting
    private final AutoCloseable watcherRegistration;
    // D-Bus signal handlers run outside the Swing thread
    // so close state must be visible across threads
    private volatile boolean closed;

    public LinuxStatusNotifierTray(LinuxSniProbe probe, LinuxDbusMenu menu) throws Exception {
        String statusNotifierWatcher = probe.getStatusNotifierWatcher();
        String itemService = getItemServicePrefix(statusNotifierWatcher) + ProcessHandle.current().pid();
        String iconThemePath = LinuxSniIconTheme.prepare();
        LinuxStatusNotifierItem item = new LinuxStatusNotifierItem(iconThemePath);

        // Export the complete item before registration so the watcher can
        // resolve the service, item properties, and menu immediately
        // The tray owns its bus name and exported objects, so use a dedicated
        // connection instead of sharing lifecycle with other future D-Bus callers
        DBusConnection newConnection = DBusConnectionBuilder.forSessionBus()
                .receivingThreadConfig()
                // IMPORTANT:
                // libdbusmenu clients issue property requests in order
                // Hence, Keep method dispatch ordered so submenu
                // parents realize before their children
                // without doing this, sub-submenus like the
                // submenus of "Diagnostic" don't show on XFCE
                .withMethodCallThreadCount(1)
                .connectionConfig()
                .withShared(false)
                .build();
        AutoCloseable newWatcherRegistration = null;
        try {
            newConnection.requestBusName(itemService);
            // LinuxDbusMenu owns signal creation while this connection owns delivery
            menu.setSignalEmitter(newConnection::sendMessage);
            newConnection.exportObject(item.getObjectPath(), item);
            newConnection.exportObject(menu.getObjectPath(), menu);
            // Desktop panels can restart their watcher while QZ Tray keeps running
            // A replacement watcher has no previous registrations, so register again
            // when the selected watcher service receives a new D-Bus owner
            newWatcherRegistration = newConnection.addSigHandler(DBus.NameOwnerChanged.class, signal -> {
                if(statusNotifierWatcher.equals(signal.name) &&
                        !signal.newOwner.isEmpty() &&
                        !signal.newOwner.equals(signal.oldOwner) &&
                        !closed) {
                    try {
                        registerStatusNotifierItem(newConnection, statusNotifierWatcher, itemService);
                        log.info("Re-registered StatusNotifier item {} after watcher restart", itemService);
                    }
                    catch(Exception e) {
                        log.warn("Unable to re-register StatusNotifier item {}", itemService, e);
                    }
                }
            });
            registerStatusNotifierItem(newConnection, statusNotifierWatcher, itemService);
        }
        catch(Exception e) {
            closeWatcherRegistration(newWatcherRegistration);
            newConnection.close();
            throw e;
        }
        connection = newConnection;
        watcherRegistration = newWatcherRegistration;

        log.info("Registered StatusNotifier item {} at {}", itemService, item.getObjectPath());
        log.info("Published StatusNotifier icon theme path {}", iconThemePath);
    }

    private static String getItemServicePrefix(String watcher) {
        // Most deployed hosts use org.kde.*, but this keeps registration
        // aligned with a freedesktop watcher when one is present
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
    public synchronized void close() throws IOException {
        // TrayManager may reach shutdown through more than one path
        // Closing only once keeps listener and connection cleanup safe
        if(closed) {
            return;
        }
        closed = true;
        // Remove the callback first so a watcher change cannot race with disconnection
        closeWatcherRegistration(watcherRegistration);
        connection.close();
    }

    private void closeWatcherRegistration(AutoCloseable registration) {
        if(registration != null) {
            try {
                // dbus-java returns an AutoCloseable which removes the signal match and handler
                registration.close();
            }
            catch(Exception e) {
                log.warn("Unable to remove StatusNotifier watcher listener", e);
            }
        }
    }
}
