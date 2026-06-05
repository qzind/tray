package qz.ui.tray.linux;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/*
 * Minimal com.canonical.dbusmenu surface used by StatusNotifier hosts
 * when they build and activate the tray item's menu.
 *
 * References:
 * https://sources.debian.org/src/libdbusmenu/18.10.20180917~bzr492%2Brepack1-2/libdbusmenu-glib/dbus-menu.xml
 * https://hypfvieh.github.io/dbus-java/exporting-objects.html
 * https://hypfvieh.github.io/dbus-java/properties.html
 * https://hypfvieh.github.io/dbus-java/dbus-java-core/apidocs/org.freedesktop.dbus/org/freedesktop/dbus/annotations/DBusInterfaceName.html
 */
@DBusInterfaceName("com.canonical.dbusmenu")
public interface CanonicalDbusMenu extends DBusInterface {

    @DBusBoundProperty(access = DBusProperty.Access.READ, name = "Version")
    UInt32 getVersion();

    @DBusBoundProperty(access = DBusProperty.Access.READ, name = "Status")
    String getStatus();

    @DBusMemberName("GetLayout")
    LinuxDbusMenuLayout getLayout(int parentId, int recursionDepth, String[] propertyNames);

    @DBusMemberName("GetGroupProperties")
    LinuxDbusMenuPropertyGroup[] getGroupProperties(int[] ids, String[] propertyNames);

    @DBusMemberName("GetProperty")
    Variant<?> getProperty(int id, String name);

    @DBusMemberName("Event")
    void event(int id, String eventId, Variant<?> data, UInt32 timestamp);

    @DBusMemberName("EventGroup")
    int[] eventGroup(LinuxDbusMenuEvent[] events);

    @DBusMemberName("AboutToShow")
    boolean aboutToShow(int id);

    @DBusMemberName("AboutToShowGroup")
    LinuxDbusMenuAboutToShowGroup aboutToShowGroup(int[] ids);
}
