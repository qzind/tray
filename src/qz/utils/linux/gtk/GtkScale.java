package qz.utils.linux.gtk;

import qz.utils.SystemUtilities;
import qz.utils.linux.gtk.jna.Gtk;

public class GtkScale {
    static Gtk GTK_INSTANCE = null;
    static boolean gtkInit = false;

    public static boolean isGtkAvailable() {
        return switch(SystemUtilities.getOs()) {
            case MAC, WINDOWS -> false;
            default -> getGtkInstance() != null;
        };
    }

    public static Gtk getGtkInstance() {
        if(!gtkInit) {
            GTK_INSTANCE = Gtk.newInstance();
            gtkInit = true;
        }
        return GTK_INSTANCE;
    }

    public static double getScaleFactor() {
        return getGtkInstance() != null? GTK_INSTANCE.getScaleFactor():1.0;
    }
}
