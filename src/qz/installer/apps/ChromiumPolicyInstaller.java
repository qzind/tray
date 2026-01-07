package qz.installer.apps;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.common.Constants;
import qz.installer.apps.chromium.LinuxChromiumPolicyInstaller;
import qz.installer.apps.chromium.MacChromiumPolicyInstaller;
import qz.installer.apps.chromium.WindowsChromiumPolicyInstaller;
import qz.utils.FileUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import static qz.common.Constants.PROPS_FILE;
import static qz.installer.Installer.*;
import static com.sun.jna.platform.win32.WinReg.*;

public abstract class ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(ChromiumPolicyInstaller.class);

    static ChromiumPolicyInstaller INSTANCE = getInstance();

    public abstract boolean install(PrivilegeLevel scope, String policyName, String ... values);
    public abstract boolean uninstall(PrivilegeLevel scope, String policyName, String ... values);

    public static void main(String ... args) throws JSONException, IOException {
        /*
        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "pp://");
        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "pp://");
        */

        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "pp://");
        getInstance().install(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        //getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        //getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "qz://");
        //getInstance().uninstall(PrivilegeLevel.USER, "URLAllowlist", "pp://");
    }

    public static ChromiumPolicyInstaller getInstance() {
       if(INSTANCE == null) {
           switch(SystemUtilities.getOs()) {
               case WINDOWS:
                   INSTANCE = new WindowsChromiumPolicyInstaller();
                   break;
               case MAC:
                   INSTANCE = new MacChromiumPolicyInstaller();
                   break;
               case LINUX:
               default:
                   INSTANCE = new LinuxChromiumPolicyInstaller();
           }
       }
       return INSTANCE;
    }

}
