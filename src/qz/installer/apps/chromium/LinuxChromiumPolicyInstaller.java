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
import qz.utils.FileUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LinuxChromiumPolicyInstaller extends ChromiumPolicyInstaller {
    private static final Logger log = LogManager.getLogger(LinuxChromiumPolicyInstaller.class);

    private static final String[] LINUX_POLICY_LOCATIONS = {
            "/etc/chromium/policies/managed/%s.json",
            "/etc/opt/chrome/policies/managed/%s.json",
            "/etc/opt/edge/policies/managed/%s.json"
    };

    @Override
    public boolean install(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        for(String location : LINUX_POLICY_LOCATIONS) {
            log.info("Installing Chromium policy {} {}/{}...", policyName, location, Constants.PROPS_FILE + ".json");

            // Chromium policy, e.g. /etc/chromium/policies/managed/qz-tray.json
            File policy = Paths.get(String.format(location, Constants.PROPS_FILE)).toFile();

            // Build JSON array (e.g. { "URLAllowlist": [ "qz://*"] } )
            JSONObject jsonPolicy = new JSONObject();
            JSONArray jsonArray = new JSONArray();

            try {
                // Ensure parent is writable
                FileUtilities.setPermissionsParentally(Files.createDirectories(Paths.get(location)), false);

                // Populate object
                if (policy.exists()) {
                    jsonPolicy = new JSONObject(FileUtils.readFileToString(policy, StandardCharsets.UTF_8));
                    jsonArray = jsonPolicy.optJSONArray(policyName);
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
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(policy))) {
                    writer.write(jsonPolicy.toString());
                    if (!policy.setReadable(true, false)) {
                        throw new IOException("Unable to set readable: " + policy);
                    }
                }
            } catch(IOException | JSONException e) {
                log.warn("An error occurred while writing the new policy file {}", policy, e);
            }
        }
        return true;
    }

    @Override
    public boolean uninstall(Installer.PrivilegeLevel scope, String policyName, String ... values) {
        for(String location : LINUX_POLICY_LOCATIONS) {
            log.info("Removing Chromium policy {} {}/{}...", policyName, location, Constants.PROPS_FILE + ".json");

            // Chromium policy, e.g. /etc/chromium/policies/managed/qz-tray.json
            File policy = Paths.get(location, Constants.PROPS_FILE + ".json").toFile();
            if(policy.exists()) {
                if(policy.delete()) {
                    log.info("Deleted Chromium policy {}", policy);
                } else {
                    log.warn("Unable to delete Chromium policy {}", policy);
                }
            }
        }

        return true;
    }
}

