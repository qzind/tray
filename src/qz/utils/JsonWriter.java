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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * A minimally intrusive JSON writer
 */
public class JsonWriter {
    protected static final Logger log = LoggerFactory.getLogger(JsonWriter.class);

    public static boolean write(String path, String data, boolean overwrite, boolean delete) throws IOException, JSONException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        if(!f.getParentFile().exists()) {
            log.warn("Warning, the parent folder of {} could not be created, skipping.", path);
            return false;
        }

        if (data == null) {
            log.warn("Data is null, nothing to merge");
            return true;
        }

        JSONObject config = f.exists() ? new JSONObject(FileUtils.readFileToString(f, Charsets.UTF_8)) : new JSONObject();
        JSONObject append = new JSONObject(data);

        if (!delete) {
            merge(config, append, overwrite);
        } else {
            remove(config, append);
        }

        FileUtils.write(f, config.toString(2));

        return true;
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
                //lists only merged if not overriding values
                for(int i = 0; i < ((JSONArray)mergeVal).length(); i++) {
                    ((JSONArray)baseVal).put(((JSONArray)mergeVal).get(i));
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
