package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBus;
import qz.utils.SystemUtilities;

import java.awt.*;
import java.util.Locale;

public class LinuxSniProbe {

    private static final Logger log = LogManager.getLogger(LinuxSniProbe.class);
    // For probing runtime d-bus names
    // but remember different DEs advertise different names
    private static final String[] STATUS_NOTIFIER_WATCHERS = {
            "org.kde.StatusNotifierWatcher",
            "org.freedesktop.StatusNotifierWatcher"
    };
    private final boolean isLinux;
    private final boolean headless;
    private final String currentDesktop;
    private final String sessionType;
    private final String display;
    private final String waylandDisplay;
    private final String dbusSessionBusAddress;
    private final String xdgRuntimeDir;
    private boolean sessionBusReachable;
    // If statusNotifierWatcherPresent == true, it does not guarantee tray icons will work
    // it only proves that a watcher service exists on d-bus
    // and that qz tray can reasonably attempt to display a tray icon
    private boolean statusNotifierWatcherPresent;
    private String statusNotifierWatcher;
    private String failureReason;

    private LinuxSniProbe() {
        isLinux = SystemUtilities.isLinux();
        headless = GraphicsEnvironment.isHeadless();
        currentDesktop = getEnv("XDG_CURRENT_DESKTOP");
        sessionType = getEnv("XDG_SESSION_TYPE");
        display = getEnv("DISPLAY");
        waylandDisplay = getEnv("WAYLAND_DISPLAY");
        dbusSessionBusAddress = getEnv("DBUS_SESSION_BUS_ADDRESS");
        xdgRuntimeDir = getEnv("XDG_RUNTIME_DIR");
    }

    public static LinuxSniProbe inspect() {
        LinuxSniProbe env = new LinuxSniProbe();

        if (!env.isLinux) {
            env.failureReason = "not-linux";
            return env;
        }
        if (env.headless) {
            env.failureReason = "headless";
            return env;
        }

        return env.inspectDbus();
    }

    private String getEnv(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value;
    }

    private String present(String value) {
        return value == null || value.isEmpty() ? "" : "present";
    }

    public static void main(String[] args) {
        System.out.println(inspect().toString());
    }

    private LinuxSniProbe inspectDbus() {
        // dbus-java handles the d-bus authentication
        // and Unix-domain socket transport for the session bus
        // In Linux this can be done in terminal by:
        // busctl --user list >/dev/null
        try (DBusConnection connection = DBusConnectionBuilder.forSessionBus().build()) {
            // Asking: "hey d-bus, what services are currently registered?"
            DBus dbus = connection.getRemoteObject(
                    "org.freedesktop.DBus",
                    "/org/freedesktop/DBus",
                    DBus.class
            );
            sessionBusReachable = true;
            // Getting a StatusNotifierWatcher means the DE
            // has a host that can display system tray/AppIndicator items
            for (String watcher : STATUS_NOTIFIER_WATCHERS) {
                if (dbus.NameHasOwner(watcher)) {
                    statusNotifierWatcher = watcher;
                    statusNotifierWatcherPresent = true;
                    break;
                }
            }
            if (!statusNotifierWatcherPresent) {
                failureReason = "status-notifier-watcher-missing";
                log.warn("No StatusNotifier watcher was found. {}", getMissingWatcherSuggestion());
            }
        } catch (Exception e) {
            log.warn("Unable to inspect Linux StatusNotifier environment. {}", getMissingWatcherSuggestion(), e);
            failureReason = "session-bus-connect-failed";
        }
        return this;
    }

    boolean canAttemptStatusNotifierPoc() {
        return isLinux && !headless && sessionBusReachable && statusNotifierWatcherPresent;
    }

    String getStatusNotifierWatcher() {
        return statusNotifierWatcher;
    }

    private String getMissingWatcherSuggestion() {
        String desktop = currentDesktop.toLowerCase(Locale.ENGLISH);

        if (desktop.contains("gnome")) {
            return "No StatusNotifier host detected. " +
                    "Install and enable GNOME AppIndicator support (e.g. gnome-shell-extension-appindicator).";
        }
        if (desktop.contains("kde") || desktop.contains("plasma")) {
            return "No StatusNotifier host detected. " +
                    "Verify the Plasma system tray widget is enabled and running.";
        }
        if (desktop.contains("xfce")) {
            return "No StatusNotifier host detected. " +
                    "Install or enable XFCE StatusNotifier support (e.g. xfce4-statusnotifier-plugin).";
        }
        return "No StatusNotifier host detected. " +
                "Install or enable AppIndicator/StatusNotifier support for your desktop environment.";
    }

    @Override
    public String toString() {
        String report = "Linux: " + isLinux + "\n"
                + "Headless: " + headless + "\n"
                + "XDG_CURRENT_DESKTOP: " + currentDesktop + "\n"
                + "XDG_SESSION_TYPE: " + sessionType + "\n"
                + "DISPLAY: " + display + "\n"
                + (waylandDisplay.isEmpty() ? "" : "WAYLAND_DISPLAY: " + waylandDisplay + "\n")
                + "DBUS_SESSION_BUS_ADDRESS: " + present(dbusSessionBusAddress) + "\n"
                + "XDG_RUNTIME_DIR: " + xdgRuntimeDir + "\n"
                + "Session bus reachable: " + sessionBusReachable + "\n"
                + "StatusNotifier watcher: " + (statusNotifierWatcherPresent ? statusNotifierWatcher + " present" : "missing") + "\n"
                + "Can attempt StatusNotifier POC: " + canAttemptStatusNotifierPoc();

        return failureReason == null ? report : report + "\nFailure reason: " + failureReason;
    }
}
