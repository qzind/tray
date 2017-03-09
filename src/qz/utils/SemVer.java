package qz.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class SemVer {
    public String MAJOR;
    public String MINOR;
    public String PATCH;
    public String PRE;
    public String FULL;
    public String MAJOR_MINOR;

    public SemVer(String version) {
        String[] parts = StringUtils.split(version, ".");
        String[] pre = StringUtils.split(version, "-");
        FULL = version;
        MAJOR = parts.length > 0 ? parts[0] : "";
        MINOR = parts.length > 1 ? parts[1] : "";
        PATCH = parts.length > 2 ? parts[2] : "";
        PRE = pre.length > 1 ? pre[1] : StringUtils.join(ArrayUtils.subarray(parts, 3, parts.length - 1), ".");
        MAJOR_MINOR = MAJOR + "." + MINOR;
    }

    @Override
    public String toString() {
        return FULL;
    }

}
