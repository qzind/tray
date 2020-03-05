package qz.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static qz.common.Constants.*;

public class LinuxInstaller extends Installer {
    protected static final Logger log = LoggerFactory.getLogger(LinuxInstaller.class);

    public static final String SHORTCUT_NAME = PROPS_FILE + ".desktop";
    public static final String STARTUP_DIR = "/etc/xdg/autostart/";
    public static final String STARTUP_LAUNCHER = STARTUP_DIR + SHORTCUT_NAME;
    public static final String APP_DIR = "/usr/share/applications/";
    public static final String APP_LAUNCHER = APP_DIR + SHORTCUT_NAME;
    public static final String UDEV_RULES = "/lib/udev/rules.d/99-udev-override.rules";
    public static final String[] CHROME_POLICY_DIRS = {"/etc/chromium/policies/managed", "/etc/opt/chrome/policies/managed" };
    public static final String CHROME_POLICY = "{ \"URLWhitelist\": [\"" + DATA_DIR + "://*\"] }";

    private String destination = "/opt/" + PROPS_FILE;

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public Installer addAppLauncher() {
        addLauncher(APP_LAUNCHER, false);
        return this;
    }

    public Installer addStartupEntry() {
        addLauncher(STARTUP_LAUNCHER, false);
        return this;
    }

    private void addLauncher(String location, boolean isStartup) {
        HashMap<String, String> fieldMap = new HashMap<>();
        // Dynamic fields
        fieldMap.put("%DESTINATION%", destination);
        fieldMap.put("%LINUX_ICON%", String.format("%s.svg", PROPS_FILE));
        fieldMap.put("%COMMAND%", String.format("%s/%s", destination, PROPS_FILE));
        fieldMap.put("%PARAM%", isStartup ? "--honorautostart" : "");

        File launcher = new File(location);
        try {
            FileUtilities.configureAssetFile("assets/linux-shortcut.desktop.in", launcher, fieldMap, LinuxInstaller.class);
            launcher.setReadable(true, false);
            launcher.setExecutable(true, false);
        } catch(IOException e) {
            log.warn("Unable to write {} file: {}", isStartup ? "startup":"launcher", location, e);
        }
    }

    public Installer removeLegacyStartup() {
        log.info("Removing legacy autostart entries for all users matching {} or {}", ABOUT_TITLE, PROPS_FILE);
        // assume users are in /home
        String[] shortcutNames = {ABOUT_TITLE, PROPS_FILE};
        for(File file : new File("/home").listFiles()) {
            if (file.isDirectory()) {
                File userStart = new File(file.getPath() + "/.config/autostart");
                if (userStart.exists() && userStart.isDirectory()) {
                    for (String shortcutName : shortcutNames) {
                        File legacyStartup = new File(userStart.getPath() + File.separator + shortcutName + ".desktop");
                        if(legacyStartup.exists()) {
                            legacyStartup.delete();
                        }
                    }
                }
            }
        }
        return this;
    }

    public Installer addSystemSettings() {
        // Legacy Ubuntu versions only: Patch Unity to show the System Tray
        if(SystemUtilities.isUbuntu()) {
            ShellUtilities.execute("gsettings", "set", "com.canonical.Unity.Panel", "systray", "-whitelist", "\"['all']\"");

            if(ShellUtilities.execute("killall", "-w", "unity", "-panel")) {
                ShellUtilities.execute("nohup", "unity", "-panel");
            }

            if(ShellUtilities.execute("killall", "-w", "unity", "-2d")) {
                ShellUtilities.execute("nohup", "unity", "-2d");
            }
        }

        // Chrome protocol handler
        for (String policyDir : CHROME_POLICY_DIRS) {
            log.info("Installing chrome protocol handler {}/{}...", policyDir, PROPS_FILE + ".json");
            try {
                FileUtilities.setPermissionsParentally(Files.createDirectories(Paths.get(policyDir)), false);
            } catch(IOException e) {
                log.warn("An error occurred creating {}", policyDir);
            }

            Path policy = Paths.get(policyDir, PROPS_FILE + ".json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(policy.toFile()))){
                writer.write(CHROME_POLICY);
                policy.toFile().setReadable(true, false);
            }
            catch(IOException e) {
                log.warn("Unable to write chrome policy: {} ({}:launch will fail)", policy, DATA_DIR);
            }

        }

        // USB permissions
        try {
            File udev = new File(UDEV_RULES);
            if (udev.exists()) {
                udev.delete();
            }
            FileUtilities.configureAssetFile("assets/linux-udev.rules.in", new File(UDEV_RULES), new HashMap<>(), LinuxInstaller.class);
            ShellUtilities.execute("udevadm", "control", "--reload-rules");
        } catch(IOException e) {
            log.warn("Could not install udev rules, usb support may fail {}", UDEV_RULES, e);
        }

        // Cleanup
        log.info("Cleaning up any remaining files...");
        new File(destination + File.separator + "install").delete();
        return this;
    }

