package qz.utils.gtk;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.Pointer;

interface Gtk3 extends Gtk {
    // Gtk 3.0+
    int gtk_get_major_version();
    int gtk_get_minor_version();
    int gtk_get_micro_version();

    // Gtk 3.10-3.21
    int gdk_screen_get_monitor_scale_factor (Pointer screen, int monitor_num);

    // Gtk 3.22+
    Pointer gdk_display_get_primary_monitor(Pointer display);
    Pointer gdk_display_get_monitor(Pointer display, int monitor_num);
    int gdk_monitor_get_scale_factor(Pointer monitor);

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

    @Override
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
            log.warn("Gtk 3.10+ is required to detect scaling factor, skipping.");
        }
        log.warn("Unable to detect Gtk3 scale factor");
        return 1.0;
    }
}