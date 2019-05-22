package qz.utils;

public class WindowsUtilities {
    public static boolean isDarkMode() {
        String path = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
        String name = "AppsUseLightTheme";
        // 0 = Dark Theme.  -1/1 = Light Theme
        return ShellUtilities.getRegistryDWORD(path, name) == 0;
    }
}
