package qz.installer.certificate.firefox.locator;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.ShellUtilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class LinuxAppLocator extends AppLocator {
    private static final Logger log = LoggerFactory.getLogger(LinuxAppLocator.class);

    public ArrayList<AppInfo> locate(AppAlias appAlias) {
        ArrayList<AppInfo> appList = new ArrayList<>();

        // Workaround for calling "firefox --version" as sudo
        String[] env = appendPaths("HOME=/tmp");

        // Search for matching executable in all path values
        for(AppAlias.Alias alias : appAlias.aliases) {

            // Add non-standard app search locations (e.g. Fedora)
            for (String dirname : appendPaths(alias.posix, "/usr/lib/$/bin", "/usr/lib64/$/bin")) {
                Path path = Paths.get(dirname, alias.posix);
                if (Files.isRegularFile(path) && Files.isExecutable(path)) {
                    try {
                        File file = path.toFile().getCanonicalFile(); // fix symlinks
                        file = new File(FilenameUtils.removeExtension(file.getPath()));//firefox workaround, changes firefox.sh to firefox

                        AppInfo appInfo = new AppInfo(alias.name, file.toPath());
                        appList.add(appInfo);

                        // Call "--version" on executable to obtain version information
                        Process p = Runtime.getRuntime().exec(new String[] {file.getCanonicalPath(), "--version" }, env);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String version = reader.readLine();
                        reader.close();
                        if (version != null) {
                            if(version.contains(" ")) {
                                String[] split = version.split(" ");
                                appInfo.setVersion(split[split.length - 1]);
                            } else {
                                appInfo.setVersion(version.trim());
                            }
                        }
                        break;
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        return appList;
    }

    @Override
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

    @Override
    public ArrayList<Path> getPidPaths(ArrayList<String> pids) {
        ArrayList<Path> pathList = new ArrayList<>();

        for(String pid : pids) {
            try {
                pathList.add(Paths.get("/proc/", pid, "/exe").toRealPath());
            } catch(IOException e) {
                log.warn("Process %s vanished", pid);
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
