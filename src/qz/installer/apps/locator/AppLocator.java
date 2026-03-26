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

    public abstract HashSet<ResolvedApp> locate(AppFamily appFamily);
    public abstract HashMap<String, Path> getPidPaths(Set<String> pids);

    /**
     * Joins the given list of apps and processes on exePath
     */
    private HashMap<String,ResolvedApp> join(HashMap<String, Path> pathMap, HashSet<ResolvedApp> resolvedApps) {
        HashMap<String,ResolvedApp> pidMap = new HashMap<>();
        for (Map.Entry<String, Path> pathEntry : pathMap.entrySet()) {
            for (ResolvedApp app : resolvedApps) {
                if (app.getExePath().equals(pathEntry.getValue())) {
                    pidMap.put(pathEntry.getKey(), app);
                    break;
                }
            }
        }
        return pidMap;
    }

    public HashSet<String> getPids(HashSet<ResolvedApp> resolvedApps) {
        HashSet<String> processNames = resolvedApps.stream()
                .map(app -> app.getExePath().getFileName().toString())
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
        // in hopes to find a pid that we can then check against our resolved apps.
        // Quoting handled by the command processor (e.g. pgrep -x "myapp|my app" is perfectly valid)
        String pgrepOutput = ShellUtilities.executeRaw("pgrep", "-x", String.join("|", processNames));

        if (!pgrepOutput.isBlank()) {
            String[] response = pgrepOutput.split("\\s*\\r?\\n");
            Collections.addAll(pids, response);
        }

        return pids;
    }

    /**
     * Gets the path to the running executables matching on <code>ResolvedApp.getExePath</code>
     * This is resource intensive; if a non-null <code>cache</code> is provided, it will return that instead
     */
    public HashMap<String,ResolvedApp> getRunningApps(HashSet<ResolvedApp> resolvedApps, HashMap<String,ResolvedApp> cache) {
        if(cache == null) {
            HashMap<String,Path> pathMap = getPidPaths(getPids(resolvedApps));
            cache = join(pathMap, resolvedApps);

            // flatpak uses its own process listing
            if(this instanceof LinuxAppLocator) {
                LinuxAppLocator locator = (LinuxAppLocator)this;
                cache.putAll(locator.getFlatpakPids(resolvedApps));
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