    public Installer removeSystemSettings() {
        // Chrome protocol handler
        for (String policyDir : CHROME_POLICY_DIRS) {
            log.info("Removing chrome protocol handler {}/{}...", policyDir, PROPS_FILE + ".json");
            Path policy = Paths.get(policyDir, PROPS_FILE + ".json");
            policy.toFile().delete();
        }

        // USB permissions
        File udev = new File(UDEV_RULES);
        if (udev.exists()) {
            udev.delete();
        }
        return this;
    }

    // Environmental variables for spawning a task using sudo. Order is important.
    static String[] SUDO_EXPORTS = {"USER", "HOME", "UPSTART_SESSION", "DISPLAY", "DBUS_SESSION_BUS_ADDRESS", "XDG_CURRENT_DESKTOP", "GNOME_DESKTOP_SESSION_ID" };

    /**
     * Spawns the process as the underlying regular user account, preserving the environment
     */
    public void spawn(List<String> args) throws Exception {
        args.remove(0); // the first arg is "spawn", remove it
        String whoami = ShellUtilities.executeRaw("logname").trim();
        if(whoami.isEmpty()) {
            whoami = System.getenv("SUDO_USER");
        }

        if(whoami != null && !whoami.trim().isEmpty()) {
            whoami = whoami.trim();
        } else {
            throw new Exception("Unable to get current user, can't spawn instance");
        }

        String[] dbusMatches = { "ibus-daemon.*--panel", "dbus-daemon.*--config-file="};

        ArrayList<String> pids = new ArrayList<>();
        for(String dbusMatch : dbusMatches) {
            pids.addAll(Arrays.asList(ShellUtilities.executeRaw("pgrep", "-f", dbusMatch).split("\\r?\\n")));
        }

        HashMap<String, String> env = new HashMap<>();
        HashMap<String, String> tempEnv = new HashMap<>();
        ArrayList<String> toExport = new ArrayList<>(Arrays.asList(SUDO_EXPORTS));
        for(String pid : pids) {
            try {
                String delim = Pattern.compile("\0").pattern();
                String[] vars = new String(Files.readAllBytes(Paths.get(String.format("/proc/%s/environ", pid)))).split(delim);
                for(String var : vars) {
                    String[] parts = var.split("=", 2);
                    if(parts.length == 2) {
                        String key = parts[0].trim();
                        String val = parts[1].trim();
                        if(toExport.contains(key)) {
                            tempEnv.put(key, val);
                        }
                    }
                }
            } catch(Exception ignore) {}

            // Only add vars for the current user
            if(whoami.trim().equals(tempEnv.get("USER"))) {
                env.putAll(tempEnv);
            }
        }

        if(env.size() == 0) {
            throw new Exception("Unable to get dbus info from /proc, can't spawn instance");
        }

        // Prepare the environment
        String[] envp = new String[env.size() + ShellUtilities.envp.length];
        int i = 0;
        // Keep existing env
        for(String keep : ShellUtilities.envp) {
            envp[i++] = keep;
        }
        for(String key :env.keySet()) {
            envp[i++] = String.format("%s=%s", key, env.get(key));
        }

        // Determine if this environment likes sudo
        String[] sudoCmd = { "sudo", "-E", "-u", whoami, "nohup" };
        String[] suCmd = { "su", whoami, "-c", "nohup" };
        String[] asUser = ShellUtilities.execute("which", "sudo") ? sudoCmd : suCmd;

        // Build and escape our command
        List<String> argsList = new ArrayList<>();
        argsList.addAll(Arrays.asList(asUser));
        String command = "";
        Pattern quote = Pattern.compile("\"");
        for(String arg : args) {
            command += String.format(" %s", arg);
        }
        argsList.add(command.trim());

        // Spawn
        System.out.println(String.join(" ", argsList));
        Runtime.getRuntime().exec(argsList.toArray(new String[argsList.size()]), envp);
    }

}
