//TODO find a better home and name for me
package qz.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class DPI {
    public static int getDPI () {
        if (!GTK.INSTANCE.gtk_init_check(0, null)) return 0;

        Pointer display = GTK.INSTANCE.gdk_display_get_default();
        int version = GTK.INSTANCE.gtk_get_minor_version();

        int factor;
        if (version >= 22) {
            Pointer monitor = GTK.INSTANCE.gdk_display_get_primary_monitor(display);
            factor = GTK.INSTANCE.gdk_monitor_get_scale_factor(monitor);
        } else if (version >= 10) {
            Pointer screen = GTK.INSTANCE.gdk_display_get_default_screen(display);
            factor = GTK.INSTANCE.gdk_screen_get_monitor_scale_factor(screen, 0);
        } else {
            factor = 0;
        }

        return factor;
    }

    private interface GTK extends Library {
        GTK INSTANCE = (GTK) Native.loadLibrary("gtk-3", GTK.class);
        boolean gtk_init_check(int argc, String[] argv);

        int gtk_get_minor_version ();
        Pointer gdk_display_get_default();
        Pointer gdk_display_get_default_screen (Pointer display);

        //3.10 to 3.21
        int gdk_screen_get_monitor_scale_factor (Pointer screen, int monitor_num);

        //3.22+
        Pointer gdk_display_get_primary_monitor (Pointer display);
        int gdk_monitor_get_scale_factor (Pointer monitor);
    }
}