package qz.installer.certificate.firefox.locator;

import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.util.ArrayList;

public abstract class AppLocator {
    protected static final org.slf4j.Logger log = LoggerFactory.getLogger(AppLocator.class);

    private static AppLocator INSTANCE = getPlatformSpecificAppLocator();

    public abstract ArrayList<AppInfo> locate(AppAlias appAlias);
    //todo bool for child culling?
    public abstract ArrayList<String> getPids(boolean exactMatch, ArrayList<String> processNames);
    public abstract ArrayList<Path> locateProcessPaths(boolean exactMatch, ArrayList<String> pids);

    public static ArrayList<Path> getRunningPaths(ArrayList<AppInfo> appList) {
        ArrayList<String> appNames = new ArrayList<>();
        for (AppInfo app : appList) {
            String exeName = app.getExePath().getFileName().toString();
            if (!appNames.contains(exeName)) appNames.add(exeName);
        }

        ArrayList<String> pids = INSTANCE.getPids(true, appNames);
        ArrayList<Path> processPaths = INSTANCE.locateProcessPaths(true, pids);
        return processPaths;
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