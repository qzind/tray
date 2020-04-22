package qz.installer.certificate.firefox.locator;

import qz.utils.SystemUtilities;

import java.util.ArrayList;

public class AppLocator {
    public static ArrayList<AppInfo> locate(AppAlias appAlias) {
        if (SystemUtilities.isWindows()) {
            return WindowsAppLocator.findApp(appAlias);
        } else if (SystemUtilities.isMac()) {
            return MacAppLocator.findApp(appAlias);
        }
        return LinuxAppLocator.findApp(appAlias);
    }
}