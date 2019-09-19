package qz.installer.certificate.firefox.locator;

import com.github.zafarkhaja.semver.Version;
import qz.utils.SystemUtilities;

import java.util.ArrayList;

public abstract class AppLocator {
    String name;
    String path;
    Version version;
    void setName(String name) {
        this.name = name;
    }

    void setPath(String path) {
        this.path = path;
    }

    void setVersion(String version) {
        try {
            // Less than three octets (e.g. "56.0") will fail parsing
            while(version.split("\\.").length < 3) {
                version = version + ".0";
            }
            if (version != null) {
                this.version = Version.valueOf(version);
            }
        } catch(Exception ignore) {}
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Version getVersion() {
        return version;
    }

    abstract boolean isBlacklisted();

    public static ArrayList<AppLocator> locate(AppAlias appAlias) {
        if (SystemUtilities.isWindows()) {
            return WindowsAppLocator.findApp(appAlias);
        } else if (SystemUtilities.isMac()) {
            return MacAppLocator.findApp(appAlias);
        }
        return LinuxAppLocator.findApp(appAlias) ;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof AppLocator && o != null && path != null) {
            if (SystemUtilities.isWindows()) {
                return path.equalsIgnoreCase(((AppLocator)o).getPath());
            } else {
                return path.equals(((AppLocator)o).getPath());
            }
        }
        return false;
    }
}
