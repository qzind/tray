package qz.utils;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GtkUtilities {
    private static final Logger log = LogManager.getLogger(GtkUtilities.class);
    private static final GTK GTK_INSTANCE = GTK.getInstance();

    public boolean isGtkAvailable() {
        return switch(SystemUtilities.getOs()) {
            case MAC, WINDOWS -> false;
            default -> GTK_INSTANCE != null;
        };
    }

    public static double getScaleFactor() {
        return GTK_INSTANCE != null ? GTK_INSTANCE.getScaleFactor() : 1.0;
    }

    enum GtkType {
        GTK3("gtk-3", GTK3.class),
        GTK2("gtk-x11-2.0", GTK2.class);

        private final String lib;
        private final Class<? extends GTK> type;

        GtkType(String lib, Class<? extends GTK> type) {
            this.lib = lib;
            this.type = type;
        }
    }

    /**
     * GTK2/GTK3 wrapper
     */
    private interface GTK extends Library {
        // Gtk2.0+
        boolean gtk_init_check(int argc, String[] argv);
        Pointer gdk_display_get_default();
        Pointer gdk_display_get_default_screen (Pointer display);

        static GTK getInstance() {
            log.debug("Finding available GTK version...");
            for(GtkType type : GtkType.values()) {
                try {
                    GTK found = Native.load(type.lib, type.type);
                    if(found != null) {
                        log.debug("Found GTK lib '{}'", type.lib);
                        if(found.gtk_init_check(0, null)) {
                            log.debug("Initialized GTK {}", found.getVersion());
                            return found;
                        }
                    }
                }
                catch(Throwable t) {
                    log.debug("Could not load {}: {}", type.lib, t.getMessage());
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

    private interface GTK3 extends GTK {
        // GTK 3.0+
        int gtk_get_major_version();
        int gtk_get_minor_version();
        int gtk_get_micro_version();

        // GTK 3.10-3.21
        int gdk_screen_get_monitor_scale_factor (Pointer screen, int monitor_num);

        // GTK 3.22+
        Pointer gdk_display_get_primary_monitor (Pointer display);
        Pointer gdk_display_get_monitor(Pointer display, int monitor_num);
        int gdk_monitor_get_scale_factor (Pointer monitor);

        default Pointer getMonitor() {
            Pointer display = gdk_display_get_default();
            if (display != null) {
                Pointer monitor = gdk_display_get_primary_monitor(display);
                if (monitor == null) {
                    log.debug("Primary monitor is null, falling back to monitor index 0");
                    monitor = gdk_display_get_monitor(display, 0);
                }
                if (monitor != null) {
                    return monitor;
                }
            }
            log.warn("Unable to obtain primary monitor");
            return null;
        }

        default Version getVersion() {
            return Version.of(gtk_get_major_version(), gtk_get_minor_version(), gtk_get_micro_version());
        }

        @Override
        default double getScaleFactor() {
            Version version = getVersion();
            if (version.isHigherThanOrEquivalentTo(Version.of(3, 10, 0))) {
                if(version.isHigherThanOrEquivalentTo(Version.of(3, 22, 0))) {
                    // use 3.22+ api
                    Pointer monitor = getMonitor();
                    if (monitor != null) {
                        int factor = gdk_monitor_get_scale_factor(monitor);
                        if (factor > 0) {
                            return factor;
                        }
                    }
                }
                // 3.10+ fallback
                Pointer screen = getScreen();
                if (screen != null) {
                    int factor = gdk_screen_get_monitor_scale_factor(screen, 0);
                    if(factor > 0) {
                        return factor;
                    }
                }
            } else {
                log.warn("GTK 3.10+ is required to detect scaling factor, skipping.");
            }
            log.warn("Unable to detect GTK3 scale factor");
            return 1.0;
        }
    }

    private interface GTK2 extends GTK {
        // GTK 2.1-3.0
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
}