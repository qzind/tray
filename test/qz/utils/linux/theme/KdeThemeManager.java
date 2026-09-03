package qz.utils.linux.theme;

import qz.utils.ShellUtilities;

import java.io.IOException;

public class KdeThemeManager implements ThemeManager {
    @Override
    public String getTheme() throws IOException {
        String theme = ShellUtilities.executeRaw("kreadconfig6", "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme");
        if(theme.trim().isEmpty()) {
            theme = ShellUtilities.executeRaw("kreadconfig5", "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme");
            if(theme.trim().isEmpty()) {
                throw new IOException("Failed to get theme name");
            }
        }
        // Strip quotes
        return theme.replace("\"", "").replace("'", "").trim();
    }

    @Override
    public void setTheme(String themeName) throws IOException {
        boolean success = ShellUtilities.execute("plasma-apply-colorscheme", themeName);
        if(!success) {
            throw new IOException("Fail to set KDE theme to " + themeName);
        }
    }

    @Override
    public void setTheme(boolean isDark) throws IOException {
        setTheme(isDark ? "BreezeDark" : "BreezeLight");
    }
}
