package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import qz.common.Constants;

import java.awt.TrayIcon;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class LinuxNotifications {

    private static final Logger log = LogManager.getLogger(LinuxNotifications.class);
    // Notify D-Bus service and object path
    // See: https://specifications.freedesktop.org/notification/latest/protocol.html
    private static final String NOTIFICATIONS_SERVICE = "org.freedesktop.Notifications";
    private static final String NOTIFICATIONS_PATH = "/org/freedesktop/Notifications";
    // Actions are id/label pairs; QZ tray notices need none
    private static final String[] NO_ACTIONS = new String[0];
    // -1 lets the notification server choose expiry
    private static final int SERVER_DEFAULT_TIMEOUT = -1;
    // 0 means this notification replaces nothing
    private static final UInt32 NO_REPLACEMENT = new UInt32(0);
    private final FreedesktopNotifications notifications;
    // Notification daemons do not resolve SNI IconThemePath
    // Pass a concrete icon path instead of qz-tray-symbolic
    private final Supplier<String> appIconSupplier;

    public LinuxNotifications(DBusConnection connection, Supplier<String> appIconSupplier) {
        FreedesktopNotifications remoteNotifications = null;
        try {
            remoteNotifications = connection.getRemoteObject(
                    NOTIFICATIONS_SERVICE,
                    NOTIFICATIONS_PATH,
                    FreedesktopNotifications.class,
                    false
            );
        }
        catch(DBusException e) {
            // Missing notification service must not break tray startup
            log.warn("Unable to connect to Linux desktop notifications", e);
        }
        this.notifications = remoteNotifications;
        this.appIconSupplier = appIconSupplier;
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) {
        if(notifications == null) {
            return;
        }
        try {
            log.debug("Sending Linux desktop notification '{}' with level {}", caption, level);
            // TrayManager caption includes VERSION for AWT tooltip/dialog use
            // Native notifications should show the stable app title instead
            UInt32 notificationId = notifications.sendNotification(
                    Constants.ABOUT_TITLE,
                    NO_REPLACEMENT,
                    appIconSupplier.get(),
                    Constants.ABOUT_TITLE,
                    text,
                    NO_ACTIONS,
                    getHints(level),
                    SERVER_DEFAULT_TIMEOUT
            );
            log.debug("Sent Linux desktop notification {}", notificationId);
        }
        catch(Exception e) {
            log.warn("Unable to send Linux desktop notification", e);
        }
    }

    private Map<String, Variant<?>> getHints(TrayIcon.MessageType level) {
        Map<String, Variant<?>> hints = new HashMap<>();
        // Urgency hint values are 0 low, 1 normal, 2 critical
        // See: https://specifications.freedesktop.org/notification/latest/urgency-levels.html
        if(level == TrayIcon.MessageType.ERROR) {
            hints.put("urgency", new Variant<>((byte)2));
        } else if(level == TrayIcon.MessageType.INFO || level == TrayIcon.MessageType.WARNING) {
            hints.put("urgency", new Variant<>((byte)1));
        }
        return hints;
    }
}
