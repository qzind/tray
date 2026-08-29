package qz.utils.linux.gtk.jna;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GTK2/GTK3/GTK4 wrapper
 */
public interface Gtk extends Library {
    Logger log = LogManager.getLogger(Gtk.class);

    // Gtk2.0+
    boolean gtk_init_check(int argc, String[] argv);
    Pointer gdk_display_get_default();
    Pointer gdk_display_get_default_screen(Pointer display);

    static Gtk newInstance() {
        return newInstance(true);
    }

    static Gtk newInstance(boolean init) {
        log.debug("Finding available GTK version...");
        for(GtkType type : GtkType.values()) {
            try {
                Gtk found = Native.load(type.lib, type.type);
                if(found != null) {
                    log.debug("Found GTK {} as '{}'", found.getVersion(), type.lib);
                    if(init) {
                        if (found.gtk_init_check(0, null)) {
                            log.debug("Initialized GTK {}", found.getVersion());
                            return found;
                        } else {
                            log.warn("Unable to initialize GTK {} as '{}'", found.getVersion(), type.lib);
                        }
                    } else {
                        log.warn("Skipping initialization of GTK {} as '{}'.  This should only occur in unit tests.", found.getVersion(), type.lib);
                        return found;
                    }
                }
            }
            catch(Throwable t) {
                log.debug("Could not load GTK as '{}'", type.lib);
            }
        }
        log.warn("Could not find a compatible GTK version");
        return null;
    }

    Version getVersion();
    double getScaleFactor();

    default Pointer getScreen() {
        Pointer display = gdk_display_get_default();
        if (display != null) {
            Pointer screen = gdk_display_get_default_screen(display);
            if (screen != null) {
                return screen;
            }
        }
        log.warn("Unable to obtain default screen");
        return null;
    }
}
