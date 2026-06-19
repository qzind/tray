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
    // Test unverified desktops without adding them to the production allowlist
    private static final String ALLOW_UNVERIFIED_DESKTOP = "qz.sni.allowUnverifiedDesktop";
    private final boolean isLinux;
    private final boolean headless;
    private final String currentDesktop;
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
        currentDesktop = getEnv("XDG_CURRENT_DESKTOP");
        sessionType = getEnv("XDG_SESSION_TYPE");
        display = getEnv("DISPLAY");
        waylandDisplay = getEnv("WAYLAND_DISPLAY");
        dbusSessionBusAddress = getEnv("DBUS_SESSION_BUS_ADDRESS");
        xdgRuntimeDir = getEnv("XDG_RUNTIME_DIR");
        verifiedDesktop = isVerifiedDesktop(currentDesktop);
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

    boolean isCinnamon() {
        return currentDesktop.toLowerCase(Locale.ENGLISH).contains("cinnamon");
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

    private boolean isVerifiedDesktop(String desktopName) {
        // End-to-end QZ Tray tests passed on Ubuntu GNOME with AppIndicator
        // support, KDE Plasma, XFCE, Ubuntu Budgie, Cinnamon, and MATE
        //
        // Tested but not usable:
        // - COSMIC registered the item, but showed a gray placeholder and no menu
        // - Pantheon/elementary OS 8.1 had no StatusNotifier watcher or panel host
        //
        // Good candidates for future verification:
        // - LXQt, whose panel has a plugin implementing the SNI specification
        //
        // Cinnamon uses an absolute PNG path for xapp-sn-watcher compatibility
        // Other desktops remain on the fallback until verified end to end
        String desktop = desktopName.toLowerCase(Locale.ENGLISH);
        return desktop.contains("gnome")
                || desktop.contains("kde")
                || desktop.contains("plasma")
                || desktop.contains("xfce")
                || desktop.contains("budgie")
                || desktop.contains("cinnamon")
                || desktop.contains("mate");
    }

    @Override
    public String toString() {
        String report = "Linux: " + isLinux + "\n"
                + "Headless: " + headless + "\n"
                + "XDG_CURRENT_DESKTOP: " + currentDesktop + "\n"
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
