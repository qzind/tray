package qz.installer.apps;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.FileUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class LinuxJsonInstaller {
    protected static final Logger log = LogManager.getLogger(LinuxJsonInstaller.class);

    public static JSONObject readJsonFile(File location) throws IOException, JSONException {
        // Ensure parent is writable
        FileUtilities.setPermissionsParentally(Files.createDirectories(location.getParentFile().toPath()), false);
        return location.exists() ? new JSONObject(FileUtils.readFileToString(location, StandardCharsets.UTF_8)) : new JSONObject();
    }

    public static void writeJsonFile(JSONObject content, File location) throws IOException {
        // Write contents, ensuring policy file is world readable
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(location))) {
            writer.write(content.toString());
            if (!location.setReadable(true, false)) {
                throw new IOException("Unable to set readable: " + location);
            }
        }
    }

    public static boolean writeJsonArray(File location, String key, Object ... values) {
        try {
            JSONObject jsonPolicy = readJsonFile(location);
            JSONArray jsonArray = jsonPolicy.optJSONArray(key);
            if(jsonArray == null) {
                log.info("JSON file found {} but without array entry for {}, we'll add it", location, key);
                jsonArray = new JSONArray();
            }

            value:
            for(Object value : values) {
                for(int i = 0; i < jsonArray.length(); i++) {
                    if (value.equals(jsonArray.opt(i))) {
                        log.info("JSON array entry {} '{}' already exists at location {}, skipping", key, value, location);
                        continue value;
                    }
                }
                jsonArray.put(value);
            }

            // Insert array into object
            jsonPolicy.put(key, jsonArray);
        } catch(IOException | JSONException e) {
            log.warn("An error occurred while writing the JSON file {}", location, e);
            return false;
        }
        return true;
    }

    public static boolean writeJsonValues(File location, String key, Object ... values) {
        try {
            for(Object value : values) {
                JSONObject jsonPolicy = readJsonFile(location);
                if (jsonPolicy.has(key)) {
                    jsonPolicy.remove(key);
                }
                jsonPolicy.put(key, value);
                writeJsonFile(jsonPolicy, location);
                // exit early/antipattern: key:value can only occur once
                return true;
            }
        } catch(IOException | JSONException e) {
            log.warn("An error occurred while writing to the JSON file {}", location, e);
        }
        return false;
    }

    public static boolean removeJsonValue(File location, String key) {
        try {
            JSONObject jsonObject = readJsonFile(location);
            if(jsonObject.has(key)) {
                jsonObject.remove(key);
            }
            writeJsonFile(jsonObject, location);
            return true;
        } catch(IOException | JSONException e) {
            log.warn("An error occurred while modifying the JSON file {}", location, e);
        }
        return false;
    }
}
