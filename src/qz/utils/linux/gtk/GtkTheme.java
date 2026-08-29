package qz.utils.linux.gtk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.linux.DeType;

import java.io.IOException;

public class GtkTheme {
    private static final Logger log = LogManager.getLogger(GtkTheme.class);

    public static boolean isDarkDesktop() {
        try {
            // Gnome historically uses "Yaru-dark" or similar
            return getTheme().matches("(?i).*\\bdark\\b.*");
        } catch(IOException e) {
            log.warn("Failed to get theme name using '{}'", DeType.getBinaryFound());
        }
        return false;
    }

    public static String getTheme() throws IOException{
        String theme = ShellUtilities.executeRaw(DeType.getBinaryFound(), "get", "org.gnome.desktop.interface", "gtk-theme");
        if(theme.trim().isEmpty()) {
            throw new IOException("Failed to get theme name");
        }
        // Strip quotes
        return theme.replace("\"", "").replace("'", "").trim();
    }
}
