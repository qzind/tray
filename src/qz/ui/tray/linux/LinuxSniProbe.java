package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBus;
import qz.utils.SystemUtilities;
import qz.utils.linux.De;
import qz.utils.linux.LinuxUtilities;

import java.awt.*;

public class LinuxSniProbe {
    private static final Logger log = LogManager.getLogger(LinuxSniProbe.class);
    // For probing runtime d-bus names
    // but remember different DEs advertise different names
    private static final String[] STATUS_NOTIFIER_WATCHERS = {
            "org.kde.StatusNotifierWatcher",
            "org.freedesktop.StatusNotifierWatcher"
    };
    // Test unverified desktops without adding them to the production allowlist
    private static final String ALLOW_UNVERIFIED_DESKTOP = "qz.sni.allowUnverifiedDesktop";
    private final boolean isLinux;
    private final boolean headless;
    private final De currentDesktop;
    private final String sessionType;
    private final String display;
    private final String waylandDisplay;
    private final String dbusSessionBusAddress;
    private final String xdgRuntimeDir;
    private final boolean verifiedDesktop;
    private final boolean allowUnverifiedDesktop;
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
        currentDesktop = LinuxUtilities.getDesktopEnvironment();
        sessionType = getEnv("XDG_SESSION_TYPE");
        display = getEnv("DISPLAY");
        waylandDisplay = getEnv("WAYLAND_DISPLAY");
        dbusSessionBusAddress = getEnv("DBUS_SESSION_BUS_ADDRESS");
        xdgRuntimeDir = getEnv("XDG_RUNTIME_DIR");
        verifiedDesktop = isSupported(currentDesktop);
        // This flag changes test eligibility only
        // It does not mark the desktop as verified
        allowUnverifiedDesktop = Boolean.getBoolean(ALLOW_UNVERIFIED_DESKTOP);
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
            } else if(!verifiedDesktop && !allowUnverifiedDesktop) {
                failureReason = "desktop-not-verified";
                log.warn("StatusNotifier is not verified for desktop '{}'; using the taskbar fallback.", currentDesktop);
            } else if(!verifiedDesktop) {
                log.warn("StatusNotifier desktop verification bypass enabled for '{}'", currentDesktop);
            }
        } catch (Exception e) {
            log.warn("Unable to inspect Linux StatusNotifier environment. {}", getMissingWatcherSuggestion(), e);
            failureReason = "session-bus-connect-failed";
        }
        return this;
    }

    public boolean canAttemptStatusNotifier() {
        // The override bypasses only the desktop allowlist
        // Linux, display, session bus, and watcher checks still apply
        return isLinux && !headless && (verifiedDesktop || allowUnverifiedDesktop) &&
                sessionBusReachable && statusNotifierWatcherPresent;
    }

    String getStatusNotifierWatcher() {
        return statusNotifierWatcher;
    }

    private String getMissingWatcherSuggestion() {
        String message = switch(currentDesktop) {
            case BUDGIE -> "Budgie System Tray Applet";
            case CINNAMON -> "XApp Status Applet (xapp-sn-watcher).";
            case GNOME -> "GNOME AppIndicator support (e.g. gnome-shell-extension-appindicator).";
            case KDE -> "System Tray Widget";
            case LXQT -> "Status Notifier Plugin";
            case MATE -> "Notification Area Applet";
            case XFCE -> "XFCE StatusNotifier (e.g. xfce4-statusnotifier-plugin)";
            default -> "AppIndicator/StatusNotifier support for your desktop environment";
        };

        return String.format("No StatusNotifier host detected. Please install/enable %s and add/enable it in the %s panel", message, currentDesktop);
    }

    private boolean isSupported(De de) {
        // End-to-end QZ Tray tests passed on Ubuntu GNOME with AppIndicator
        // support, KDE, XFCE, LXQt, Ubuntu Budgie, Cinnamon, and MATE
        //
        // Tested but not usable:
        // - COSMIC displays the icon and menu, but the nested Diagnostic submenu
        //   does not open
        // - Pantheon/elementary OS 8.1 had no StatusNotifier watcher or panel host
        //
        // Cinnamon uses an absolute PNG path for xapp-sn-watcher compatibility
        // Other desktops remain on the fallback until verified end to end
        return switch(de) {
            case CINNAMON, BUDGIE, GNOME, KDE, LXQT, MATE, XFCE -> true;
            default -> false;
        };
    }

    @Override
    public String toString() {
        String report = "Linux: " + isLinux + "\n"
                + "Headless: " + headless + "\n"
                + "XDG_CURRENT_DESKTOP: " + getEnv("XDG_CURRENT_DESKTOP") + "\n"
                + "Verified desktop: " + verifiedDesktop + "\n"
                + "Allow unverified desktop: " + allowUnverifiedDesktop + "\n"
                + "XDG_SESSION_TYPE: " + sessionType + "\n"
                + "DISPLAY: " + display + "\n"
                + (waylandDisplay.isEmpty() ? "" : "WAYLAND_DISPLAY: " + waylandDisplay + "\n")
                + "DBUS_SESSION_BUS_ADDRESS: " + present(dbusSessionBusAddress) + "\n"
                + "XDG_RUNTIME_DIR: " + xdgRuntimeDir + "\n"
                + "Session bus reachable: " + sessionBusReachable + "\n"
                + "StatusNotifier watcher: " + (statusNotifierWatcherPresent ? statusNotifierWatcher + " present" : "missing") + "\n"
                + "Can attempt StatusNotifier: " + canAttemptStatusNotifier();

        return failureReason == null ? report : report + "\nFailure reason: " + failureReason;
    }
}
