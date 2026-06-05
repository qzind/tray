package qz.ui.tray.linux;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

public final class LinuxDbusMenuPropertyGroup extends Struct {

    @Position(0)
    public final int id;

    @Position(1)
    public final Map<String, Variant<?>> properties;

    public LinuxDbusMenuPropertyGroup(int id, Map<String, Variant<?>> properties) {
        this.id = id;
        this.properties = properties;
    }
}
