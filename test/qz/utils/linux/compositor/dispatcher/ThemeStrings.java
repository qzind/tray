package qz.utils.linux.compositor.dispatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface ThemeStrings {
    Logger log = LogManager.getLogger(ThemeStrings.class);

    String[][] themeGetters();

    String[][] themeSetters(boolean isDark);

    String[][] themeSetters(String themeName);

    String getThemeName(boolean isDark);
}