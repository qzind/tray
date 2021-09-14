/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer;

import com.sun.jna.platform.win32.*;
import mslinks.ShellLink;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.utils.*;
import qz.ws.PrintSocketServer;

import javax.swing.*;

import static qz.common.Constants.*;
import static qz.installer.WindowsSpecialFolders.*;
import static com.sun.jna.platform.win32.WinReg.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;


public class WindowsInstaller extends Installer {
    protected static final Logger log = LoggerFactory.getLogger(WindowsInstaller.class);
    private String destination = getDefaultDestination();
    private String destinationExe = getDefaultDestination() + File.separator + PROPS_FILE + ".exe";

    public void setDestination(String destination) {
        this.destination = destination;
        this.destinationExe = destination + File.separator + PROPS_FILE + ".exe";
    }

    /**
     * Cycles through registry keys removing legacy (<= 2.0) startup entries
     */
    public Installer removeLegacyStartup() {
        log.info("Removing legacy startup entries for all users matching " + ABOUT_TITLE);
        for (String user : Advapi32Util.registryGetKeys(HKEY_USERS)) {
            WindowsUtilities.deleteRegValue(HKEY_USERS, user.trim() + "\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", ABOUT_TITLE);
        }

        try {
            FileUtils.deleteQuietly(new File(STARTUP + File.separator + ABOUT_TITLE + ".lnk"));
        } catch(Win32Exception ignore) {}

        return this;
    }

    public Installer addAppLauncher() {
        try {
            // Delete old 2.0 launcher
            FileUtils.deleteQuietly(new File(COMMON_START_MENU + File.separator + "Programs" + File.separator + ABOUT_TITLE + ".lnk"));
            Path loc = Paths.get(COMMON_START_MENU.toString(), "Programs", ABOUT_TITLE);
            loc.toFile().mkdirs();
            String lnk = loc + File.separator + ABOUT_TITLE + ".lnk";
            String exe = destination + File.separator + PROPS_FILE+ ".exe";
            log.info("Creating launcher \"{}\" -> \"{}\"", lnk, exe);
            ShellLink.createLink(exe, lnk);
        } catch(IOException | Win32Exception e) {
            log.warn("Could not create launcher", e);
        }
        return this;
    }

