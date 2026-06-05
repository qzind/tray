package qz.ui.tray.linux;

import org.freedesktop.dbus.interfaces.DBusSerializable;

// dbus-java uses DBusSerializable.deserialize(...) to derive
// the multi-value return signature for AboutToShowGroup.
public final class LinuxDbusMenuAboutToShowGroup implements DBusSerializable {

    public final int[] updatesNeeded;

    public final int[] idErrors;

    public LinuxDbusMenuAboutToShowGroup(int[] updatesNeeded, int[] idErrors) {
        this.updatesNeeded = updatesNeeded;
        this.idErrors = idErrors;
    }

    public static LinuxDbusMenuAboutToShowGroup deserialize(int[] updatesNeeded, int[] idErrors) {
        return new LinuxDbusMenuAboutToShowGroup(updatesNeeded, idErrors);
    }

    @Override
    public Object[] serialize() {
        return new Object[] { updatesNeeded, idErrors };
    }
}
