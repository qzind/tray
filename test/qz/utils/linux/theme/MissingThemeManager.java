package qz.utils.linux.theme;

import org.testng.SkipException;

public class MissingThemeManager implements ThemeManager {
    @Override
    public String getTheme() {
        throw new SkipException("Unable to get theme.  Coverage missing for this compositor/desktop environment");
    }

    @Override
    public void setTheme(String themeName) {
        throw new SkipException("Unable to set theme.  Coverage missing for this compositor/desktop environment");
    }

    @Override
    public void setTheme(boolean isDark) {
        setTheme(null);
    }
}
