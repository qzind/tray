package qz.installer.apps.chromium;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.ChromiumPolicyInstaller;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.locator.AppInfo;
import qz.installer.apps.locator.AppLocator;
import qz.utils.FileUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;

public class LinuxChromiumPolicyInstaller extends ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(LinuxChromiumPolicyInstaller.class);

    private static final String MANAGED_POLICY_PATH_PATTERN = "%s/policies/managed/%s.json";

    public static void main(String ... args) {
        LinuxChromiumPolicyInstaller installer = new LinuxChromiumPolicyInstaller();
        installer.install(Installer.PrivilegeLevel.SYSTEM, "SafeBrowsingEnabled", "true");
        installer.uninstall(Installer.PrivilegeLevel.SYSTEM, "SafeBrowsingEnabled", "true");
    }

    // TODO:
    // 1. Decide whether we loop over all apps or all found apps
    // 2. Decide if we should use a symlink to a JSON file in /opt/qz-tray or if we should
    //    write each file individually
    // 3. JSON should support boolean/int/string and maybe array

    @Override
    public boolean install(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        if(scope != Installer.PrivilegeLevel.SYSTEM) {
            // TODO: Remove this if Linux user-level policies can be confirmed
            log.info("Skipping installation of Chromium policy {}, not supported as PrivilegeLevel {}", policyName, scope);
            return false;
        }
        boolean success = true;
        for(AppInfo appInfo : AppLocator.getInstance().locate(AppAlias.CHROMIUM)) {
             if(!writeJson(calculatePolicyFile(scope, appInfo), policyName, values)) {
                 success = false;
             }
        }
        return success;
    }

    private static File calculatePolicyFile(Installer.PrivilegeLevel scope, AppInfo appInfo) {
        Path prefix;
        switch(scope) {
            case SYSTEM:
                prefix = Paths.get("/");
                switch(appInfo.getName(true)) {
                    case "chromium":
                        // OS-provided are stored in /etc/<name>
                        prefix = prefix.resolve("etc");
                        break;
                    default:
                        // 3rd-party provided are stored in /etc/opt/<name>
                        prefix = prefix.resolve("etc").resolve("opt");
                }
                break;
            case USER:
                // ~/.config
                prefix = Paths.get(System.getProperty("user.home")).resolve(".config");
                break;
            default:
                throw new UnsupportedOperationException(String.format("Scope %s is not yet supported", scope));
        }
        prefix = prefix.resolve(appInfo.getName(true).toLowerCase(Locale.ENGLISH));
        String policyFile = String.format(MANAGED_POLICY_PATH_PATTERN, prefix, Constants.PROPS_FILE);
        return new File(policyFile);
    }

    private static boolean writeJson(File location, String policyName, String ... values) {
        // Chromium policy, e.g. /etc/chromium/policies/managed/qz-tray.json
        ///File location = Paths.get(String.format(pattern, Constants.PROPS_FILE)).toFile();
        log.info("Installing Chromium policy {} {}...", policyName, location);


        // Build JSON array (e.g. { "URLAllowlist": [ "qz://*"] } )
        JSONObject jsonPolicy = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        try {
            // Ensure parent is writable
            FileUtilities.setPermissionsParentally(Files.createDirectories(location.getParentFile().toPath()), false);

            // Populate object
            if (location.exists()) {
                jsonPolicy = new JSONObject(FileUtils.readFileToString(location, StandardCharsets.UTF_8));
                JSONArray found = jsonPolicy.optJSONArray(policyName);
                if(found == null) {
                    log.info("Chromium policy found {} but without entry for {}, we'll add it", location, policyName);
                } else {
                    jsonArray = found;
                }
            }

            value:
            for(String value : values) {
                for(int i = 0; i < jsonArray.length(); i++) {
                    if (jsonArray.optString(i, "").equals(value)) {
                        log.info("Chromium policy {} '{}' already exists at location {}, skipping", policyName, value, location);
                        continue value;
                    }
                }
                jsonArray.put(value);
            }

            // Insert array into object
            jsonPolicy.put(policyName, jsonArray);

            // Write contents, ensuring policy file is world readable
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(location))) {
                writer.write(jsonPolicy.toString());
                if (!location.setReadable(true, false)) {
                    throw new IOException("Unable to set readable: " + location);
                }
            }
        } catch(IOException | JSONException e) {
            log.warn("An error occurred while writing the new policy file {}", location, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        if(scope != Installer.PrivilegeLevel.SYSTEM) {
            log.info("Skipping removal of Chromium policy {}, not supported as PrivilegeLevel {}", policyName, scope);
            return false;
        }
        for(AppInfo appInfo : AppLocator.getInstance().locate(AppAlias.CHROMIUM)) {
            // Chromium policy, e.g. /etc/chromium/policies/managed/qz-tray.json
            File location = calculatePolicyFile(scope, appInfo);
            log.info("Removing Chromium policy {} {}...", policyName, location);

            if(location.exists()) {
                if(location.delete()) {
                    log.info("Deleted Chromium policy {}", location);
                } else {
                    log.warn("Unable to delete Chromium policy {}", location);
                }
            }
        }

        return true;
    }
}

