package qz.utils.linux.compositor.dispatcher;

import org.testng.SkipException;

public class UnknownStrings implements ThemeStrings {
    @Override
    public String[][] themeSetters(boolean isDark) {
        throw new SkipException("Coverage missing for this compositor/desktop environment");
    }

    @Override
    public String[][] themeSetters(String themeName) {
        throw new SkipException("Coverage missing for this compositor/desktop environment");
    }

    @Override
    public String[][] themeGetters() {
        throw new SkipException("Coverage missing for this compositor/desktop environment");
    }

    @Override
    public String getThemeName(boolean isDark) {
        throw new SkipException("Coverage missing for this compositor/desktop environment");
    }
}
