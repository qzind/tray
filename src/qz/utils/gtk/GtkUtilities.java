package qz.utils.gtk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.UnixUtilities;

import java.io.IOException;

public class GtkUtilities {
    private static final Logger log = LogManager.getLogger(GtkUtilities.class);

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
        try {
            // Gnome historically uses "Yaru-dark" or similar
            return getTheme().matches("(?i).*\\bdark\\b.*");
        } catch(IOException e) {
            log.warn("Failed to get theme name using '{}'", UnixUtilities.DesktopEnvironment.binaryFound);
        }
        return false;
    }

    public static String getTheme() throws IOException{
        String theme = ShellUtilities.executeRaw(UnixUtilities.DesktopEnvironment.binaryFound, "get", "org.gnome.desktop.interface", "gtk-theme");
        if(theme.trim().isEmpty()) {
            throw new IOException("Failed to get theme name");
        }
        // Strip quotes
        return theme.replace("\"", "").replace("'", "").trim();
    }
}
