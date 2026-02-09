package qz.installer.apps.locator;

import com.github.zafarkhaja.semver.Version;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.apps.AppVersionParser;
import qz.installer.apps.locator.AppAlias.Alias;

/**
 * Container class for installed app information
 */
public class AppInfo {
    private static final Logger log = LogManager.getLogger(AppInfo.class);

    private final AppAlias.Alias alias;
    private final Path appPath;
    private final Path exePath;
    private final Version version;
    private final String[] exeCommand;

    public AppInfo(Alias alias, Path appPath, Path exePath, Version version, String ... exeParams) {
        this.alias = alias;
        this.appPath = appPath;
        this.exePath = exePath;
        this.version = version;
        this.exeCommand = constructCommand(exePath, exeParams);
    }

    public AppInfo(Alias alias, Path appPath, Path exePath, String version, String ... exeParams) {
        this(alias, appPath, exePath, AppVersionParser.parse(version), exeParams);
    }

    public AppInfo(Alias alias, Path exePath, Version version) {
        this(alias, exePath.getParent(), exePath, version);
    }

    public AppInfo(Alias alias, Path appPath, Path exePath, String version) {
        this(alias, appPath, exePath, AppVersionParser.parse(version));
    }

    public Alias getAlias() {
        return alias;
    }

    public String getName(boolean stripVendor) {
        return alias.getName(stripVendor);
    }

    public Path getExePath() {
        return exePath;
    }

    public Path getAppPath() {
        return appPath;
    }

    public Version getVersion() {
        return version;
    }

    public String[] getExeCommand() {
        return exeCommand;
    }

    public boolean exists() {
        return exePath.toFile().exists() && appPath.toFile().exists();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof AppInfo && appPath != null) {
            return appPath.equals(((AppInfo)o).getAppPath());
        }
        return false;
    }

    /**
     * The command needed to start the application including parameters
     * e.g. { "/usr/bin/firefox" }, or  { "/usr/bin/flatpak", "run", "org.mozilla.firefox" }
     */
    public static String[] constructCommand(Path exePath, String ... exeParams) {
        List<String> command = new LinkedList<>();
        command.add(exePath.toString());
        command.addAll(List.of(exeParams));
        return command.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return alias + " " + appPath;
    }
}
