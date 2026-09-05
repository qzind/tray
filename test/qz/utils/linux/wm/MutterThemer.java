package qz.utils.linux.wm;

public class MutterThemer extends Mutter implements Themer {
    static final String GTK_THEME_KEY = "gtk-theme";
    static final String COLOR_SCHEME_KEY = "color-scheme";

    @Override
    protected void addMatchers() {
        super.addMatchers();

        addMatcher(ExecutorList.GET_THEME, "([A-Za-z0-9_-]+)",
                   "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");

        addMatcher(ExecutorList.SET_THEME, ".*",
                   "gsettings", "set", "org.gnome.desktop.interface", GTK_THEME_KEY);

        addMatcher(ExecutorList.SET_THEME, ".*",
                   "gsettings", "set", "org.gnome.desktop.interface", COLOR_SCHEME_KEY);
    }

    @Override
    public String getThemeName(boolean isDark, Executor executor) {
        // Return the correct value depending on which shell command we're running
        String key = executor.execute[executor.execute.length - 1];
        return switch(key) {
            case GTK_THEME_KEY -> isDark? "Adwaita-dark":"Adwaita";
            case COLOR_SCHEME_KEY -> isDark? "prefer-dark":"default";
            default -> throw new UnsupportedOperationException("Can't infer theme name from key '" + key + "'");
        };
    }

}
