package qz.ui.tray.linux;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.interfaces.DBusInterface;

/*
 * KDE-namespaced d-bus registry service used by the desktop environment
 * to track and expose StatusNotifierItems to the panel/tray
 *
 * The watcher does not render UI. It only registers items and forwards
 * them to the desktop tray implementation
 *
 * References:
 * https://hypfvieh.github.io/dbus-java/exporting-objects.html
 * https://specifications.freedesktop.org/status-notifier-item/latest/status-notifier-watcher.html
 */
@DBusInterfaceName("org.kde.StatusNotifierWatcher")
public interface KdeStatusNotifierWatcher extends DBusInterface {

    @DBusMemberName("RegisterStatusNotifierItem")
    void registerStatusNotifierItem(String service);
}
