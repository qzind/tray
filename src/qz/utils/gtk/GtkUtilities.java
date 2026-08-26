package qz.utils.gtk;

import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.UnixUtilities;

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

    public static boolean isDarkDesktop() {
        String raw = ShellUtilities.executeRaw(UnixUtilities.DesktopEnvironment.binaryFound, "get", "org.gnome.desktop.interface", "gtk-theme");
        // Gnome historically uses "Yaru-dark" or similar
        return raw.matches("(?i).*\\bdark\\b.*");
    }

}
