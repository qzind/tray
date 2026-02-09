package qz.installer.apps.locator;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.apps.AppVersionParser;
import qz.utils.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class LinuxAppLocator extends AppLocator {
    private static final Logger log = LogManager.getLogger(LinuxAppLocator.class);
    private static final String[] RESTRICTED = { "/bin", "/usr/bin", "/usr/local/bin" };
    public static final Path FLATPAK_PATH = getFlatpakPath();

    public HashSet<AppInfo> locate(AppAlias appAlias) {
        HashSet<AppInfo> appList = new HashSet<>();

        // Conventional apps and snaps
        for(AppAlias.Alias alias : appAlias.aliases) {
            String exeName = alias.getSlug();
            String[] envPath = { "PATH=" + patternPathAppender(exeName, "/usr/lib/$/bin", "/usr/lib64/$/bin", "/usr/lib/$", "/usr/lib64/$") };
            String exeString = ShellUtilities.executeRaw(envPath,"which", exeName);
            if(exeString.isBlank()) {
                log.info("Could not find '{}' '{}' on this system, skipping.", alias.getVendor(), alias.getName(true));
                continue; // can't find it, let's move on
            }
            Path exePath = Paths.get(exeString.trim());
            if (Files.isRegularFile(exePath) && Files.isExecutable(exePath)) {
                log.info("Found '{}' '{}': '{}', investigating...", alias.getVendor(), alias.getName(true), exePath);
                try {
                    // Canonicalize (unless it's a snap)
                    exePath = toRealPath(exePath);

                    // Short-circuit for firefox.sh and friends
                    exePath = legacyDotShRemoval(exePath);

                    if(isShellScript(exePath)) {
                        exePath = parseScript(exePath, alias.getSlug());
                    } else {
                        log.info("Assuming '{}' '{}' is installed: '{}'", alias.getVendor(), alias.getName(true), exePath);
                    }

                    if(exePath == null) {
                        log.info("'{}' '{}' was not found on this system.", alias.getVendor(), alias.getName(true));
                        continue;
                    }

                    String[] envHome = { "HOME=/tmp" }; // firefox --version 'sudo' workaround per https://stackoverflow.com/questions/52941623
                    String stdOut = ShellUtilities.executeRaw(envHome, exePath.toString(), "--version");

                    AppInfo appInfo = new AppInfo(
                            alias,
                            getAppPath(exePath),
                            exePath,
                            AppVersionParser.parseStdOut(stdOut)
                    );

                    if(!isRestricted(appInfo)) {
                        appList.add(appInfo);
                    } else {
                        log.warn("App path '{}' is restricted, skipping", appInfo.getAppPath());
                    }
                } catch(Exception e) {
                    log.warn("Something went wrong getting app info for '{}' '{}'", alias.getVendor(), alias.getName(true), e);
                }
            }
        }

        // Flatpak apps
        if(FLATPAK_PATH != null) {
            for(AppAlias.Alias alias : appAlias.aliases) {
                AppInfo flatpakInfo = getFlatpakAppInfo(alias);
                if(flatpakInfo != null) {
                    appList.add(flatpakInfo);
                }
            }
        }

        return appList;
    }

    private AppInfo getFlatpakAppInfo(AppAlias.Alias alias) {
        if(FLATPAK_PATH == null) {
            log.error("An attempt to call 'flatpak' was made on this system, but it does not appear to be installed");
            return null;
        }

        // Call "flatpak info org.mozilla.firefox"
        String appInfo = ShellUtilities.executeRaw(FLATPAK_PATH.toString(), "info", alias.getBundleId());
        if(appInfo.isEmpty()) {
            return null;
        }
        String[] lines = appInfo.split("[\r?\n]+");
        String version = null;
        for(String line : lines) {
            if(line.trim().startsWith("Version:")) {
                version = line.trim().split(":", 2)[1].trim();
            }
        }
        if(version == null || version.isBlank()) {
            return null;
        }
        // Call "flatpak info --show-location org.mozilla.firefox"
        String appLocation = ShellUtilities.executeRaw(FLATPAK_PATH.toString(), "info", "--show-location", alias.getBundleId());

        if(appLocation.isBlank()) {
            return null;
        }

        Path appPath = Paths.get(appLocation.trim());
        // Clean up the uid paths, e.g. "94c5cc42e1001b02248c3472b0cb36c1e6a4a8bd264901"
        Path activePath = appPath.getParent().resolve("active");
        if(activePath.toFile().exists()) {
            appPath = activePath;
        }
        return new AppInfo(alias, appPath, FLATPAK_PATH, version, "run", alias.getBundleId());
    }

    private static Path getFlatpakPath() {
        String flatpakLocation = ShellUtilities.executeRaw("which", "flatpak").trim();
        return !flatpakLocation.isBlank() ? Paths.get(flatpakLocation) : null;
    }

    /**
     * Detect apps whose paths that too near the root of the filesystem
     */
    private boolean isRestricted(AppInfo appInfo) {
        return Arrays.stream(RESTRICTED).anyMatch(s -> appInfo.getAppPath().toString().equals(s));
    }

    /**
     * Match "/snap/" portion of /snap/bin/firefox
     */
    private boolean isSnap(Path exePath) {
        Path snap = Paths.get("/snap");
        return exePath.startsWith(snap);
    }

    /**
     * If snap, translate "/snap/bin/firefox" --> "/snap/firefox/current"
     * Otherwise, return the parent directory of the executable
     */
    private Path getAppPath(Path exePath) throws IOException {
        if(isSnap(exePath)) {
            Path name = exePath.getFileName();
            Path appPath = exePath.getParent().resolveSibling(name).resolve("current");
            if (appPath.toFile().exists()) {
                return appPath;
            } else {
                throw new IOException(String.format("Snap detected at '%s' but can't locate '%s'", exePath, appPath));
            }
        }
        return exePath.getParent();
    }

    private boolean isShellScript(Path exePath) {
        try {
            String contentType = FileUtilities.probeContentType(exePath);
            if (contentType != null && contentType.endsWith("/x-shellscript")) {
                if (UnixUtilities.isFedora()) {
                    // Firefox's script is full of variables and not parsable, fallback to /usr/lib64/$, etc
                    log.info("Found shell script at {}, but we're on Fedora, so we'll look in some known locations instead.", exePath);
                    // FIXME: continue;
                    return false;
                }
                // Debian and Arch like to place a stub script directly in /usr/bin/
                return true;
            }
        } catch(IOException e) {
            log.warn("Unexpected error occurred trying to probe the content type from '{}'", exePath, e);
        }
        return false;
    }

    /**
     * Expands symlinks to real paths, unless it's a snap
     */
    private Path toRealPath(Path exePath) throws IOException {
        Path realExePath = exePath.toRealPath();
        if (realExePath.endsWith("snap")) {
            log.info("Resolving '{}' points to the 'snap' command, we'll use the original value of '{}' instead...", realExePath, exePath);
            return exePath;
        }
        return realExePath;
    }

    /**
     * Attempt to chomp off the <code>.sh</code> file extension and return.
     * This technique was used by older Ubuntu + Firefox versions where 'firefox.sh' would be side-by-side 'firefox'
     */
    private Path legacyDotShRemoval(Path exePath) {
        if(exePath.getFileName().toString().endsWith(".sh")) {
            File exeFile = new File(FilenameUtils.removeExtension(exePath.toString()));
            log.info("Found an '.sh' file: {}, removing file extension and investigating... {}", exePath, exeFile);
            if(exeFile.exists()) {
                return exeFile.toPath();
            } else {
                log.info("'{}' doesn't seem to exist.  That's OK, we'll parse the shell script '{}' instead...", exeFile, exePath);
            }
        }
        return exePath;
    }

    private Path parseScript(Path scriptPath, String lookFor) throws IOException {
        // TODO: log.info("{} bin was expected but script found...  Reading...", appAlias.name());
        try(BufferedReader reader = new BufferedReader(new FileReader(scriptPath.toFile()))) {
            String line;
            while((line = reader.readLine()) != null) {
                if (line.startsWith("exec") && line.contains(lookFor)) {
                    String[] parts = line.split(" ");
                    // Get the app name after "exec"
                    if (parts.length > 1) {
                        log.info("Found a familiar line '{}', using '{}'", line, parts[1]);
                        Path exeCandidate = Paths.get(parts[1]);
                        //String exec = parts[1];
                        // Handle edge-case for esr release
                        if (!exeCandidate.isAbsolute()) {
                            log.warn("We expected '{}' to be a fully qualified path, but it's not.  What do we do?", exeCandidate);
                            throw new UnsupportedEncodingException("FIXME FIXME FIXME");
                            // FIXME Script doesn't contain the full path, go deeper
                            // FIXME exec = Paths.get(dirName, exec).toFile().getCanonicalPath();
                        }
                        if (!exeCandidate.toFile().exists()) {
                            // Make sure it actually exists
                            //if(!(exeFile = new File(exec)).exists()) {
                            log.warn("We found a valid path '{}', but it doesn't exist.  We'll keep looking...", exeCandidate);
                        }
                        return exeCandidate;
                    }
                }
            }
        } catch(IOException e) {
            log.warn("An unexpected error occurred parsing '{}'", scriptPath);
        }
        return null;
    }

    /**
     * Linux has additional flatpak processes which aren't included in this listing
     */
    @Override
    public HashSet<String> getPids(HashSet<AppInfo> appList) {
        // Everything except flatpak
        HashSet<String> processNames = appList.stream()
                .map(appInfo -> appInfo.getExePath().getFileName().toString())
                .filter(exeName -> !exeName.equals("flatpak"))
                .collect(Collectors.toCollection(HashSet::new));

        if(processNames.contains("firefox")) {
            processNames.add("MainThread"); // Workaround Firefox 79 https://github.com/qzind/tray/issues/701
            processNames.add("GeckoMain");  // Workaround Firefox 94 https://bugzilla.mozilla.org/show_bug.cgi?id=1742606
        }

        if(processNames.contains("chromium")) {
            processNames.add("chrome"); // Workaround /snap/chromium/<id>/usr/lib/chromium-browser/chrome
        }

        return getPidsByName(processNames);
    }

    public HashMap<String,AppInfo> getFlatpakPids(HashSet<AppInfo> appList) {
        HashMap<String,AppInfo> pidMap = new HashMap<>();
        // skip if flatpak isn't supported
        if(SystemUtilities.isWindows() || SystemUtilities.isMac() || LinuxAppLocator.FLATPAK_PATH == null) {
            return pidMap;
        }

        // flatpak ps --columns=application,pid
        String running = ShellUtilities.executeRaw(
                LinuxAppLocator.FLATPAK_PATH.toString(),
                "ps", "--columns=application,pid"
        );
        String[] lines = running.split("\\r?\\n");

        for(AppInfo appInfo : appList) {
            for(String line : lines) {
                String[] split = line.split(" ", 2);
                if(split.length < 2) continue;
                String application = split[0];
                String pid = split[1];
                if (application.trim().equals(appInfo.getAlias().getBundleId()) && !pid.isBlank()) {
                    pidMap.put(pid.trim(), appInfo);
                }
            }
        }

        return pidMap;
    }

    @Override
    public HashMap<String, Path> getPidPaths(Set<String> pids) {
        HashMap<String, Path> pathMap = new HashMap<>();
        for(String pid : pids) {
            try {
                // Resolve /proc/<pid>/exe (symlink) to the calling executable
                Path exePath = Paths.get("/proc/", pid, !SystemUtilities.isSolaris() ? "/exe" : "/path/a.out").toRealPath();
                if(isSnap(exePath)) {
                    pathMap.put(pid, fixSnapExePath(exePath));
                } else {
                    pathMap.put(pid, exePath);
                }
            } catch(IOException e) {
                log.warn("Process {} vanished", pid);
            }
        }

        return pathMap;
    }

    /**
     * Handle nuances with exePath reporting
     *   /snap/chromium/3353/usr/lib/chromium-browser/chrome
     *   /snap/firefox/7764/usr/lib/firefox/firefox
     */
    public Path fixSnapExePath(Path exePath) {
        if(exePath.getNameCount() >= 2) {
            String snap = exePath.getName(0).toString();
            String appName = exePath.getName(1).toString();

            return Paths.get("/", snap, "bin", appName);
        }
        return exePath;
    }

    /**
     * Returns a <code>pathSeparator</code> delimited string that can be used to inject
     * values into PATH, etc.
     * <p>
     * Useful for strange Firefox install locations (e.g. Fedora)
     *  Usage: patternPathAppender("firefox", "/usr/lib64");
     * </p>
     */
    private static String patternPathAppender(String exeName, String ... prefixes) {
        // List needed for strict ordering
        StringBuilder builder = new StringBuilder();
        builder.append(System.getenv("PATH"));
        for (String prefix : prefixes) {
            builder
                    .append(File.pathSeparator)
                    .append(prefix.replaceAll("\\$", exeName));
        }

        return builder.toString();
    }
}
