package qz.ui.tray.linux;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.Properties;

/*
 * KDE-namespaced StatusNotifierItem interface used by the deployed
 * AppIndicator/StatusNotifier implementations on common Linux desktops
 *
 * This contains the methods invoked by the tray host side
 * via d-bus when the user interacts with the tray item
 *
 * It relies heavily on d-bus properties and signals which
 * must be exported separately
 *
 * References:
 * https://hypfvieh.github.io/dbus-java/exporting-objects.html
 * https://hypfvieh.github.io/dbus-java/properties.html
 * https://specifications.freedesktop.org/status-notifier-item/latest/status-notifier-item.html
 */
@DBusInterfaceName("org.kde.StatusNotifierItem")
@DBusProperty(access = DBusProperty.Access.READ, name = "Category", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "Id", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "Title", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "Status", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "IconName", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "IconThemePath", type = String.class)
@DBusProperty(access = DBusProperty.Access.READ, name = "Menu", type = DBusPath.class)
public interface KdeStatusNotifierItem extends DBusInterface, Properties {

    @DBusMemberName("ContextMenu")
    void contextMenu(int x, int y);

    @DBusMemberName("Activate")
    void activate(int x, int y);

    @DBusMemberName("SecondaryActivate")
    void secondaryActivate(int x, int y);

    @DBusMemberName("Scroll")
    void scroll(int delta, String orientation);
}
