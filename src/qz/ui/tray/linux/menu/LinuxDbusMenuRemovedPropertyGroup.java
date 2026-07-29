package qz.ui.tray.linux.menu;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

// Maps the removed-properties half of ItemsPropertiesUpdated
// D-Bus signature: (ias)
public class LinuxDbusMenuRemovedPropertyGroup extends Struct {

    @Position(0)
    private final int id;
    @Position(1)
    private final String[] properties;

    public LinuxDbusMenuRemovedPropertyGroup(int id, String[] properties) {
        this.id = id;
        this.properties = properties;
    }
}
