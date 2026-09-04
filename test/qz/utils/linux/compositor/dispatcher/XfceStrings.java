package qz.utils.linux.compositor.dispatcher;

public class XfceStrings implements ThemeStrings {
    @Override
    public String[][] themeGetters() {
        return new  String[][] {
                {"xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName"}
        };
    }

    @Override
    public String[][] themeSetters(String themeName) {
        return new String[][] {
                {"xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName", "-s", themeName}
        };
    }

    @Override
    public String[][] themeSetters(boolean isDark) {
        return themeSetters(getThemeName(isDark));
    }

    @Override
    public String getThemeName(boolean isDark) {
        return isDark ? "Greybird-dark" : "Greybird";
    }
}
