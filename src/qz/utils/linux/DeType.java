package qz.utils.linux;

import qz.utils.ShellUtilities;

/**
 * Helper class for identifying a Desktop environment in Linux or a Linux-like system.
 *
 * <p>
 * <b>Note:</b>
 * <ul>
 *  <li>
 *      Some tools such as <code>gsettings</code> will often exist in other desktops so we prioritize
 *      searching non-Gnome tools first.
 *  </li>
 *  <li>
 *      Some tools such as <code>kreadconfig5</code> will coexist with <code>kreadconfig6</code>
 *      so we prioritize searching for these tools in descending order
 * </li>
 * </ul>
 * </p>
 */
public enum DeType {
    KDE("kreadconfig6", "kreadconfig5"), // keep before gsettings, keep descending
    GNOME("gsettings"),
    UNKNOWN;

    private static String binaryFound = "false";
    private final String[] binaries;

    DeType(String ... binaries) {
        this.binaries = binaries;
    }

    /**
     * Look for a CLI tool that would "likely" indicate our Desktop Environment
     */
    boolean isLikely() {
        for(String binary : binaries) {
            if(ShellUtilities.execute("which", binary)) {
                binaryFound = binary;
                return true;
            }
        }
        return false;
    }

    public static String getBinaryFound() {
        return binaryFound;
    }
}
