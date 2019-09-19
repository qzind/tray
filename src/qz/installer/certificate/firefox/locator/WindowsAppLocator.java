/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer.certificate.firefox.locator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.WindowsUtilities;


import java.io.File;
import java.util.ArrayList;

import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

public class WindowsAppLocator extends AppLocator {
    protected static final Logger log = LoggerFactory.getLogger(MacAppLocator.class);
    private static String REG_TEMPLATE = "Software\\%s%s\\%s%s";

    public WindowsAppLocator(String name, String path, String version) {
        setName(name);
        setPath(path);
        setVersion(version);
    }
    public static ArrayList<AppLocator> findApp(AppAlias appAlias) {
        ArrayList<AppLocator> appList = new ArrayList<>();
        for (AppAlias.Alias alias : appAlias.aliases) {
            if (alias.vendor != null) {
                String[] suffixes = new String[]{ "", " ESR"};
                String[] prefixes = new String[]{ "", "WOW6432Node\\"};
                for (String suffix : suffixes) {
                    for (String prefix : prefixes) {
                        String key = String.format(REG_TEMPLATE, prefix, alias.vendor, alias.name, suffix);
                        AppLocator appLocator = getAppInfo(alias.name, key, suffix);
                        if (appLocator != null && !appList.contains(appLocator)) {
                            appList.add(appLocator);
                        }
                    }
                }
            }
        }
        return appList;
    }

    public static AppLocator getAppInfo(String name, String key, String suffix) {
        String version = WindowsUtilities.getRegString(HKEY_LOCAL_MACHINE, key, "CurrentVersion");
        if (version != null) {
            version = version.split(" ")[0]; // chop off (x86 ...)
            if (!suffix.isEmpty()) {
                if (key.endsWith(suffix)) {
                    key = key.substring(0, key.length() - suffix.length());
                }
                version = version + suffix;
            }
            String path = WindowsUtilities.getRegString(HKEY_LOCAL_MACHINE, key + " " + version + "\\bin", "PathToExe");
            if (path != null) {
                // SemVer: Replace spaces in suffixes with dashes
                path = new File(path).getParent();
                version = version.replaceAll(" ", "-");
                return new WindowsAppLocator(name, path, version);
            }
        }
        return null;
    }

    @Override
    boolean isBlacklisted() {
        return false;
    }
}
