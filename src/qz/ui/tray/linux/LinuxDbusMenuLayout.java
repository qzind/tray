package qz.ui.tray.linux;

import org.freedesktop.dbus.interfaces.DBusSerializable;
import org.freedesktop.dbus.types.UInt32;

// dbus-java uses DBusSerializable.deserialize(...) to derive
// the multi-value return signature for GetLayout.
public final class LinuxDbusMenuLayout implements DBusSerializable {

    public final UInt32 revision;

    public final LinuxDbusMenuLayoutItem layout;

    public LinuxDbusMenuLayout(UInt32 revision, LinuxDbusMenuLayoutItem layout) {
        this.revision = revision;
        this.layout = layout;
    }

    public static LinuxDbusMenuLayout deserialize(UInt32 revision, LinuxDbusMenuLayoutItem layout) {
        return new LinuxDbusMenuLayout(revision, layout);
    }

    @Override
    public Object[] serialize() {
        return new Object[] { revision, layout };
    }
}
