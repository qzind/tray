package qz.installer.certificate.firefox.locator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class LinuxAppLocator {
    private static final Logger log = LoggerFactory.getLogger(LinuxAppLocator.class);

    public static ArrayList<AppInfo> findApp(AppAlias appAlias) {
        ArrayList<AppInfo> appList = new ArrayList<>();

        // Workaround for calling "firefox --version" as sudo
        String[] env = appendPaths("HOME=/tmp");

        // Search for matching executable in all path values
        for(AppAlias.Alias alias : appAlias.aliases) {

            // Add non-standard app search locations (e.g. Fedora)
            for (String dirname : appendPaths(alias.posix, "/usr/lib/$/bin", "/usr/lib64/$/bin")) {
                File file = new File(dirname, alias.posix);
                if (file.isFile() && file.canExecute()) {
                    try {
                        file = file.getCanonicalFile(); // fix symlinks
                        AppInfo appInfo = new AppInfo(alias.name, file.getCanonicalPath(), file.getParentFile().getCanonicalPath());
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


    /**
     * Returns a PATH value with provided paths appended, replacing "$" with POSIX app name
     * Useful for strange Firefox install locations (e.g. Fedora)
     *
     * Usage: appendPaths("firefox", "/usr/lib64");
     *
     */
    public static String[] appendPaths(String posix, String ... prefixes) {
        String newPath = System.getenv("PATH");
        for (String prefix : prefixes) {
            newPath = newPath + File.pathSeparator + prefix.replaceAll("\\$", posix);
        }
        return newPath.split(File.pathSeparator);
    }
}