    public Installer addStartupEntry() {
        try {
            String lnk = WindowsSpecialFolders.COMMON_STARTUP + File.separator + ABOUT_TITLE + ".lnk";
            String exe = destination + File.separator + PROPS_FILE+ ".exe";
            log.info("Creating startup entry \"{}\" -> \"{}\"", lnk, exe);
            ShellLink link = ShellLink.createLink(exe, lnk);
            link.setCMDArgs("--honorautostart"); // honors auto-start preferences
        } catch(IOException | Win32Exception e) {
            log.warn("Could not create startup launcher", e);
        }
        return this;
    }
    public Installer removeSystemSettings() {
        // Cleanup registry
        WindowsUtilities.deleteRegKey(HKEY_LOCAL_MACHINE, "Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\" + ABOUT_TITLE);
        WindowsUtilities.deleteRegKey(HKEY_LOCAL_MACHINE, "Software\\" + ABOUT_TITLE);
        WindowsUtilities.deleteRegKey(HKEY_LOCAL_MACHINE, DATA_DIR);
        // Chrome protocol handler
        WindowsUtilities.deleteRegData(HKEY_LOCAL_MACHINE, "SOFTWARE\\Policies\\Google\\Chrome\\URLWhitelist", String.format("%s://*", DATA_DIR));

        // Cleanup launchers
        for(WindowsSpecialFolders folder : new WindowsSpecialFolders[] { START_MENU, COMMON_START_MENU, DESKTOP, PUBLIC_DESKTOP, COMMON_STARTUP, RECENT }) {
            try {
                new File(folder + File.separator + ABOUT_TITLE + ".lnk").delete();
                // Since 2.1, start menus use subfolder
                if (folder.equals(COMMON_START_MENU) || folder.equals(START_MENU)) {
                    FileUtils.deleteQuietly(new File(folder + File.separator + "Programs" + File.separator + ABOUT_TITLE + ".lnk"));
                    FileUtils.deleteDirectory(new File(folder + File.separator + "Programs" + File.separator + ABOUT_TITLE));
                }
            } catch(IOException | Win32Exception ignore) {}
        }

        // Cleanup firewall rules
        ShellUtilities.execute("netsh.exe", "advfirewall", "delete", "rule", String.format("name=%s", ABOUT_TITLE));
        return this;
    }

    public Installer addSystemSettings() {
        /**
         * TODO: Upgrade JNA!
         *       64-bit registry view is currently invoked by nsis (windows-installer.nsi.in) using SetRegView 64
         *       However, newer version of JNA offer direct WinNT.KEY_WOW64_64KEY registry support, safeguarding
         *       against direct calls to "java -jar qz-tray.jar install|keygen|etc", which will be needed moving forward
         *       for support and troubleshooting.
         */

        // Mime-type support e.g. qz:launch
        WindowsUtilities.addRegValue(HKEY_CLASSES_ROOT, DATA_DIR, "", String.format("URL:%s Protocol", ABOUT_TITLE));
        WindowsUtilities.addRegValue(HKEY_CLASSES_ROOT, DATA_DIR, "URL Protocol", "");
        WindowsUtilities.addRegValue(HKEY_CLASSES_ROOT, String.format("%s\\DefaultIcon", DATA_DIR), "",  String.format("\"%s\",1", destinationExe));
        WindowsUtilities.addRegValue(HKEY_CLASSES_ROOT, String.format("%s\\shell\\open\\command", DATA_DIR), "",  String.format("\"%s\" \"%%1\"", destinationExe));

        /// Uninstall info
        String uninstallKey = String.format("Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\%s", ABOUT_TITLE);
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, String.format("Software\\%s", ABOUT_TITLE), "", destination);
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "DisplayName", String.format("%s %s", ABOUT_TITLE, VERSION));
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "Publisher", ABOUT_COMPANY);
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "UninstallString", destination + File.separator + "uninstall.exe");
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "DisplayIcon", destinationExe);
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "HelpLink", ABOUT_SUPPORT_URL );
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "URLUpdateInfo", ABOUT_DOWNLOAD_URL);
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "URLInfoAbout", ABOUT_SUPPORT_URL);
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "DisplayVersion", VERSION.toString());
        WindowsUtilities.addRegValue(HKEY_LOCAL_MACHINE, uninstallKey, "EstimatedSize", FileUtils.sizeOfDirectoryAsBigInteger(new File(destination)).intValue() / 1024);

        // Chrome protocol handler
        WindowsUtilities.addNumberedRegValue(HKEY_LOCAL_MACHINE, "SOFTWARE\\Policies\\Google\\Chrome\\URLWhitelist", String.format("%s://*", DATA_DIR));

        // Firewall rules
        String ports = StringUtils.join(PrintSocketServer.SECURE_PORTS, ",") + "," + StringUtils.join(PrintSocketServer.INSECURE_PORTS, ",");
        ShellUtilities.execute("netsh.exe", "advfirewall", "delete", "rule", String.format("name=%s", ABOUT_TITLE));
        ShellUtilities.execute("netsh.exe", "advfirewall", "firewall", "add", "rule", String.format("name=%s", ABOUT_TITLE),
                               "dir=in", "action=allow", "profile=any", String.format("localport=%s", ports), "localip=any", "protocol=tcp");
        return this;
    }

    @Override
    public Installer addUserSettings() {
        // Whitelist loopback for IE/Edge
        if(ShellUtilities.execute("CheckNetIsolation.exe", "LoopbackExempt", "-a", "-n=Microsoft.MicrosoftEdge_8wekyb3d8bbwe")) {
            log.warn("Could not whitelist loopback connections for IE, Edge");
        }

        try {
            // Intranet settings; uncheck "include sites not listed in other zones"
            String key = "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings\\Zones\\1";
            String value = "Flags";
            if (Advapi32Util.registryKeyExists(HKEY_CURRENT_USER, key) && Advapi32Util.registryValueExists(HKEY_CURRENT_USER, key, value)) {
                int data = Advapi32Util.registryGetIntValue(HKEY_CURRENT_USER, key, value);
                // remove value using bitwise XOR
                Advapi32Util.registrySetIntValue(HKEY_CURRENT_USER, key, value, data ^ 16);
            }

            // Legacy Edge loopback support
            key = "Software\\Classes\\Local Settings\\Software\\Microsoft\\Windows\\CurrentVersion\\AppContainer\\Storage\\microsoft.microsoftedge_8wekyb3d8bbwe\\MicrosoftEdge\\ExperimentalFeatures";
            value = "AllowLocalhostLoopback";
            if (Advapi32Util.registryKeyExists(HKEY_CURRENT_USER, key) && Advapi32Util.registryValueExists(HKEY_CURRENT_USER, key, value)) {
                int data = Advapi32Util.registryGetIntValue(HKEY_CURRENT_USER, key, value);
                // remove value using bitwise OR
                Advapi32Util.registrySetIntValue(HKEY_CURRENT_USER, key, value, data | 1);
            }
        } catch(Exception e) {
            log.warn("An error occurred configuring the \"Local Intranet Zone\"; connections to \"localhost\" may fail", e);
        }
        return super.addUserSettings();
    }

    public static String getDefaultDestination() {
        String path = System.getenv("ProgramW6432");
        if (path == null || path.trim().isEmpty()) {
            path = System.getenv("ProgramFiles");
        }
        return path + File.separator + ABOUT_TITLE;
    }

    public String getDestination() {
        return destination;
    }

    public void spawn(List<String> args) throws Exception {
        if(SystemUtilities.isAdmin()) {
            log.warn("Spawning as user isn't implemented; starting process with elevation instead");
        }
        ShellUtilities.execute(args.toArray(new String[args.size()]));
    }

    @Override
    public Installer addServiceRegistration(String user) {
        log.warn("Registering system service: {}", PROPS_FILE);
        if(!SystemUtilities.isAdmin()) {
            throw new UnsupportedOperationException("Installing a service requires elevation");
        }

        if(WindowsUtilities.serviceExists(PROPS_FILE)) {
            log.warn("System service is already registered, removing.");
            removeServiceRegistration();
        }

        Path nssm = SystemUtilities.getJarParentPath().resolve("utils/nssm.exe");
        Path qz = SystemUtilities.getJarParentPath().resolve(PROPS_FILE + ".exe");

        // Install the service
        if(ShellUtilities.execute(nssm.toString(), "install", PROPS_FILE,
                                  qz.toString(),
                                  ArgValue.WAIT.getMatches()[0],
                                  ArgValue.STEAL.getMatches()[0],
                                  ArgValue.HEADLESS.getMatches()[0])) {
            ShellUtilities.execute(nssm.toString(), "set", PROPS_FILE, "DisplayName", ABOUT_TITLE);
            ShellUtilities.execute(nssm.toString(), "set", PROPS_FILE, "Description", ABOUT_DESCRIPTION);
            ShellUtilities.execute(nssm.toString(), "set", PROPS_FILE, "DependOnService", "Spooler");
            log.info("Successfully registered system service: {}", PROPS_FILE);
            if(user != null && !user.trim().isEmpty()) {
                log.info("Setting service to run as {}", user);
                if(!ShellUtilities.execute(nssm.toString(), "set", "ObjectName", user)) {
                    log.warn("Could not set service to run as {}, please configure manually.", user);
                }
            }
            // Kill all running instances
            TaskKiller.killAll();
            // Instruct autostart to be ignored
            FileUtilities.disableGlobalAutoStart();
            log.info("Starting system service: {}", PROPS_FILE);
            if(WindowsUtilities.startService(PROPS_FILE)) {
                log.info("System system service started successfully.", PROPS_FILE);
                return this;
            }
        }
        throw new UnsupportedOperationException("An error occurred installing the service");
    }

    @Override
    public Installer removeServiceRegistration() {
        log.info("Removing system service: {}", PROPS_FILE);
        if(!SystemUtilities.isAdmin()) {
            throw new UnsupportedOperationException("Removing a service requires elevation");
        }

        if(WindowsUtilities.serviceExists(PROPS_FILE)) {
            WindowsUtilities.stopService(PROPS_FILE);
            Path nssm = SystemUtilities.getJarParentPath().resolve("utils/nssm.exe");
            if(ShellUtilities.execute(nssm.toString(), "remove", PROPS_FILE, "confirm")) {
                // Old tutorials used "QZ Tray" as the service name
                ShellUtilities.execute(nssm.toString(), "remove", ABOUT_TITLE, "confirm");
                // Restore default autostart settings by deleting the preference file
                FileUtils.deleteQuietly(FileUtilities.SHARED_DIR.resolve(AUTOSTART_FILE).toFile());
                log.info("System service successfully removed: {}", PROPS_FILE);
            } else {
                log.error("An error occurred removing system service: {}", PROPS_FILE);
            }
        } else {
            log.info("System service was not found, skipping.");
        }

        return this;
    }
}
