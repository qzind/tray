package qz.ui.tray.linux;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/*
 * Mirrors KdeStatusNotifierItem for environments that expose the freedesktop
 * watcher namespace instead of the deployed KDE/AppIndicator namespace
 */
@DBusInterfaceName("org.freedesktop.StatusNotifierItem")
@DBusProperty(access = DBusProperty.Access.READ, name = "Category", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "Id", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "Title", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "Status", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "IconName", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "IconThemePath", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "Menu", type = DBusPath.class)
public interface FreedesktopStatusNotifierItem extends DBusInterface, Properties {

    @DBusMemberName("ContextMenu")
    void contextMenu(int x, int y);

    @DBusMemberName("Activate")
    void activate(int x, int y);

    @DBusMemberName("SecondaryActivate")
    void secondaryActivate(int x, int y);

    @DBusMemberName("Scroll")
    void scroll(int delta, String orientation);
}
