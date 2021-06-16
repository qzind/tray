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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    protected static final Logger log = LoggerFactory.getLogger(MacInstaller.class);
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
        if(ShellUtilities.execute(new String[] { "/usr/bin/defaults", "write", plist }, new String[] { "qz://*" }).isEmpty()) {
            ShellUtilities.execute("/usr/bin/defaults", "write", plist, "URLWhitelist", "-array-add", "qz://*");
        }
        return this;
    }
    public Installer removeSystemSettings() {
        // Remove startup entry
        File dest = new File(LAUNCH_AGENT_PATH);
        dest.delete();
        return this;
    }

    @Override
    public Installer removeLegacyFiles() {
        // Files/folders moved to Contents/ since #770
        String dirs[] = {
                "demo",
                "libs"
        };
        String[] files = {
                PROPS_FILE + ".jar",
                "uninstall",
                "LICENSE.TXT",
                "Contents/Resources/apple-icon.icns"
        };
        String[] move = {
                PROPS_FILE + ".properties"
        };
        for (String dir : dirs) {
            try {
                FileUtils.deleteDirectory(new File(getInstance().getDestination() + File.separator + dir));
            } catch(IOException ignore) {}
        }
        for (String file : files) {
            new File(getInstance().getDestination() + File.separator + file).delete();
        }
        // Move from "/" to "/Contents"
        for (String file : move) {
            Path dest, source = null;
            try {
                source = Paths.get(getInstance().getDestination(), file);
                dest = Paths.get(getInstance().getDestination(), "Contents", file);
                if(source.toFile().exists()) {
                    Files.move(source, dest);
                }
            } catch(IOException ignore) {
            } finally {
                if(source != null) {
                    source.toFile().delete();
                }
            }
        }

        return super.removeLegacyFiles();
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
            String whoami = System.getenv("USER");
            if(whoami == null || whoami.isEmpty() || whoami.equals("root")) {
                // Fallback, should only fire via Terminal + sudo
                whoami = ShellUtilities.executeRaw("logname").trim();
            }
            // Start directly without waitFor(...), avoids deadlocking
            Runtime.getRuntime().exec(new String[] { "su", whoami, "-c", "\"" + StringUtils.join(args, "\" \"") + "\""});
        } else {
            Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
        }
    }
}
