package qz.ui.tray.linux.menu;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

// D-Bus structs are positional
// dbus-java's @Position annotations define the wire order
// used by the DBusMenu EventGroup signature
public class LinuxDbusMenuEvent extends Struct {

    @Position(0)
    private final int id;
    @Position(1)
    private final String eventId;
    @Position(2)
    private final Variant<?> data;
    @Position(3)
    private final UInt32 timestamp;

    // Used by dbus-java when a tray host calls EventGroup with a(isvu)
    // QZ code does not construct these directly, but without this constructor
    // dbus-java cannot materialize grouped click events such as "About" item
    public LinuxDbusMenuEvent(int id, String eventId, Variant<?> data, UInt32 timestamp) {
        this.id = id;
        this.eventId = eventId;
        this.data = data;
        this.timestamp = timestamp;
    }

    int getId() {
        return id;
    }

    String getEventId() {
        return eventId;
    }

    Variant<?> getData() {
        return data;
    }

    UInt32 getTimestamp() {
        return timestamp;
    }
}