package qz.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GtkUtilities {
    private static final Logger log = LogManager.getLogger(GtkUtilities.class);

    /**
     * Initializes Gtk2/3 and returns the desktop scaling factor, usually 1.0 or 2.0
     */
    public static double getScaleFactor() {
        try {
            GTK gtkHandle = getGtkInstance();
            if (gtkHandle != null && gtkHandle.gtk_init_check(0, null)) {
                log.debug("Initialized Gtk");

                if (gtkHandle instanceof GTK2) {
                    return getGtk2ScaleFactor((GTK2)gtkHandle);
                } else {
                    return getGtk3ScaleFactor((GTK3)gtkHandle);
                }
            } else {
                log.warn("An error occurred initializing the Gtk library");
            }
        } catch(Throwable t) {
            log.warn("An error occurred initializing the Gtk library", t);
        }
        return 1;
    }

    public static boolean isAvailable() {
        return getGtkInstance() != null;
    }

    private static GTK getGtkInstance() {
        log.debug("Finding available Gtk version...");
        // Prefer GTK3 because modern Linux desktops expose monitor scaling there
        GTK gtk = loadGtk("gtk-3", GTK3.class);
        if(gtk == null) {
            // Fall back to GTK2 for older desktops and Java runtimes
            gtk = loadGtk("gtk-x11-2.0", GTK2.class);
        }
        if(gtk == null) {
            log.warn("Not a compatible Gtk version");
        }
        return gtk;
    }

    private static <T extends GTK> T loadGtk(String library, Class<T> type) {
        try {
            // Without this approach sun.awt.UNIXToolkit is blocked
            // by Java modules which then hides GTK identification
            T gtk = Native.load(library, type);
            log.debug("Found {}", library);
            return gtk;
        } catch(Throwable t) {
            log.debug("Could not load {}: {}", library, t.getMessage());
        }
        return null;
    }

    private static double getGtk2ScaleFactor(GTK2 gtk2) {
        Pointer display = gtk2.gdk_display_get_default();
        log.debug("Gtk 2.10+ detected, calling \"gdk_screen_get_resolution\"");
        Pointer screen = gtk2.gdk_display_get_default_screen(display);
        return gtk2.gdk_screen_get_resolution(screen) / 96.0d;
    }

    private static double getGtk3ScaleFactor(GTK3 gtk3) {
        Pointer display = gtk3.gdk_display_get_default();
        int gtkMinorVersion = gtk3.gtk_get_minor_version();
        if (gtkMinorVersion < 10) {
            log.warn("Gtk 3.10+ is required to detect scaling factor, skipping.");
        } else if (gtkMinorVersion >= 22) {
            log.debug("Gtk 3.22+ detected, calling \"gdk_monitor_get_scale_factor\"");
            Pointer monitor = gtk3.gdk_display_get_primary_monitor(display);
            return gtk3.gdk_monitor_get_scale_factor(monitor);
        } else if (gtkMinorVersion >= 10) {
            log.debug("Gtk 3.10+ detected, calling \"gdk_screen_get_monitor_scale_factor\"");
            Pointer screen = gtk3.gdk_display_get_default_screen(display);
            return gtk3.gdk_screen_get_monitor_scale_factor(screen, 0);
        }
        return 1;
    }

    /**
     * Gtk2/Gtk3 wrapper
     */
    private interface GTK extends Library {
        // Gtk2.0+
        boolean gtk_init_check(int argc, String[] argv);
        Pointer gdk_display_get_default();
        Pointer gdk_display_get_default_screen (Pointer display);
    }

    private interface GTK3 extends GTK {
        // Gtk 3.0+
        int gtk_get_minor_version ();

        // Gtk 3.10-3.21
        int gdk_screen_get_monitor_scale_factor (Pointer screen, int monitor_num);

        // Gtk 3.22+
        Pointer gdk_display_get_primary_monitor (Pointer display);
        int gdk_monitor_get_scale_factor (Pointer monitor);
    }

    private interface GTK2 extends GTK {
        // Gtk 2.1-3.0
        double gdk_screen_get_resolution(Pointer screen);
    }
}
