package qz.utils.linux;

import java.io.IOException;

public interface DeThemeHelper {
    static DeThemeHelper getDeThemeHelper() {
        return getDeThemeHelper(DisplayServerType.getDeType());
    }

    static DeThemeHelper getDeThemeHelper(DisplayServerType displayServerType) {
        return switch(displayServerType) {
            case KWIN -> new KdeThemeHelper();
            case MUTTER -> new GnomeThemeHelper();
            default -> null;
        };
    }

    String getTheme() throws IOException;
    void setTheme(String themeName) throws IOException;
    void setTheme(boolean isDark) throws IOException;
}