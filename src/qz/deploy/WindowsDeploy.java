/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 *
 */

package qz.deploy;

import qz.common.Constants;
import qz.utils.ShellUtilities;

/**
 * @author Tres Finocchiaro
 */
public class WindowsDeploy extends DeployUtilities {

    // Try using ${windows.icon} first, if it exists
    private static String qzIcon = System.getenv("programfiles").replace(" (x86)", "") + "\\" + Constants.ABOUT_TITLE + "\\windows-icon.ico";
    private static String defaultIcon = System.getenv("windir") + "\\system32\\SHELL32.dll";
    private static boolean useQzIcon = fileExists(qzIcon);

    @Override
    public boolean createStartupShortcut() {
        return ShellUtilities.executeRegScript(
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\\",
                "add",
                getShortcutName(),
                quoteWrap(fixWhitespaces(getJarPath()))
        );
    }

    @Override
    public String getParentDirectory() {
        return fixWhitespaces(super.getParentDirectory());
    }

    @Override
    public boolean createDesktopShortcut() {
        return createShortcut(System.getenv("userprofile") + "\\Desktop\\");
    }

    @Override
    public boolean removeStartupShortcut() {
        return ShellUtilities.executeRegScript(
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\\",
                "delete",
                getShortcutName()
        );
    }

    @Override
    public boolean removeDesktopShortcut() {
        return deleteFile(System.getenv("userprofile") + "\\Desktop\\" + getShortcutName() + ".url");
    }


    @Override
    public boolean hasStartupShortcut() {
        return ShellUtilities.executeRegScript(
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\\",
                "query",
                getShortcutName()
        );
    }

    @Override
    public boolean hasDesktopShortcut() {
        return fileExists(System.getenv("userprofile") + "\\Desktop\\" + getShortcutName() + ".url");
    }

    /**
     * Enables websockets in IE by treating "localhost" the same as an internet zone
     * This setting must be unchecked in order for websockets to communicate back to localhost via:
     * - Internet Options > Security > Local Intranet > Sites > [ ] Include (intranet) sites not listed in other zones
     * Note, the IE settings dialog won't reflect this change until after a browser restart
     *
     * @return true if successful
     */
    public static boolean configureIntranetZone() {
        String path = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Zones\\1";
        String name = "Flags";
        int mask = 16;

        // If the above mask exists, remove it using XOR, thus disabling this default setting
        int data = ShellUtilities.getRegistryDWORD(path, name);
        return !(data != -1 && (data & mask) == mask) || ShellUtilities.setRegistryDWORD(path, name, data ^ mask);
    }

    /**
     * Enables websockets in Microsoft Edge by allowing loopback connections to  "localhost"
     * This setting must be checked in order for websockets to communicate back to localhost via:
     * - about:flags > Developer Settings > Allow localhost loopback (this might put your device at risk)
     * Due to the volatility of this registry path, this registry key path may need to change over time
     *
     * @return true if successful
     */
    public static boolean configureEdgeLoopback() {
        String path = "HKCU\\Software\\Classes\\Local Settings\\Software\\Microsoft\\Windows\\CurrentVersion\\AppContainer\\Storage\\microsoft.microsoftedge_8wekyb3d8bbwe\\MicrosoftEdge\\ExperimentalFeatures";
        String name = "AllowLocalhostLoopback";
        int mask = 1;

        // If the above mask exists, remove it using XOR, thus disabling this default setting
        int data = ShellUtilities.getRegistryDWORD(path, name);
        return !(data != -1 && (data & mask) != mask) || ShellUtilities.setRegistryDWORD(path, name, data | mask);
    }

    /**
     * Returns the string with Windows formatted escaped double quotes, useful for
     * inserting registry keys
     *
     * @return The supplied string wrapped in double quotes
     */
    private String quoteWrap(String text) {
        return "\\\"" + text + "\\\"";
    }

    /**
     * Creates a Windows ".url" shortcut
     *
     * @param folderPath Absolute path to a jar file
     * @return Whether or not the shortcut  was created successfully
     */
    private boolean createShortcut(String folderPath) {
        String workingPath = getParentDirectory(getJarPath());
        String shortcutPath = folderPath + getShortcutName() + ".url";

        // Create the shortcut's parent folder if it does not exist
        return createParentFolder(shortcutPath) && writeArrayToFile(shortcutPath, new String[] {
                "[InternetShortcut]",
                "URL=" + fixURL(getJarPath()),
                workingPath.trim().isEmpty()? "":"WorkingDirectory=" + fixURL(workingPath),
                // SHELL32.DLL:16 is a printer icon on all Windows Operating systems
                "IconIndex=" + (useQzIcon? 0:16),
                "IconFile=" + (useQzIcon? qzIcon:defaultIcon),
                "HotKey=0"
        });
    }

    /**
     * Attempts to correct URL path conversions that occur on old JREs and older
     * Windows versions.  For now, just addresses invalid forward slashes, but
     * there could be other URLs which will need special consideration.
     *
     * @param filePath The absolute file path to convert
     * @return The converted path
     */
    private static String fixURL(String filePath) {
        return "file:///" + filePath.replace("\\", "/");
    }
}
