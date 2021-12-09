package qz.installer.certificate.firefox.locator;

import com.github.zafarkhaja.semver.Version;

import java.nio.file.Path;
import qz.installer.certificate.firefox.locator.AppAlias.Alias;

/**
 * Container class for installed app information
 */
public class AppInfo {
    private AppAlias.Alias alias;
    private Path path;
    private Path exePath;
    private Version version;

    public AppInfo(Alias alias, Path exePath, String version) {
        this.alias = alias;
        this.path = exePath.getParent();
        this.exePath = exePath;
        this.version = parseVersion(version);
    }

    public AppInfo(Alias alias, Path path, Path exePath, String version) {
        this.alias = alias;
        this.path = path;
        this.exePath = exePath;
        this.version = parseVersion(version);
    }

    public AppInfo(Alias alias, Path exePath) {
        this.alias = alias;
        this.path = exePath.getParent();
        this.exePath = exePath;
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
        this.version = parseVersion(version);
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    private static Version parseVersion(String version) {
        try {
            // Ensure < 3 octets (e.g. "56.0") doesn't failing
            while(version.split("\\.").length < 3) {
                version = version + ".0";
            }
            return Version.valueOf(version);
        } catch(Exception ignore1) {
            // Catch poor formatting (e.g. "97.0a1"), try to use major version only
            if(version.split("\\.").length > 0) {
                try {
                    String[] tryFix = version.split("\\.");
                    return Version.valueOf(tryFix[0] + ".0.0-unknown");
                } catch(Exception ignore2) {}
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof AppInfo && o != null && path != null) {
            return path.equals(((AppInfo)o).getPath());
        }
        return false;
    }

    @Override
    public String toString() {
        return alias + " " + path;
    }
}
