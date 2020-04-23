package qz.installer.certificate.firefox.locator;

import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class AppLocator {
    protected static final org.slf4j.Logger log = LoggerFactory.getLogger(AppLocator.class);

    private static AppLocator INSTANCE = getPlatformSpecificAppLocator();

    public abstract ArrayList<AppInfo> locate(AppAlias appAlias);
    public abstract ArrayList<Path> getPidPaths(ArrayList<String> pids);
    public abstract ArrayList<String> getPids(boolean parentPids, ArrayList<String> processNames);

    public ArrayList<String> getPids(ArrayList<String> processNames) {
        return getPids(true, processNames);
    }
    public ArrayList<String> getPids(boolean parentPids, String ... processNames) {
        return getPids(parentPids, new ArrayList<>(Arrays.asList(processNames)));
    }
    public static ArrayList<Path> getRunningPaths(ArrayList<AppInfo> appList) {
        ArrayList<String> appNames = new ArrayList<>();
        for (AppInfo app : appList) {
            String exeName = app.getExePath().getFileName().toString();
            if (!appNames.contains(exeName)) appNames.add(exeName);
        }

        return INSTANCE.getPidPaths(INSTANCE.getPids(appNames));
    }

    public static AppLocator getInstance() {
        return INSTANCE;
    }

    private static AppLocator getPlatformSpecificAppLocator() {
        if (SystemUtilities.isWindows()) {
            return new WindowsAppLocator();
        } else if (SystemUtilities.isMac()) {
            return new MacAppLocator();
        }
        return new LinuxAppLocator();
    }
}