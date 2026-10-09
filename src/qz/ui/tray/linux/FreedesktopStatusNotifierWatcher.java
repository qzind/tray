package qz.ui.tray.linux;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.interfaces.DBusInterface;

/*
 * Mirrors KdeStatusNotifierWatcher for environments that expose the freedesktop
 * watcher namespace instead of the deployed KDE/AppIndicator namespace
 */
@DBusInterfaceName("org.freedesktop.StatusNotifierWatcher")
public interface FreedesktopStatusNotifierWatcher extends DBusInterface {

    @DBusMemberName("RegisterStatusNotifierItem")
    void registerStatusNotifierItem(String service);
}