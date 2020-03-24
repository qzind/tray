package qz.utils;

import java.awt.*;

public class WindowsUtilities {
    public static boolean isDarkMode() {
        String path = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
        String name = "AppsUseLightTheme";
        // 0 = Dark Theme.  -1/1 = Light Theme
        return ShellUtilities.getRegistryDWORD(path, name) == 0;
    }

    public static int getScaleFactor() {
        // JDK9+ Only
        return (int)(Toolkit.getDefaultToolkit().getScreenResolution() / 96.0);
    }
}
