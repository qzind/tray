package qz.installer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.UnixUtilities;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

import static qz.common.Constants.*;

public class LinuxInstaller extends Installer {
    protected static final Logger log = LogManager.getLogger(LinuxInstaller.class);

    public static final String SHORTCUT_NAME = PROPS_FILE + ".desktop";
    public static final String STARTUP_DIR = "/etc/xdg/autostart/";
    public static final String STARTUP_LAUNCHER = STARTUP_DIR + SHORTCUT_NAME;
    public static final String APP_DIR = "/usr/share/applications/";
    public static final String APP_LAUNCHER = APP_DIR + SHORTCUT_NAME;
    public static final String UDEV_RULES = "/lib/udev/rules.d/99-udev-override.rules";
    public static final String[] CHROME_POLICY_DIRS = {"/etc/chromium/policies/managed", "/etc/opt/chrome/policies/managed" };
    public static final String CHROME_POLICY = "{ \"URLAllowlist\": [\"" + DATA_DIR + "://*\"] }";

    private String destination = "/opt/" + PROPS_FILE;
    private String sudoer;

    public LinuxInstaller() {
        super();
        sudoer = getSudoer();
    }

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
        addLauncher(STARTUP_LAUNCHER, true);
        return this;
    }

    private void addLauncher(String location, boolean isStartup) {
        HashMap<String, String> fieldMap = new HashMap<>();
        // Dynamic fields
        fieldMap.put("%DESTINATION%", destination);
        fieldMap.put("%LINUX_ICON%", String.format("%s.svg", PROPS_FILE));
        fieldMap.put("%COMMAND%", String.format("%s/%s", destination, PROPS_FILE));
        fieldMap.put("%PARAM%", isStartup ? "--honorautostart" : "%u");

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
        if(UnixUtilities.isUbuntu()) {
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
            // udev rules should be -rw-r--r--
            udev.setReadable(true, false);
            ShellUtilities.execute("udevadm", "control", "--reload-rules");
        } catch(IOException e) {
            log.warn("Could not install udev rules, usb support may fail {}", UDEV_RULES, e);
        }

        // Cleanup incorrectly placed files
        File badFirefoxJs = new File("/usr/bin/defaults/pref/" + PROPS_FILE + ".js");
        File badFirefoxCfg = new File("/usr/bin/" + PROPS_FILE + ".cfg");

        if(badFirefoxCfg.exists()) {
            log.info("Removing incorrectly placed Firefox configuration {}, {}...", badFirefoxJs, badFirefoxCfg);
            badFirefoxCfg.delete();
            new File("/usr/bin/defaults").delete();
        }

        // Cleanup incorrectly placed files
        File badFirefoxPolicy = new File("/usr/bin/distribution/policies.json");
        if(badFirefoxPolicy.exists()) {
            log.info("Removing incorrectly placed Firefox policy {}", badFirefoxPolicy);
            badFirefoxPolicy.delete();
            // Delete the distribution folder too, as long as it's empty
            File badPolicyFolder = badFirefoxPolicy.getParentFile();
            if(badPolicyFolder.isDirectory() && badPolicyFolder.listFiles().length == 0) {
                badPolicyFolder.delete();
            }
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
        if(!SystemUtilities.isAdmin()) {
            // Not admin, just run as the existing user
            ShellUtilities.execute(args.toArray(new String[args.size()]));
            return;
        }

        // Get user's environment from dbus, etc
        HashMap<String, String> env = getUserEnv(sudoer);
        if(env.size() == 0) {
            throw new Exception("Unable to get dbus info; can't spawn instance");
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

        // Concat "sudo|su", sudoer, "nohup", args
        ArrayList<String> argsList = sudoCommand(sudoer, true, args);

        // Spawn
        log.info("Executing: {}", Arrays.toString(argsList.toArray()));
        Runtime.getRuntime().exec(argsList.toArray(new String[argsList.size()]), envp);
    }

    /**
     * Constructs a command to help running as another user using "sudo" or "su"
     */
    public static ArrayList<String> sudoCommand(String sudoer, boolean async, List<String> cmds) {
        ArrayList<String> sudo = new ArrayList<>();
        if(StringUtils.isEmpty(sudoer) || !userExists(sudoer)) {
            throw new UnsupportedOperationException(String.format("Parameter [sudoer: %s] is empty or the provided user was not found", sudoer));
        }
        if(ShellUtilities.execute("which", "sudo") // check if sudo exists
                || ShellUtilities.execute("sudo", "-u", sudoer, "-v")) { // check if user can login
            // Pass directly into "sudo"
            log.info("Guessing that this system prefers \"sudo\" over \"su\".");
            sudo.add("sudo");

            // Add calling user
            sudo.add("-E"); // preserve environment
            sudo.add("-u");
            sudo.add(sudoer);

            // Add "background" task support
            if(async) {
                sudo.add("nohup");
            }
            if(cmds != null && cmds.size() > 0) {
                // Add additional commands
                sudo.addAll(cmds);
            }
        } else {
            // Build and escape for "su"
            log.info("Guessing that this system prefers \"su\" over \"sudo\".");
            sudo.add("su");

            // Add calling user
            sudo.add(sudoer);

            sudo.add("-c");

            // Add "background" task support
            if(async) {
                sudo.add("nohup");
            }
            if(cmds != null && cmds.size() > 0) {
                // Add additional commands
                sudo.addAll(Arrays.asList(StringUtils.join(cmds, "\" \"") + "\""));
            }
        }
        return sudo;
    }

    /**
     * Gets the most likely non-root user account that the installer is running from
     */
    private static String getSudoer() {
        String sudoer = ShellUtilities.executeRaw("logname").trim();
        if(sudoer.isEmpty() || SystemUtilities.isSolaris()) {
            sudoer = System.getenv("SUDO_USER");
        }
        return sudoer;
    }

    /**
     * Uses two common POSIX techniques for testing if the provided user account exists
     */
    private static boolean userExists(String user) {
        return ShellUtilities.execute("id", "-u", user) ||
                ShellUtilities.execute("getent", "passwd", user);
    }

    /**
     * Attempts to extract user environment variables from the dbus process to
     * allow starting a graphical application as the current user.
     *
     * If this fails, items such as the user's desktop theme may not be known to Java
     * at runtime resulting in the Swing L&F instead of the Gtk L&F.
     */
    private static HashMap<String, String> getUserEnv(String matchingUser) {
        if(!SystemUtilities.isAdmin()) {
           throw new UnsupportedOperationException("Administrative access is required");
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
            if(pid.isEmpty()) {
                continue;
            }
            try {
                String[] vars;
                if(SystemUtilities.isSolaris()) {
                    // Use pargs -e $$ to get environment
                    log.info("Reading environment info from [pargs, -e, {}]", pid);
                    String pargs = ShellUtilities.executeRaw("pargs", "-e", pid);
                    vars = pargs.split("\\r?\\n");
                    String delim = "]: ";
                    for(int i = 0; i < vars.length; i++) {
                        if(vars[i].contains(delim)) {
                            vars[i] = vars[i].substring(vars[i].indexOf(delim) + delim.length()).trim();
                        }
                    }
                } else {
                    // Assume /proc/$$/environ
                    String environ = String.format("/proc/%s/environ", pid);
                    String delim = Pattern.compile("\0").pattern();
                    log.info("Reading environment info from {}", environ);
                    vars = new String(Files.readAllBytes(Paths.get(environ))).split(delim);
                }
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
            } catch(Exception e) {
                log.warn("An unexpected error occurred obtaining dbus info", e);
            }

            // Only add vars for the current user
            if(matchingUser.trim().equals(tempEnv.get("USER"))) {
                env.putAll(tempEnv);
            } else {
                log.debug("Expected USER={} but got USER={}, skipping results for {}", matchingUser, tempEnv.get("USER"), pid);
            }

            // Use gtk theme
            if(env.containsKey("XDG_CURRENT_DESKTOP") && !env.containsKey("GNOME_DESKTOP_SESSION_ID")) {
                if(env.get("XDG_CURRENT_DESKTOP").toLowerCase(Locale.ENGLISH).contains("gnome")) {
                    env.put("GNOME_DESKTOP_SESSION_ID", "this-is-deprecated");
                }
            }
        }
        return env;
    }

}
