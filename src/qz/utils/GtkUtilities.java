package qz.utils;

import qz.utils.gtk.Gtk;

public class GtkUtilities {
    static final Gtk GTK_INSTANCE = Gtk.getInstance();

    public static boolean isGtkAvailable() {
        return switch(SystemUtilities.getOs()) {
            case MAC, WINDOWS -> false;
            default -> GTK_INSTANCE != null;
        };
    }

    public static double getScaleFactor() {
        return GTK_INSTANCE != null? GTK_INSTANCE.getScaleFactor():1.0;
    }
}
