package qz.installer.certificate.firefox.locator;

import org.slf4j.LoggerFactory;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public abstract class AppLocator {
    protected static final org.slf4j.Logger log = LoggerFactory.getLogger(AppLocator.class);

    private static AppLocator INSTANCE = getPlatformSpecificAppLocator();

    public abstract ArrayList<AppInfo> locate(AppAlias appAlias);
    public abstract ArrayList<Path> getPidPaths(ArrayList<String> pids);

    public ArrayList<String> getPids(ArrayList<String> processNames) {
        return getPids(true, processNames);
    }
    public ArrayList<String> getPids(boolean parentPids, String ... processNames) {
        return getPids(parentPids, new ArrayList<>(Arrays.asList(processNames)));
    }

    /**
     * Linux, Mac
     */
    public ArrayList<String> getPids(boolean unused, ArrayList<String> processNames) {
        String[] response;
        ArrayList<String> pidList = new ArrayList<>();

        if (processNames.size() == 0) return pidList;

        // Quoting handled by the command processor (e.g. pgrep -x "myapp|my app" is perfectly valid)
        String data = ShellUtilities.executeRaw("pgrep", "-x", String.join("|", processNames));

        //Splitting an empty string results in a 1 element array, this is not what we want
        if (!data.isEmpty()) {
            response = data.split("\\s*\\r?\\n");
            Collections.addAll(pidList, response);
        }

        return pidList;
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