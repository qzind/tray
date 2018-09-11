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
import java.nio.file.*;

/**
 * @author Tres Finocchiaro
 */
public class WindowsDeploy extends DeployUtilities {

    @Override
    public boolean createDesktopShortcut() {
        return createShortcut(System.getenv("userprofile") + "\\Desktop\\");
    }

    @Override
    public boolean canAutoStart() {
        return Files.exists(Paths.get(getStartupDirectory(), getShortcutName() + ".lnk"));
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
