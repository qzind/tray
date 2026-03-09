package qz.installer.apps.policy.installer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.PolicyState;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static qz.utils.FileUtilities.*;

public class LinuxPolicyInstaller implements PolicyInstaller.PrimitivePolicyInstaller {
    protected static final Logger log = LogManager.getLogger(LinuxPolicyInstaller.class);

    @Override
    public PolicyState putValue(PolicyState state, Object value) {
        String key = state.getName();
        Path location = state.getLocation();
        try {
            JSONObject jsonPolicy = readJsonFile(location);
            if (jsonPolicy.has(key)) {
                jsonPolicy.remove(key);
            }
            jsonPolicy.put(key, value);
            writeJsonFile(jsonPolicy, location, true);
        } catch(IOException | JSONException e) {
            return state.setFailed(e);
        }
        return state.setSucceeded();
    }

    @Override
    public PolicyState removeValue(PolicyState state) {
        String key = state.getName();
        Path location = state.getLocation();
        try {
            JSONObject jsonObject = readJsonFile(location);
            if(jsonObject.has(key)) {
                jsonObject.remove(key);
            }
            writeJsonFile(jsonObject, location, true);
        } catch(IOException | JSONException e) {
            return state.setFailed(e);
        }
        return state.setSucceeded();
    }

    @Override
    public PolicyState putEntries(PolicyState state, Object... values) {
        String key = state.getName();
        Path location = state.getLocation();
        try {
            JSONObject jsonPolicy = readJsonFile(location);
            JSONArray jsonArray = jsonPolicy.optJSONArray(key);
            if(jsonArray == null) {
                log.debug("JSON file found {} but without array entry for {}, we'll add it", location, key);
                jsonArray = new JSONArray();
            }

            value:
            for(Object value : values) {
                for(int i = 0; i < jsonArray.length(); i++) {
                    if (value.equals(jsonArray.opt(i))) {
                        log.debug("JSON array entry {} '{}' already exists at location {}, skipping", key, value, location);
                        continue value;
                    }
                }
                jsonArray.put(value);
            }

            // Insert array into object
            jsonPolicy.put(key, jsonArray);
            writeJsonFile(jsonPolicy, location, true);
        } catch(IOException | JSONException e) {
            return state.setFailed(e);
        }
        return state.setSucceeded();
    }

    @Override
    public PolicyState removeEntries(PolicyState state, Object ... values) {
        String key = state.getName();
        Path location = state.getLocation();
        Set<Object> remove = new HashSet<>(Arrays.asList(values));
        try {
            JSONObject jsonPolicy = readJsonFile(location);
            JSONArray jsonArray = jsonPolicy.optJSONArray(key);
            if(jsonArray != null) {
                for(int i = jsonArray.length() - 1; i >= 0; i--) {
                    Object existing = jsonArray.get(i);
                    if (remove.contains(existing)) {
                        jsonArray.remove(i);
                    }
                }
                writeJsonFile(jsonPolicy, location, true);
            }
        } catch(IOException | JSONException e) {
            return state.setFailed(e);
        }
        return state.setSucceeded();
    }
}
