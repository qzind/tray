package qz.utils.linux.compositor.dispatcher;

public class KwinStrings implements ThemeStrings {
    @Override
    public String[][] themeGetters() {
        return new String[][] {
                {"kreadconfig6", "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme"},
                {"kreadconfig5", "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme"}
        };
    }

    @Override
    public String[][] themeSetters(String themeName) {
        return new String[][] {
                {"plasma-apply-colorscheme", themeName},
        };
    }

    @Override
    public String[][] themeSetters(boolean isDark) {
        return themeSetters(getThemeName(isDark));
    }

    @Override
    public String getThemeName(boolean isDark) {
        return isDark? "BreezeDark":"BreezeLight";
    }
}
