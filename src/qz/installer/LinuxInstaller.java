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
    public static final String SYSTEM_STARTUP_DIR = "/etc/xdg/autostart";
    public static final String USER_STARTUP_DIR = "%s/.config/autostart";
    public static final String APP_DIR = "/usr/share/applications";
    public static final String SYSTEM_APP_LAUNCHER = APP_DIR;
    public static final String USER_APP_LAUNCHER = "%s/.local/share/applications";
    public static final String UDEV_RULES = "/lib/udev/rules.d/99-udev-override.rules";

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
        String appLauncher = SystemUtilities.isAdmin() ? SYSTEM_APP_LAUNCHER : String.format(USER_APP_LAUNCHER, System.getProperty("user.home"));
        addLauncher(String.format("%s/%s", appLauncher, SHORTCUT_NAME), false);
        return this;
    }

    public Installer addStartupEntry() {
        String startupDir = SystemUtilities.isAdmin() ? SYSTEM_STARTUP_DIR : String.format(USER_STARTUP_DIR, System.getProperty("user.home"));
        addLauncher(String.format("%s/%s", startupDir, SHORTCUT_NAME), true);
        return this;
    }

    private void addLauncher(String location, boolean isStartup) {
        boolean ownerOnly = !SystemUtilities.isAdmin();
        HashMap<String, String> fieldMap = new HashMap<>();
        // Dynamic fields
        fieldMap.put("%DESTINATION%", destination);
        fieldMap.put("%LINUX_ICON%", String.format("%s.svg", PROPS_FILE));
        fieldMap.put("%COMMAND%", String.format("%s/%s", destination, PROPS_FILE));
        fieldMap.put("%PARAM%", isStartup ? "--honorautostart" : "%u");

        File launcher = new File(location);
        try {
            FileUtilities.configureAssetToFile(LinuxInstaller.class, "assets/linux-shortcut.desktop.in", fieldMap, launcher);
            if (!launcher.setReadable(true, ownerOnly) || !launcher.setExecutable(true, ownerOnly)) {
                throw new IOException("Unable to change permissions for launcher");
            }
        } catch(IOException e) {
            log.warn("Unable to write {} file: {}", isStartup ? "startup":"launcher", location, e);
        }
    }

    public Installer removeLegacyStartup() {
        log.info("Removing legacy autostart entries for all users matching {} or {}", ABOUT_TITLE, PROPS_FILE);
        // assume users are in /home
        String[] shortcutNames = {ABOUT_TITLE, PROPS_FILE};
        String[] shortcutPatterns = {USER_STARTUP_DIR, USER_APP_LAUNCHER};
        for(File homeDir : Objects.requireNonNull(new File("/home").listFiles())) {
            if (homeDir.isDirectory()) {
                // FIXME: "removeLegacyStartup()" is a misnomer now that we cleanup app launchers too
                for(String shortcutPattern : shortcutPatterns) {
                    Path folder = Paths.get(String.format(shortcutPattern, homeDir));
                    if (folder.toFile().exists() && folder.toFile().isDirectory()) {
                        for(String shortcutName : shortcutNames) {
                            String name = String.format("%s.desktop", shortcutName);
                            File shortcut = folder.resolve(name).toFile();
                            if (shortcut.exists()) {
                                if(shortcut.delete()) {
                                    log.info("Removing {}", shortcut);
                                }
                            }
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

        // USB permissions
        File udev = new File(UDEV_RULES);
        try {
            FileUtilities.configureAssetToFile(LinuxInstaller.class, "assets/linux-udev.rules.in", new HashMap<>(), udev);
            // udev rules should be -rw-r--r--
            if(!udev.setReadable(true, false)) {
                throw new IOException(String.format("Can't set '%s' readable", udev));
            }
            if(!ShellUtilities.execute("udevadm", "control", "--reload-rules")) {
                throw new IOException("Can't reload udevadm, USB support may not work until a reboot");
            }
        } catch(IOException e) {
            log.warn("Could not install udev rules, error creating '{}', USB support may fail", udev, e);
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
        // USB permissions
        File udev = new File(UDEV_RULES);
        if (udev.exists()) {
            udev.delete();
        }
        return this;
    }

    // Environmental variables for spawning a task using sudo. Order is important.
    static String[] SUDO_EXPORTS = {"USER", "HOME", "UPSTART_SESSION", "DISPLAY", "DBUS_SESSION_BUS_ADDRESS", "XDG_CURRENT_DESKTOP", "GNOME_DESKTOP_SESSION_ID", "WAYLAND_DISPLAY" };

    /**
     * Spawns the process as the underlying regular user account, preserving the environment
     */
    public void spawn(List<String> args) throws Exception {
        ArrayList<String> argsList;
        if(!SystemUtilities.isAdmin()) {
            argsList = new ArrayList<>(args);
            argsList.add(0, "nohup");
        } else {
            // Concat "sudo|su", sudoer, "nohup", args
            argsList = sudoCommand(sudoer, args);
        }

        // Spawn
        log.info("Executing: {}", Arrays.toString(argsList.toArray()));
        super.startProcess(argsList);
    }

    /**
     * Constructs a command to help running as another user using "sudo" or "su"
     * <p>
     * Note: sudo has a bug with "-E" that prevents passing environment variables
     * so they must be passed on the command line
     * </p>
     */
    public static ArrayList<String> sudoCommand(String sudoer, List<String> cmds) {
        ArrayList<String> sudo = new ArrayList<>();
        if(StringUtils.isEmpty(sudoer) || !userExists(sudoer)) {
            throw new UnsupportedOperationException(String.format("Parameter [sudoer: %s] is empty or the provided user was not found", sudoer));
        }
        if(ShellUtilities.execute("which", "sudo") // check if sudo exists
                || ShellUtilities.execute("sudo", "-u", sudoer, "-v")) { // check if user can login
            // Pass directly into "sudo"
            log.info("Guessing that this system prefers \"sudo\" over \"su\".");
            sudo.add("sudo");

            // Get user's environment from dbus, etc
            getUserEnv(sudoer).forEach((key, value) -> {
                if (value != null && !value.isBlank()) {
                    sudo.add(String.format("%s=%s", key, value));
                }
            });

            // Add calling user
            sudo.add("-u");
            sudo.add(sudoer);

            // Let the process outlive its parent
            sudo.add("nohup");
            if(cmds != null && cmds.size() > 0) {
                // Add additional commands
                sudo.addAll(cmds);
            }
        } else {
            // Build and escape for "su"
            log.info("Guessing that this system prefers \"su\" over \"sudo\".");

            // su passes as one large command wrap in single-quotes
            cmds.replaceAll(s -> String.format("'%s'", s));

            sudo.add("su");

            // Add calling user
            sudo.add(sudoer);

            sudo.add("-c");

            // Let the process outlive its parent
            cmds.add(0, "nohup");

            // Get user's environment from dbus, etc
            getUserEnv(sudoer).forEach((key, value) -> {
                if (value != null && !value.isBlank()) {
                    cmds.add(0, String.format("%s='%s'", key, value));
                }
            });


            // Add additional commands
            sudo.add(StringUtils.join(cmds, " "));
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
