package qz.installer;
/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;

import static qz.common.Constants.*;

public class MacInstaller extends Installer {
    protected static final Logger log = LogManager.getLogger(MacInstaller.class);
    private static final String PACKAGE_NAME = getPackageName();
    public static final String LAUNCH_AGENT_PATH = String.format("/Library/LaunchAgents/%s.plist", MacInstaller.PACKAGE_NAME);
    private String destination = "/Applications/" + ABOUT_TITLE + ".app";

    public Installer addAppLauncher() {
        // not needed; registered when "QZ Tray.app" is copied
        return this;
    }

    public Installer addStartupEntry() {
        File dest = new File(LAUNCH_AGENT_PATH);
        HashMap<String, String> fieldMap = new HashMap<>();
        // Dynamic fields
        fieldMap.put("%PACKAGE_NAME%", PACKAGE_NAME);
        fieldMap.put("%COMMAND%", String.format("%s/Contents/MacOS/%s", destination, ABOUT_TITLE));
        fieldMap.put("%PARAM%", "--honorautostart");

        try {
            FileUtilities.configureAssetFile("assets/mac-launchagent.plist.in", dest, fieldMap, MacInstaller.class);
            // Disable service until reboot
            if(SystemUtilities.isMac()) {
                ShellUtilities.execute("/bin/launchctl", "unload", MacInstaller.LAUNCH_AGENT_PATH);
            }
        } catch(IOException e) {
            log.warn("Unable to write startup file: {}", dest, e);
        }

        return this;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public Installer addSystemSettings() {
        // Chrome protocol handler
        String plist = "/Library/Preferences/com.google.Chrome.plist";
        if(ShellUtilities.execute(new String[] { "/usr/bin/defaults", "write", plist }, new String[] {DATA_DIR + "://*" }).isEmpty()) {
            ShellUtilities.execute("/usr/bin/defaults", "write", plist, "URLWhitelist", "-array-add", DATA_DIR +"://*");
        }
        return this;
    }
    public Installer removeSystemSettings() {
        // Remove startup entry
        File dest = new File(LAUNCH_AGENT_PATH);
        dest.delete();
        return this;
    }

    /**
     * Removes legacy (<= 2.0) startup entries
     */
    public Installer removeLegacyStartup() {
        log.info("Removing startup entries for all users matching " + ABOUT_TITLE);
        String script = "tell application \"System Events\" to delete "
                + "every login item where name is \"" + ABOUT_TITLE + "\""
                + " or name is \"" + PROPS_FILE + ".jar\"";

        // Run on background thread in case System Events is hung or slow to respond
        final String finalScript = script;
        new Thread(() -> {
            ShellUtilities.executeAppleScript(finalScript);
        }).run();
        return this;
    }

    public static String getPackageName() {
        String packageName;
        String[] parts = ABOUT_URL.split("\\W");
        if (parts.length >= 2) {
            // Parse io.qz.qz-print from Constants
            packageName = String.format("%s.%s.%s", parts[parts.length - 1], parts[parts.length - 2], PROPS_FILE);
        } else {
            // Fallback on something sane
            packageName = "local." + PROPS_FILE;
        }
        return packageName;
    }

    public void spawn(List<String> args) throws Exception {
        if(SystemUtilities.isAdmin()) {
            // macOS unconventionally uses "$USER" during its install process
            String sudoer = System.getenv("USER");
            if(sudoer == null || sudoer.isEmpty() || sudoer.equals("root")) {
                // Fallback, should only fire via Terminal + sudo
                sudoer = ShellUtilities.executeRaw("logname").trim();
            }
            // Start directly without waitFor(...), avoids deadlocking
            Runtime.getRuntime().exec(new String[] { "su", sudoer, "-c", "\"" + StringUtils.join(args, "\" \"") + "\""});
        } else {
            Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
        }
    }
}
