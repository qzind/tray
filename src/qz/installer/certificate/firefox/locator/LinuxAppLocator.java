package qz.installer.certificate.firefox.locator;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.SystemUtilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class LinuxAppLocator extends AppLocator {
    private static final Logger log = LogManager.getLogger(LinuxAppLocator.class);

    public ArrayList<AppInfo> locate(AppAlias appAlias) {
        ArrayList<AppInfo> appList = new ArrayList<>();

        // Workaround for calling "firefox --version" as sudo
        String[] env = appendPaths("HOME=/tmp");

        // Search for matching executable in all path values
        aliasLoop:
        for(AppAlias.Alias alias : appAlias.aliases) {

            // Add non-standard app search locations (e.g. Fedora)
            for (String dirname : appendPaths(alias.getPosix(), "/usr/lib/$/bin", "/usr/lib64/$/bin")) {
                Path path = Paths.get(dirname, alias.getPosix());
                if (Files.isRegularFile(path) && Files.isExecutable(path)) {
                    log.info("Found {} {}: {}, investigating...", alias.getVendor(), alias.getName(true), path);
                    try {
                        File file = path.toFile().getCanonicalFile(); // fix symlinks
                        if(file.getPath().endsWith("/snap")) {
                            // Ubuntu 22.04+ ships Firefox as a snap
                            // Snaps are read-only and are symlinks back to /usr/bin/snap
                            // Reset the executable back to /snap/bin/firefox to get proper version information
                            file = path.toFile();
                        }
                        String contentType = Files.probeContentType(file.toPath());
                        if(file.getPath().endsWith(".sh")) {
                            // Legacy Ubuntu likes to use .../firefox/firefox.sh, return .../firefox/firefox instead
                            log.info("Found an '.sh' file: {}, removing file extension: {}", file, file = new File(FilenameUtils.removeExtension(file.getPath())));
                        } else if(contentType != null && contentType.equals("application/x-shellscript")) {
                            // Debian and Arch like to place a stub script directly in /usr/bin/
                            // TODO: Split into a function; possibly recurse on search paths
                            log.info("{} bin was expected but script found...  Reading...", appAlias.name());
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            String line;
                            while((line = reader.readLine()) != null) {
                                if(line.startsWith("exec") && line.contains(alias.getPosix())) {
                                    String[] parts = line.split(" ");
                                    // Get the app name after "exec"
                                    if (parts.length > 1) {
                                        log.info("Found a familiar line '{}', using '{}'", line, parts[1]);
                                        Path p = Paths.get(parts[1]);
                                        String exec = parts[1];
                                        // Handle edge-case for esr release
                                        if(!p.isAbsolute()) {
                                            // Script doesn't contain the full path, go deeper
                                            exec = Paths.get(dirname, exec).toFile().getCanonicalPath();
                                            log.info("Calculated full bin path {}", exec);
                                        }
                                        // Make sure it actually exists
                                        if(!(file = new File(exec)).exists()) {
                                            log.warn("Sorry, we couldn't detect the real path of {}.  Skipping...", appAlias.name());
                                            continue aliasLoop;
                                        }
                                        break;
                                    }
                                }
                            }
                        } else {
                            log.info("Assuming {} {} is installed: {}", alias.getVendor(), alias.getName(true), file);
                        }
                        AppInfo appInfo = new AppInfo(alias, file.toPath());
                        if(file.getPath().startsWith("/snap/")) {
                            // Ubuntu 22.04+ uses snaps, fallback to a sane "path" value
                            String snapPath = file.getPath(); // e.g. /snap/bin/firefox
                            snapPath = snapPath.replaceFirst("/bin/", "/");
                            snapPath += "/current";
                            appInfo.setPath(Paths.get(snapPath));
                        }

                        appList.add(appInfo);

                        // Call "--version" on executable to obtain version information
                        Process p = Runtime.getRuntime().exec(new String[] {file.getPath(), "--version" }, env);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String version = reader.readLine();
                        reader.close();
                        if (version != null) {
                            log.info("We obtained version info: {}, but we'll need to parse it", version);
                            if(version.contains(" ")) {
                                String[] split = version.split(" ");
                                String parsed = split[split.length - 1];
                                String stripped = parsed.replaceAll("[^\\d.]", "");
                                appInfo.setVersion(stripped);
                                if(!parsed.equals(stripped)) {
                                    // Add the meta data back (e.g. "esr")
                                    appInfo.getVersion().setBuildMetadata(parsed.replaceAll("[\\d.]", ""));
                                }
                            } else {
                                appInfo.setVersion(version.trim());
                            }
                        }
                        break;
                    } catch(Exception e) {
                        log.warn("Something went wrong getting app info for {} {}", alias.getVendor(), alias.getName(true), e);
                    }
                }
            }
        }

        return appList;
    }

    @Override
    public ArrayList<Path> getPidPaths(ArrayList<String> pids) {
        ArrayList<Path> pathList = new ArrayList<>();

        for(String pid : pids) {
            try {
                pathList.add(Paths.get("/proc/", pid, !SystemUtilities.isSolaris() ? "/exe" : "/path/a.out").toRealPath());
            } catch(IOException e) {
                log.warn("Process {} vanished", pid);
            }
        }

        return pathList;
    }

    /**
     * Returns a PATH value with provided paths appended, replacing "$" with POSIX app name
     * Useful for strange Firefox install locations (e.g. Fedora)
     *
     * Usage: appendPaths("firefox", "/usr/lib64");
     *
     */
    private static String[] appendPaths(String posix, String ... prefixes) {
        String newPath = System.getenv("PATH");
        for (String prefix : prefixes) {
            newPath = newPath + File.pathSeparator + prefix.replaceAll("\\$", posix);
        }
        return newPath.split(File.pathSeparator);
    }
}
