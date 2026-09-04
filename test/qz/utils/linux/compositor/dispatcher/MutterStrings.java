package qz.utils.linux.compositor.dispatcher;


public class MutterStrings implements ThemeStrings {
    @Override
    public String[][] themeGetters() {
        return new String[][] {
            {"gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"}
        };
    }

    @Override
    public String[][] themeSetters(String themeName) {
        return new String[][] {
                {"gsettings", "set", "org.gnome.desktop.interface", "gtk-theme", themeName},
                {"gsettings", "set", "org.gnome.desktop.interface", "color-scheme", Dispatcher.isDark(themeName) ? "prefer-dark" : "default"}
        };
    }

    @Override
    public String[][] themeSetters(boolean isDark) {
        return themeSetters(getThemeName(isDark));
    }

    @Override
    public String getThemeName(boolean isDark) {
        return isDark ? "Adwaita-dark" : "Adwaita";
    }
}
