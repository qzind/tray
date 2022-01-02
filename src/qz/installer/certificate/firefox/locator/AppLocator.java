package qz.installer.certificate.firefox.locator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public abstract class AppLocator {
    protected static final Logger log = LogManager.getLogger(AppLocator.class);

    private static AppLocator INSTANCE = getPlatformSpecificAppLocator();

    public abstract ArrayList<AppInfo> locate(AppAlias appAlias);
    public abstract ArrayList<Path> getPidPaths(ArrayList<String> pids);

    public ArrayList<String> getPids(String ... processNames) {
        return getPids(new ArrayList<>(Arrays.asList(processNames)));
    }

    /**
     * Linux, Mac
     */
    public ArrayList<String> getPids(ArrayList<String> processNames) {
        String[] response;
        ArrayList<String> pidList = new ArrayList<>();

        if(processNames.contains("firefox") && !(SystemUtilities.isWindows() || SystemUtilities.isMac())) {
            processNames.add("MainThread"); // Workaround Firefox 79 https://github.com/qzind/tray/issues/701
            processNames.add("GeckoMain");  // Workaround Firefox 94 https://bugzilla.mozilla.org/show_bug.cgi?id=1742606
        }

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
        return getRunningPaths(appList, null);
    }

    /**
     * Gets the path to the running executables matching on <code>AppInfo.getExePath</code>
     * This is resource intensive; if a non-null <code>cache</code> is provided, it will return that instead
     */
    public static ArrayList<Path> getRunningPaths(ArrayList<AppInfo> appList, ArrayList<Path> cache) {
        if(cache == null) {
            ArrayList<String> appNames = new ArrayList<>();
            for(AppInfo app : appList) {
                String exeName = app.getExePath().getFileName().toString();
                if (!appNames.contains(exeName)) appNames.add(exeName);
            }
            cache = INSTANCE.getPidPaths(INSTANCE.getPids(appNames));
        }

        return cache;
    }

    public static AppLocator getInstance() {
        return INSTANCE;
    }

    private static AppLocator getPlatformSpecificAppLocator() {
        switch(SystemUtilities.getOsType()) {
            case WINDOWS:
                return new WindowsAppLocator();
            case MAC:
                return new MacAppLocator();
            default:
                return new LinuxAppLocator();
        }
    }
}
