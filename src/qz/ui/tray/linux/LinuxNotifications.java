package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import qz.common.Constants;

import java.awt.TrayIcon;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LinuxNotifications {

    private static final Logger log = LogManager.getLogger(LinuxNotifications.class);
    // Notify D-Bus service and object path
    // See: https://specifications.freedesktop.org/notification/latest/protocol.html
    private static final String NOTIFICATIONS_SERVICE = "org.freedesktop.Notifications";
    private static final String NOTIFICATIONS_PATH = "/org/freedesktop/Notifications";
    private static final String DBUS_SERVICE = "org.freedesktop.DBus";
    private static final String DBUS_PATH = "/org/freedesktop/DBus";
    // Actions are id/label pairs; QZ tray notices need none
    private static final String[] NO_ACTIONS = new String[0];
    // -1 lets the notification server choose expiry
    private static final int SERVER_DEFAULT_TIMEOUT = -1;
    // 0 means this notification replaces nothing
    private static final UInt32 NO_REPLACEMENT = new UInt32(0);
    private final FreedesktopNotifications notifications;
    // Notification daemons do not resolve SNI IconThemePath
    // Pass a concrete icon path instead of qz-tray-symbolic
    private final String appIcon;

    public LinuxNotifications(DBusConnection connection, String appIcon) {
        FreedesktopNotifications remoteNotifications = null;
        try {
            if(hasNotificationServer(connection)) {
                remoteNotifications = connection.getRemoteObject(
                        NOTIFICATIONS_SERVICE,
                        NOTIFICATIONS_PATH,
                        FreedesktopNotifications.class,
                        false
                );
            } else {
                log.info("Linux desktop notifications unavailable");
            }
        }
        catch(DBusException e) {
            // Missing notification service must not break tray startup
            log.warn("Unable to connect to Linux desktop notifications", e);
        }
        this.notifications = remoteNotifications;
        this.appIcon = appIcon;
    }

    private boolean hasNotificationServer(DBusConnection connection) throws DBusException {
        DBus dbus = connection.getRemoteObject(DBUS_SERVICE, DBUS_PATH, DBus.class, false);
        // getRemoteObject can return a proxy before the service exists
        // MATE may expose notifyd only as a D-Bus activatable service
        return dbus.NameHasOwner(NOTIFICATIONS_SERVICE) ||
                Arrays.asList(dbus.ListActivatableNames()).contains(NOTIFICATIONS_SERVICE);
    }

    public void displayMessage(String caption, String text, TrayIcon.MessageType level) {
        if(notifications == null) {
            return;
        }
        try {
            log.debug("Sending Linux desktop notification '{}' with level {}", caption, level);
            UInt32 notificationId = notifications.sendNotification(
                    Constants.ABOUT_TITLE,
                    NO_REPLACEMENT,
                    appIcon,
                    getSummary(caption, level),
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

    private String getSummary(String caption, TrayIcon.MessageType level) {
        // GNOME HIG asks titles to summarize the event
        // Windows uses app attribution separately from text
        // https://developer.gnome.org/hig/patterns/feedback/notifications.html
        // https://learn.microsoft.com/en-us/windows/apps/develop/notifications/app-notifications/app-notifications-content
        if(level == TrayIcon.MessageType.ERROR) {
            return "Problem";
        }
        if(level == TrayIcon.MessageType.WARNING) {
            return "Attention";
        }
        if(level == TrayIcon.MessageType.INFO) {
            return "Update";
        }
        return caption;
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
