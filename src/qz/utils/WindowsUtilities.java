package qz.utils;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef;
import qz.common.Constants;

import java.awt.*;

public class WindowsUtilities {
    public static boolean isDarkMode() {
        String path = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
        String name = "AppsUseLightTheme";
        // 0 = Dark Theme.  -1/1 = Light Theme
        return ShellUtilities.getRegistryDWORD(path, name, true) == 0;
    }
    public static int getScaleFactor() {
        if (Constants.JAVA_VERSION.lessThan(Version.valueOf("9.0.0"))) {
            WinDef.HDC hdc = GDI32.INSTANCE.CreateCompatibleDC(null);
            if (hdc != null) {
                int actual = GDI32.INSTANCE.GetDeviceCaps(hdc, 10 /* VERTRES */);
                int logical = GDI32.INSTANCE.GetDeviceCaps(hdc, 117 /* DESKTOPVERTRES */);
                GDI32.INSTANCE.DeleteDC(hdc);
                if (logical != 0 && logical/actual > 1) {
                    return logical/actual;
                }
            }
        }
        return (int)(Toolkit.getDefaultToolkit().getScreenResolution() / 96.0);
    }
}
