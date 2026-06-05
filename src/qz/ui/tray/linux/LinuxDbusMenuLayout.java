package qz.ui.tray.linux;

import org.freedesktop.dbus.interfaces.DBusSerializable;
import org.freedesktop.dbus.types.UInt32;

// DBusMenu GetLayout returns two D-Bus values: revision and layout
// dbus-java uses DBusSerializable.deserialize(...) and serialize()
// to model that multi-value return
//
// References:
// https://dbus.freedesktop.org/doc/dbus-java/dbus-java.pdf
// https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-core/src/main/java/org/freedesktop/dbus/Marshalling.java
public class LinuxDbusMenuLayout implements DBusSerializable {

    private final UInt32 revision;
    private final LinuxDbusMenuLayoutItem layout;

    public LinuxDbusMenuLayout(UInt32 revision, LinuxDbusMenuLayoutItem layout) {
        this.revision = revision;
        this.layout = layout;
    }

    // Required by dbus-java during exportObject(...), when it generates
    // introspection XML and derives the D-Bus return signature for GetLayout
    // The method body is unused in this POC, but removing
    // the method causes the error:
    // "Serializable classes must implement a deserialize method"
    @SuppressWarnings("unused")
    public void deserialize(UInt32 revision, LinuxDbusMenuLayoutItem layout) {}

    @Override
    public Object[] serialize() {
        return new Object[] { revision, layout };
    }
}