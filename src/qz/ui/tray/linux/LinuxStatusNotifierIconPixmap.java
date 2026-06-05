package qz.ui.tray.linux;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

// SNI IconPixmap is an array of (width, height, ARGB bytes) structs.
// dbus-java uses @Position to turn this class into the D-Bus `(iiay)` shape.
public class LinuxStatusNotifierIconPixmap extends Struct {

    @Position(0)
    private final int width;
    @Position(1)
    private final int height;
    @Position(2)
    private final byte[] pixels;

    LinuxStatusNotifierIconPixmap(int width, int height, byte[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }
}
