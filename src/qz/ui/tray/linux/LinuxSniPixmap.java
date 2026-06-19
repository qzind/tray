package qz.ui.tray.linux;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;

// Defines the a(iiay) StatusNotifier icon pixmap signature
// Named icons can return an empty array like KDE Connect
public class LinuxSniPixmap extends Struct {

    @Position(0)
    private final int width;
    @Position(1)
    private final int height;
    @Position(2)
    private final byte[] data;

    public LinuxSniPixmap(int width, int height, byte[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getData() {
        return data;
    }
}
