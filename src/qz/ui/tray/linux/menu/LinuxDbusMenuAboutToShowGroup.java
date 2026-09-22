package qz.ui.tray.linux.menu;

import org.freedesktop.dbus.interfaces.DBusSerializable;

// DBusMenu AboutToShowGroup returns two D-Bus arrays
// DBusSerializable keeps that multi-value return explicit
// without adding a project-specific wrapper API
//
// References:
// https://dbus.freedesktop.org/doc/dbus-java/dbus-java.pdf
// https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-core/src/main/java/org/freedesktop/dbus/Marshalling.java
public class LinuxDbusMenuAboutToShowGroup implements DBusSerializable {

    private final int[] updatesNeeded;
    private final int[] idErrors;

    public LinuxDbusMenuAboutToShowGroup(int[] updatesNeeded, int[] idErrors) {
        this.updatesNeeded = updatesNeeded;
        this.idErrors = idErrors;
    }

    // Required by dbus-java during exportObject(...), when it generates
    // introspection XML and derives the D-Bus return signature for AboutToShowGroup
    // The method body is unused in this POC, but removing
    // the method causes the error:
    // "Serializable classes must implement a deserialize method"
    @SuppressWarnings("unused")
    public void deserialize(int[] updatesNeeded, int[] idErrors) {}

    @Override
    public Object[] serialize() {
        return new Object[] { updatesNeeded, idErrors };
    }
}