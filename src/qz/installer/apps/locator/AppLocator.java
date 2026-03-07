package qz.installer.apps.locator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AppLocator {
    protected static final Logger log = LogManager.getLogger(AppLocator.class);

    private static final AppLocator INSTANCE = getPlatformSpecificAppLocator();

    public abstract HashSet<AppInfo> locate(AppAlias appAlias);
    public abstract HashMap<String, Path> getPidPaths(Set<String> pids);

    /**
     * Joins the given list of apps and processes on exePath
     */
    private HashMap<String, AppInfo> join(HashMap<String, Path> pathMap, HashSet<AppInfo> appList) {
        HashMap<String, AppInfo> pidMap = new HashMap<>();
        for(Map.Entry<String, Path> pathEntry : pathMap.entrySet()) {
            for(AppInfo appInfo : appList) {
                if(pidMap.containsKey(pathEntry.getKey())) {
                    continue;
                }
                if(appInfo.getExePath().equals(pathEntry.getValue())) {
                    pidMap.put(pathEntry.getKey(), appInfo);
                    break;
                }
            }
        }
        return pidMap;
    }

    public HashSet<String> getPids(HashSet<AppInfo> appList) {
        HashSet<String> processNames = appList.stream()
                .map(appInfo -> appInfo.getExePath().getFileName().toString())
                .collect(Collectors.toCollection(HashSet::new));

        return getPidsByName(processNames);
    }

    /**
     * Linux, Mac
     */
    public HashSet<String> getPidsByName(HashSet<String> processNames) {
        HashSet<String> pids = new HashSet<>();
        if (processNames.isEmpty()) return pids;


        // We can't find an app by path (only by name) so we have to crawl potentially matching patterns
        // in hopes to find a pid that we can then check against our appList.
        // Quoting handled by the command processor (e.g. pgrep -x "myapp|my app" is perfectly valid)
        String pgrepOutput = ShellUtilities.executeRaw("pgrep", "-x", String.join("|", processNames));

        if (!pgrepOutput.isBlank()) {
            String[] response = pgrepOutput.split("\\s*\\r?\\n");
            Collections.addAll(pids, response);
        }

        return pids;
    }

    /**
     * Gets the path to the running executables matching on <code>AppInfo.getExePath</code>
     * This is resource intensive; if a non-null <code>cache</code> is provided, it will return that instead
     */
    public HashMap<String,AppInfo> getRunningApps(HashSet<AppInfo> appList, HashMap<String,AppInfo> cache) {
        if(cache == null) {
            HashMap<String,Path> pathMap = getPidPaths(getPids(appList));
            cache = join(pathMap, appList);

            // flatpak uses its own process listing
            if(this instanceof LinuxAppLocator) {
                LinuxAppLocator locator = (LinuxAppLocator)this;
                cache.putAll(locator.getFlatpakPids(appList));
            }

        }
        return cache;
    }

    public static AppLocator getInstance() {
        return INSTANCE;
    }

    private static AppLocator getPlatformSpecificAppLocator() {
        switch(SystemUtilities.getOs()) {
            case WINDOWS:
                return new WindowsAppLocator();
            case MAC:
                return new MacAppLocator();
            default:
                return new LinuxAppLocator();
        }
    }
}
