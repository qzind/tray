package qz.utils.linux;

import java.io.IOException;

public interface DeThemeHelper {
    static DeThemeHelper getDeThemeHelper() {
        return getDeThemeHelper(CompositorType.getCompositors());
    }

    static DeThemeHelper getDeThemeHelper(CompositorType ... compositorTypes) {
        for(CompositorType compositorType : compositorTypes) {
            // Get first match
            switch(compositorType) {
                case KWIN6, KWIN5: return new KdeThemeHelper();
                case MUTTER: return new GnomeThemeHelper();
            };
        }
        return null;
    }

    String getTheme() throws IOException;
    void setTheme(String themeName) throws IOException;
    void setTheme(boolean isDark) throws IOException;
}