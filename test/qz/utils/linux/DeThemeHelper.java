package qz.utils.linux;

import java.io.IOException;

public interface DeThemeHelper {
    static DeThemeHelper getDeThemeHelper() {
        return getDeThemeHelper(CompositorType.getDe());
    }

    static DeThemeHelper getDeThemeHelper(CompositorType compositorType) {
        return switch(compositorType) {
            case KWIN -> new KdeThemeHelper();
            case MUTTER -> new GnomeThemeHelper();
            default -> null;
        };
    }

    String getTheme() throws IOException;
    void setTheme(String themeName) throws IOException;
    void setTheme(boolean isDark) throws IOException;
}