package qz.utils.linux;

import qz.utils.ShellUtilities;

import java.io.IOException;

public class GnomeThemeHelper implements DeThemeHelper {
    @Override
    public String getTheme() throws IOException{
        String theme = ShellUtilities.executeRawSilently("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
        if(theme.trim().isEmpty()) {
            throw new IOException("Failed to get theme name");
        }
        // Strip quotes
        return theme.replace("\"", "").replace("'", "").trim();
    }

    @Override
    public void setTheme(String themeName) throws IOException {
        // legacy
        boolean legacy = ShellUtilities.execute("gsettings", "set", "org.gnome.desktop.interface", "gtk-theme", themeName);
        // modern
        boolean success = ShellUtilities.execute("gsettings", "set", "org.gnome.desktop.interface", "color-scheme", themeName.contains("dark") ? "prefer-dark" : "default");

        if(!legacy || !success) {
            throw new IOException("Fail to set GTK theme to " + themeName);
        }
    }

    @Override
    public void setTheme(boolean isDark) throws IOException {
        setTheme(isDark ? "Adwaita-dark" : "Adwaita");
    }
}
