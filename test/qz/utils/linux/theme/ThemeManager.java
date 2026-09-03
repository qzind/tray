package qz.utils.linux.theme;

import qz.utils.linux.CompositorType;

import java.io.IOException;

public interface ThemeManager {
    static ThemeManager getThemeManager() {
        return getThemeManager(CompositorType.getCompositors());
    }

    static ThemeManager getThemeManager(CompositorType ... compositorTypes) {
        for(CompositorType compositorType : compositorTypes) {
            // Get first match
            switch(compositorType) {
                case KWIN6, KWIN5: return new KdeThemeManager();
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