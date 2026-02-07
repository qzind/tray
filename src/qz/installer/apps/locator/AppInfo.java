package qz.installer.apps.locator;

import com.github.zafarkhaja.semver.Version;

import java.nio.file.Path;

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
    private final Path exePath;

    // TODO: Make these final by refactoring how LinuxAppLocator crawls this information
    private Path path;
    private Version version;

    public AppInfo(Alias alias, Path exePath, String version) {
        this.alias = alias;
        this.exePath = exePath;
        this.path = exePath.getParent();
        setVersion(version);
    }

    public AppInfo(Alias alias, Path path, Path exePath, String version) {
        this.alias = alias;
        this.exePath = exePath;
        this.path = path;
        setVersion(version);
    }

    public AppInfo(Alias alias, Path exePath) {
        this.alias = alias;
        this.exePath = exePath;
        this.path = exePath.getParent();
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

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = AppVersionParser.parse(version);
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof AppInfo && path != null) {
            return path.equals(((AppInfo)o).getPath());
        }
        return false;
    }

    @Override
    public String toString() {
        return alias + " " + path;
    }
}
