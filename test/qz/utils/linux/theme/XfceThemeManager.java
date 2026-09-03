package qz.utils.linux.theme;

import qz.utils.ShellUtilities;

import java.io.IOException;

public class XfceThemeManager implements ThemeManager {

    @Override
    public String getTheme() throws IOException {
        String theme = ShellUtilities.executeRaw("xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName").trim();
        if(theme.trim().isEmpty()) {
            throw new IOException("Failed to get theme name");
        }
        // Strip quotes
        return theme.replace("\"", "").replace("'", "").trim();
    }

    @Override
    public void setTheme(String themeName) throws IOException {
        boolean success = ShellUtilities.execute("xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName", "-s", themeName);
        if(!success) {
            throw new IOException("Fail to set XFCE theme to " + themeName);
        }
    }

    @Override
    public void setTheme(boolean isDark) throws IOException {
        setTheme(isDark ? "Greybird-dark" : "Greybird");
    }
}
