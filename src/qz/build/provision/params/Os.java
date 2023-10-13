package qz.build.provision.params;

import org.apache.commons.lang3.StringUtils;
import qz.utils.SystemUtilities;

import java.util.*;

/**
 * Basic OS parser
 */
public enum Os {
    WINDOWS,
    MAC,
    LINUX,
    SOLARIS, // unsupported
    ALL, // special handling
    UNKNOWN;

    public boolean matches(HashSet<Os> osList) {
        return this == ALL || osList.contains(ALL) || (this != UNKNOWN && osList.contains(this));
    }

    public static boolean matchesHost(HashSet<Os> osList) {
        for(Os os : osList) {
            if(os == SystemUtilities.getOs() || os == ALL) {
                return true;
            }
        }
        return false;
    }

    public static Os parseStrict(String input) throws UnsupportedOperationException {
        return EnumParser.parseStrict(Os.class, input, ALL, UNKNOWN);
    }

    public static Os bestMatch(String input) {
        if(input != null) {
            String name = input.toLowerCase(Locale.ENGLISH);
            if (name.contains("win")) {
                return Os.WINDOWS;
            } else if (name.contains("mac")) {
                return Os.MAC;
            } else if (name.contains("linux")) {
                return Os.LINUX;
            } else if (name.contains("sunos")) {
                return Os.SOLARIS;
            }
        }
        return Os.UNKNOWN;
    }

    public static HashSet<Os> parse(String input) {
        return EnumParser.parseSet(Os.class, Os.ALL, input);
    }

    public static String serialize(HashSet<Os> osList) {
        if(osList.contains(ALL)) {
            return "*";
        }
        return StringUtils.join(osList, "|");
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }
}