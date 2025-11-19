package qz.installer;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.common.Constants;
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
import java.nio.file.Paths;

import static qz.installer.Installer.*;
import static com.sun.jna.platform.win32.WinReg.*;

public class ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(ChromiumPolicyInstaller.class);

    private static final String[] WINDOWS_POLICY_LOCATIONS = {
            "SOFTWARE\\Policies\\Google\\Chrome\\",
    };

    private static final String[] MACOS_POLICY_LOCATIONS = {
            "/Library/Preferences/com.google.Chrome.plist",
    };

    private static final String[] LINUX_POLICY_LOCATIONS = {
            "/etc/chromium/policies/managed",
            "/etc/opt/chrome/policies/managed",
    };

    private static boolean installWindows(PrivilegeLevel scope, String policyName, String ... values) {
        HKEY root = scope == PrivilegeLevel.SYSTEM ? HKEY_LOCAL_MACHINE : HKEY_CURRENT_USER;
        for(String location : WINDOWS_POLICY_LOCATIONS) {
            log.info("Installing Chromium policy {}\\{}...", location, policyName);
            for(String value : values) {
                WindowsUtilities.addNumberedRegValue(root, location + "\\" + policyName, value);
            }
        }
        return true;
    }

    private static boolean installMac(PrivilegeLevel scope, String policyName, String ... values) {
        for(String location : MACOS_POLICY_LOCATIONS) {
            log.info("Installing Chromium policy {} {}...", policyName, location);
            for(String value : values) {
                if (ShellUtilities.execute(new String[] {"/usr/bin/defaults", "write", location}, new String[] {value}).isEmpty()) {
                    ShellUtilities.execute("/usr/bin/defaults", "write", location, policyName, "-array-add", value);
                }
            }
        }
        return true;
    }

    private static boolean installLinux(PrivilegeLevel scope, String policyName, String ... values) throws JSONException, IOException {
        for(String location : LINUX_POLICY_LOCATIONS) {
            log.info("Installing Chromium policy {} {}/{}...", policyName, location, Constants.PROPS_FILE + ".json");

            // Chromium policy, e.g. /etc/chromium/policies/managed/qz-tray.json
            File policy = Paths.get(location, Constants.PROPS_FILE + ".json").toFile();

            // Build JSON array (e.g. { "URLAllowlist": [ "qz://*"] } )
            JSONObject jsonPolicy = null;
            JSONArray jsonArray = null;

            // Ensure parent is writable
            FileUtilities.setPermissionsParentally(Files.createDirectories(Paths.get(location)), false);

            // Populate object
            if(policy.exists()) {
                try {
                    jsonPolicy = new JSONObject(FileUtils.readFileToString(policy, StandardCharsets.UTF_8));
                    jsonArray = jsonPolicy.optJSONArray(policyName);
                } catch(JSONException e) {
                    log.warn("JSONException occurred reading " + jsonPolicy + ", creating a new file.");
                }
            }
            if(jsonPolicy == null) {
                jsonPolicy = new JSONObject();
            }

            // Populate (or append) array
            if(jsonArray == null) {
                jsonArray = new JSONArray();
            }
            for(String value : values) {
                jsonArray.put(value);
            }

            // Insert array into object
            jsonPolicy.put(policyName, jsonArray);

            // Write contents, ensuring policy file is world readable
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(policy))){
                writer.write(jsonPolicy.toString());
                if(!policy.setReadable(true, false)) {
                    throw new IOException("Unable to set readable: " + policy);
                }
            }
        }
        return true;
    }

    public static boolean install(PrivilegeLevel scope, String policyName, String ... values) throws IOException, JSONException {
        switch(SystemUtilities.getOs()) {
            case WINDOWS:
                return installWindows(scope, policyName, values);
            case MAC:
                return installMac(scope, policyName, values);
            case LINUX:
            default:
                return installLinux(scope, policyName, values);
        }
    }

    // TODO: Add uninstaller
}
