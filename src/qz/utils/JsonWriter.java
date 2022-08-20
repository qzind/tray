/**
 * @author Brett B.
 *
 * Copyright (C) 2019 QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */


package qz.utils;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * A minimally intrusive JSON writer
 */
public class JsonWriter {
    protected static final Logger log = LogManager.getLogger(JsonWriter.class);

    public static boolean write(String path, String data, boolean overwrite, boolean delete) throws IOException, JSONException {
        File f = new File(path);
        if(!f.getParentFile().exists()) {
            log.warn("Warning, the parent folder of {} does not exist, skipping.", path);
            return false;
        }

        if (data == null) {
            log.warn("Data is null, nothing to merge");
            return true;
        }

        JSONObject config = f.exists() && f.length() > 0 ? new JSONObject(FileUtils.readFileToString(f, Charsets.UTF_8)) : new JSONObject();
        JSONObject append = new JSONObject(data);

        if (!delete) {
            merge(config, append, overwrite);
        } else {
            remove(config, append);
        }

        FileUtils.write(f, config.toString(2), StandardCharsets.UTF_8);

        return true;
    }

    public static boolean contains(File path, String data) {
        try {
            if (!path.exists() || (data == null && data.isEmpty())) {
                return false;
            }

            String jsonData = FileUtils.readFileToString(path, StandardCharsets.UTF_8);
            JSONObject before = new JSONObject(jsonData);
            JSONObject after = new JSONObject(jsonData);
            merge(after, new JSONObject(data), true);
            return before.toString().equals(after.toString());
        } catch(JSONException | IOException ignore) {
            return false;
        }
    }

    /**
     * Appends all keys from {@code merger} to {@code base}
     *
     * @param base      Root JSON object to merge into
     * @param merger    JSON Object of keys to merge
     * @param overwrite If existing keys in {@code base} should be overwritten if defined in {@code merger}
     */
    private static void merge(JSONObject base, JSONObject merger, boolean overwrite) throws JSONException {
        Iterator itr = merger.keys();
        while(itr.hasNext()) {
            String key = (String)itr.next();

            Object baseVal = base.opt(key);
            Object mergeVal = merger.opt(key);

            if (baseVal == null) {
                //add new key
                base.put(key, mergeVal);
            } else if (baseVal instanceof JSONObject && mergeVal instanceof JSONObject) {
                //deep copy sub-keys
                merge((JSONObject)baseVal, (JSONObject)mergeVal, overwrite);
            } else if (overwrite) {
                //force new key val if existing and allowed
                base.put(key, mergeVal);
            } else if (baseVal instanceof JSONArray && mergeVal instanceof JSONArray) {
                JSONArray baseArr = (JSONArray)baseVal;
                JSONArray mergeArr = (JSONArray)mergeVal;

                //lists only merged if not overriding values
                for(int i = 0; i < mergeArr.length(); i++) {
                    //check if value is already in the base array
                    boolean exists = false;
                    for(int j = 0; j < baseArr.length(); j++) {
                        if (baseArr.get(j).equals(mergeArr.get(i))) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        baseArr.put(mergeArr.get(i));
                    }
                }
            }
        }
    }

    /**
     * Removes all keys in {@code deletion} from {@code base}
     *
     * @param base     Root JSON object to delete from
     * @param deletion JSON object of keys to delete
     */
    private static void remove(JSONObject base, JSONObject deletion) {
        Iterator itr = deletion.keys();
        while(itr.hasNext()) {
            String key = (String)itr.next();

            Object baseVal = base.opt(key);
            Object delVal = deletion.opt(key);

            if (baseVal instanceof JSONObject && delVal instanceof JSONObject) {
                //only delete sub-keys
                remove((JSONObject)baseVal, (JSONObject)delVal);
            } else if (baseVal instanceof JSONArray && delVal instanceof JSONArray) {
                //only delete elements in list
                for(int i = 0; i < ((JSONArray)delVal).length(); i++) {
                    ((JSONArray)baseVal).remove(((JSONArray)delVal).opt(i));
                }
            } else if (baseVal != null) {
                //delete entire key
                base.remove(key);
            }
        }
    }

}
