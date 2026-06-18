package qz.ui.tray.linux.menu;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

// D-Bus structs are positional
// This maps DBusMenu's per-item property group
// to the id plus a{sv} property dictionary expected by tray hosts
public class LinuxDbusMenuPropertyGroup extends Struct {

    @Position(0)
    private final int id;
    @Position(1)
    private final Map<String, Variant<?>> properties;

    public LinuxDbusMenuPropertyGroup(int id, Map<String, Variant<?>> properties) {
        this.id = id;
        this.properties = properties;
    }
}