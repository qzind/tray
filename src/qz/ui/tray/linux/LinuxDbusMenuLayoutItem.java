package qz.ui.tray.linux;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.Variant;

import java.util.Map;

public final class LinuxDbusMenuLayoutItem extends Struct {

    @Position(0)
    public final int id;

    @Position(1)
    public final Map<String, Variant<?>> properties;

    @Position(2)
    public final Variant<?>[] children;

    public LinuxDbusMenuLayoutItem(int id, Map<String, Variant<?>> properties, Variant<?>[] children) {
        this.id = id;
        this.properties = properties;
        this.children = children;
    }
}
