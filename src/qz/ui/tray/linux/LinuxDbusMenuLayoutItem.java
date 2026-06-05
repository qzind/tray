package qz.ui.tray.linux;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

// D-Bus structs are positional
// dbus-java's @Position annotations keep this
// class aligned with DBusMenu's (ia{sv}av) layout item signature
public class LinuxDbusMenuLayoutItem extends Struct {

    @Position(0)
    private final int id;
    @Position(1)
    private final Map<String, Variant<?>> properties;
    @Position(2)
    private final Variant<?>[] children;

    public LinuxDbusMenuLayoutItem(int id, Map<String, Variant<?>> properties, Variant<?>[] children) {
        this.id = id;
        this.properties = properties;
        this.children = children;
    }
}