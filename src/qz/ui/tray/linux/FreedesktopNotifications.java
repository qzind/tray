package qz.ui.tray.linux;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

/*
 * org.freedesktop.Notifications service used by desktop notification servers.
 * Notify has D-Bus signature susssasa{sv}i -> u.
 */
@DBusInterfaceName("org.freedesktop.Notifications")
public interface FreedesktopNotifications extends DBusInterface {

    // WARNING:
    // Never name this method notify(...).
    // Java proxy handling will resolve it as Object.notify()
    // and this will make it return without sending the d-bus
    // Notify call
    @DBusMemberName("Notify")
    UInt32 sendNotification(String appName,
                            UInt32 replacesId,
                            String appIcon,
                            String summary,
                            String body,
                            String[] actions,
                            Map<String, Variant<?>> hints,
                            int expireTimeout);
}