package qz.utils.linux.gtk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.linux.DeTheme;
import qz.utils.linux.DeType;

import java.io.IOException;

import static qz.utils.linux.DeType.getBinaryFound;

public class GtkTheme implements DeTheme {
    private static final Logger log = LogManager.getLogger(GtkTheme.class);

    @Override
    public boolean isDarkDesktop() {
        try {
            // Gnome historically uses "Yaru-dark" or similar
            return getTheme().matches("(?i).*\\bdark\\b.*");
        } catch(IOException e) {
            log.warn("Failed to get theme name using '{}'", DeType.getBinaryFound());
        }
        return false;
    }

    @Override
    public String getTheme() throws IOException{
        String theme = ShellUtilities.executeRawSilently(DeType.getBinaryFound(), "get", "org.gnome.desktop.interface", "gtk-theme");
        if(theme.trim().isEmpty()) {
            throw new IOException("Failed to get theme name");
        }
        // Strip quotes
        return theme.replace("\"", "").replace("'", "").trim();
    }

    @Override
    public void setTheme(String themeName) throws IOException {
        boolean success = ShellUtilities.execute(getBinaryFound(), "set", "org.gnome.desktop.interface", "gtk-theme", themeName);
        if(!success) {
            throw new IOException("Fail to set GTK theme to " + themeName);
        }
    }

    @Override
    public void setTheme(boolean isDark) throws IOException {
        setTheme(isDark ? "Adwaita-dark" : "Adwaita");
    }
}
