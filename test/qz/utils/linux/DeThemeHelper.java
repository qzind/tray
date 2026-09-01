package qz.utils.linux;

import java.io.IOException;

public interface DeThemeHelper {
    static DeThemeHelper getDeThemeHelper() {
        return getDeThemeHelper(DeType.getDeType());
    }

    static DeThemeHelper getDeThemeHelper(DeType deType) {
        return switch(deType) {
            case KDE -> new KdeThemeHelper();
            case GNOME -> new GnomeThemeHelper();
            default -> null;
        };
    }

    String getTheme() throws IOException;
    void setTheme(String themeName) throws IOException;
    void setTheme(boolean isDark) throws IOException;
}