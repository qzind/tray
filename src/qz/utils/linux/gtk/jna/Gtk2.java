package qz.utils.linux.gtk.jna;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

interface Gtk2 extends Gtk {
    // Gtk 2.1-3.0
    double gdk_screen_get_resolution(Pointer screen);

    default Version getVersion() {
        NativeLibrary lib = Native.getNativeLibrary(this);
        try {
            return Version.of(
                    lib.getGlobalVariableAddress("gtk_major_version").getInt(0),
                    lib.getGlobalVariableAddress("gtk_minor_version").getInt(0),
                    lib.getGlobalVariableAddress("gtk_micro_version").getInt(0));
        } catch(Throwable t) {
            log.debug("Failed to read GTK2 version info", t);
            return Version.of(0);
        }
    }

    @Override
    default double getScaleFactor() {
        Pointer screen = getScreen();
        if(screen != null) {
            double dpi = gdk_screen_get_resolution(screen);
            if(dpi > 0) {
                return dpi / 96.0d;
            }
        }
        log.warn("Unable to detect GTK2 scale factor");
        return 1.0;
    }
}
