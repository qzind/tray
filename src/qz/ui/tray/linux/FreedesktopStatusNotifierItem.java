package qz.ui.tray.linux;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.interfaces.DBusInterface;

/*
 * Mirrors KdeStatusNotifierItem for environments that expose the freedesktop
 * watcher namespace instead of the deployed KDE/AppIndicator namespace
 */
@DBusInterfaceName("org.freedesktop.StatusNotifierItem")
public interface FreedesktopStatusNotifierItem extends DBusInterface {

    @DBusBoundProperty(access = DBusProperty.Access.READ, name = "Category")
    String getCategory();

    @DBusBoundProperty(access = DBusProperty.Access.READ, name = "Id")
    String getId();

    @DBusBoundProperty(access = DBusProperty.Access.READ, name = "Title")
    String getTitle();

    @DBusBoundProperty(access = DBusProperty.Access.READ, name = "Status")
    String getStatus();

    @DBusBoundProperty(access = DBusProperty.Access.READ, name = "IconName")
    String getIconName();

    @DBusBoundProperty(access = DBusProperty.Access.READ, name = "IconThemePath")
    String getIconThemePath();

    @DBusMemberName("ContextMenu")
    void contextMenu(int x, int y);

    @DBusMemberName("Activate")
    void activate(int x, int y);

    @DBusMemberName("SecondaryActivate")
    void secondaryActivate(int x, int y);

    @DBusMemberName("Scroll")
    void scroll(int delta, String orientation);
}
