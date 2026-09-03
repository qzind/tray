package qz.utils.linux.theme;

import qz.utils.linux.compositor.Compositor;

import java.io.IOException;

// TODO: Refactor by implementing / overriding the new dispatchers
public interface ThemeManager {
    static ThemeManager getThemeManager() {
        return getThemeManager(Compositor.detectCompositor());
    }

    static ThemeManager getThemeManager(Compositor... compositors) {
        for(Compositor compositor : compositors) {
            // Get first match
            switch(compositor) {
                case KWIN: return new KdeThemeManager();
                case MUTTER: return new GnomeThemeManager();
                case XFCE: return new XfceThemeManager();
            };
        }
        return new MissingThemeManager();
    }

    String getTheme() throws IOException;
    void setTheme(String themeName) throws IOException;
    void setTheme(boolean isDark) throws IOException;
}