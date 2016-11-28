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

import mslinks.ShellLink;
import qz.utils.ShellUtilities;

import java.io.IOException;
import java.nio.file.NoSuchFileException;

/**
 * @author Tres Finocchiaro
 */
public class WindowsDeploy extends DeployUtilities {
    private static String DELETED_ICON = System.getenv("windir") + "\\system32\\SHELL32.dll";

    @Override
    public boolean createStartupShortcut() {
        return createShortcut(getStartupDirectory());
    }

    @Override
    public boolean createDesktopShortcut() {
        return createShortcut(System.getenv("userprofile") + "\\Desktop\\");
    }

    @Override
    public boolean removeStartupShortcut() {
        try {
            // Dummy shortcut
            ShellLink link = ShellLink.createLink("%SystemRoot%\\system32\\rundll32.exe", getStartupDirectory() + getShortcutName() + ".lnk");
            link.setIconLocation(DELETED_ICON);
            link.getHeader().setIconIndex(131);
            link.saveTo(getStartupDirectory() + getShortcutName() + ".lnk");
            return true;
        }
        catch(Exception ex) {
            log.warn("Error removing startup shortcut", ex);
            return false;
        }
    }

    @Override
    public boolean removeDesktopShortcut() {
        return deleteFile(System.getenv("userprofile") + "\\Desktop\\" + getShortcutName() + ".lnk");
    }


    @Override
    public boolean hasStartupShortcut() {
        try {
            ShellLink link = new ShellLink(getStartupDirectory() + getShortcutName() + ".lnk");
            return link.getIconLocation() == null || !link.getIconLocation().equals(DELETED_ICON);
        }
        catch(NegativeArraySizeException ignore) {
            // Per mslinks issues #4
            return true;
        }
        catch(NoSuchFileException ignore) {
            log.warn("Startup shortcut does not exist");
        }
        catch(Exception ex) {
            log.error("Error detecting startup shortcut", ex);
        }

        return false;
    }

    @Override
    public boolean hasDesktopShortcut() {
        return fileExists(System.getenv("userprofile") + "\\Desktop\\" + getShortcutName() + ".lnk");
    }

    /**
     * Remove flag "Include all local (intranet) sites not listed in other zones". Requires CheckNetIsolation
     * to be effective; Has no effect on domain networks with "Automatically detect intranet network" checked.
     *
     * @return true if successful
     */
    public static boolean configureIntranetZone() {
        String path = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Zones\\1";
        String name = "Flags";
        int value = 16;

        // If the above value is set, remove it using bitwise XOR, thus disabling this setting
        int data = ShellUtilities.getRegistryDWORD(path, name);
        return data != -1 && ((data & value) != value || ShellUtilities.setRegistryDWORD(path, name, data ^ value));
    }

    /**
     * Set legacy Edge flag: about:flags > Developer Settings > Allow localhost loopback
     *
     * @return true if successful
     */
    public static boolean configureEdgeLoopback() {
        String path = "HKCU\\Software\\Classes\\Local Settings\\Software\\Microsoft\\Windows\\CurrentVersion\\AppContainer\\Storage\\microsoft.microsoftedge_8wekyb3d8bbwe\\MicrosoftEdge\\ExperimentalFeatures";
        String name = "AllowLocalhostLoopback";
        int value = 1;

        // If the above value does not exist, add it using bitwise OR, thus enabling this setting
        int data = ShellUtilities.getRegistryDWORD(path, name);
        return data != -1 && ((data & value) == value || ShellUtilities.setRegistryDWORD(path, name, data | value));
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
        }
        catch(IOException ex) {
            log.warn("Error creating desktop shortcut", ex);
            return false;
        }
        return true;
    }

    /**
     * Returns path to executable jar or windows executable
     */
    private String getAppPath() {
        return getJarPath().replaceAll(".jar$", ".exe");
    }

    private static String getStartupDirectory() {
        if (System.getenv("programdata") == null) {
            // XP
            return System.getenv("allusersprofile") + "\\Start Menu\\Programs\\Startup\\";
        }
        return System.getenv("programdata") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup\\";
    }
}
