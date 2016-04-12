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

import org.mslinks.ShellLink;
import qz.common.Constants;
import qz.utils.ShellUtilities;

import java.io.IOException;

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
                quoteWrap(getAppPath())
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
        return deleteFile(System.getenv("userprofile") + "\\Desktop\\" + getShortcutName() + ".lnk");
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
        return fileExists(System.getenv("userprofile") + "\\Desktop\\" + getShortcutName() + ".lnk");
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
     * Creates a Windows ".lnk" shortcut
     *
     * @param folderPath Absolute path to a jar file
     * @return Whether or not the shortcut  was created successfully
     */
    private boolean createShortcut(String folderPath) {
        try {
            ShellLink.createLink(getAppPath(), folderPath + getShortcutName() + ".lnk");
        } catch (IOException ex) {
            log.warn("Error creating desktop shortcut", ex);
            return false;
        }
        return true;
    }

    /**
     * Returns path to executable jar or windows executable
     */
    private String getAppPath() {
        return fixWhitespaces(getJarPath()).replaceAll(".jar$", ".exe");
    }
}
