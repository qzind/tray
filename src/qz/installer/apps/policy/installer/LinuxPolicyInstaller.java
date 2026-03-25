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
import java.util.*;

import static qz.installer.apps.policy.PolicyInstaller.normalizeFloats;
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
        try {
            JSONObject jsonPolicy = readJsonFile(state.getLocation());
            putEntries(jsonPolicy, state.getName(), values);
            writeJsonFile(jsonPolicy, state.getLocation(), true);
            state.setSucceeded();
        }
        catch(JSONException | IOException e) {
           state.setFailed(e);
        }
        return state;
    }

    private void putEntries(JSONObject jsonObject, String key, Object... values) throws JSONException, IOException {
        JSONArray jsonArray = jsonObject.optJSONArray(key);
        if(jsonArray == null) {
            log.debug("JSON file found but without array entry for {}, we'll add it", key);
            jsonArray = new JSONArray();
        } else {
            // Explicitly dedupe before adding
            jsonArray = removeFromJsonArray(new HashSet<>(Arrays.asList(values)), jsonArray);
        }

        value:
        for(Object value : values) {
            for(int i = 0; i < jsonArray.length(); i++) {
                if (value.equals(jsonArray.opt(i))) {
                    log.debug("JSON array entry {} '{}' already exists, skipping", key, value);
                    continue value;
                }
            }
            jsonArray.put(value);
        }

        // Insert array into object
        jsonObject.put(key, jsonArray);
    }

    @Override
    public PolicyState removeEntries(PolicyState state, Object ... values) {
        String key = state.getName();
        Path location = state.getLocation();
        Set<Object> removeSet = new HashSet<>(Arrays.asList(values));
        try {
            JSONObject jsonPolicy = readJsonFile(location);
            JSONArray jsonArray = jsonPolicy.optJSONArray(key);
            if(jsonArray != null) {
                jsonArray = removeFromJsonArray(removeSet, jsonArray);
                if(jsonArray.length() > 0) {
                    jsonPolicy.put(key, jsonArray); // explicit put, pointer has changed
                } else {
                    jsonPolicy.remove(key); // clean up empty array
                }
                writeJsonFile(jsonPolicy, location, true);
            } else {
                return state.setSucceeded(String.format("skipping, array value '%s' doesn't exist", key));
            }
        } catch(IOException | JSONException e) {
            return state.setFailed(e);
        }
        return state.setSucceeded();
    }

    @Override
    public PolicyState putMap(PolicyState state, Map<String,Object> map) {
        String key = state.getName();
        Path location = state.getLocation();
        try {
            JSONObject jsonPolicy = readJsonFile(location);
            JSONObject jsonObject = jsonPolicy.optJSONObject(key);
            if(jsonObject == null) {
                log.debug("JSON file found '{}' but without map entry for '{}', we'll add it", location, key);
                jsonObject = new JSONObject();
            }

            for(Map.Entry<String, Object> entry : map.entrySet()) {
                // Handle nested array
                if(entry.getValue() instanceof Object[]) {
                    putEntries(jsonObject, entry.getKey(), (Object[])entry.getValue());
                } else {
                    jsonObject.put(entry.getKey(), entry.getValue());
                }
            }

            // Insert map into object
            jsonPolicy.put(key, jsonObject);
            writeJsonFile(jsonPolicy, location, true);
        } catch(IOException | JSONException e) {
            return state.setFailed(e);
        }
        return state.setSucceeded();
    }

    @Override
    public Object getValue(PolicyState state) {
        Object value = null;
        try {
            value = normalizeFloats(readJsonFile(state.getLocation()).opt(state.getName()));
        } catch(JSONException | IOException ignore) {}
        return state.failIfNull(value);
    }

    @Override
    public Object[] getEntries(PolicyState state) {
        Object[] values = null;
        try {
            JSONArray entries = readJsonFile(state.getLocation()).optJSONArray(state.getName());
            if (entries == null) {
                return new Object[0];
            }
            values = toArray(entries);
        } catch(JSONException | IOException ignore) {}
        return state.failIfNull(values);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String,Object> getMap(PolicyState state) {
        String key = state.getName();
        Path location = state.getLocation();
        Map<String,Object> map = null;
        try {
            JSONObject jsonPolicy = readJsonFile(location);
            map = new HashMap<>();
            JSONObject jsonObject = jsonPolicy.optJSONObject(key);
            if(jsonObject != null) {
                Iterator<String> iterator = jsonObject.keys(); // unchecked: seems to always be <String>
                while(iterator.hasNext()) {
                    String mapKey = iterator.next();
                    Object o = jsonObject.get(mapKey);
                    if(o instanceof JSONArray) {
                        map.put(mapKey, toArray((JSONArray)o));
                    } else {
                        map.put(mapKey, normalizeFloats(o));
                    }
                }
            }
            state.setSucceeded(map.isEmpty() ? String.format("JSON file '%s' is missing map entry for '%s', returning an empty map", location, key) : null);
        } catch(IOException | JSONException e) {
            state.setFailed(e);
        }
        return state.failIfNull(map);
    }

    /**
     * Uses an intermediary ArrayList to fix a bug with JSONArray
     * See also: <a href="https://github.com/jettison-json/jettison/issues/113">#133</a>
     */
    private JSONArray removeFromJsonArray(Set<Object> removeSet, JSONArray jsonArray) throws JSONException {
        ArrayList<Object> tempArray = new ArrayList<>();
        // populate from JSONArray
        for(int i = 0; i < jsonArray.length(); i++) {
            tempArray.add(i, jsonArray.get(i));
        }
        // remove matches
        for(int i = tempArray.size() - 1; i >= 0; i--) {
            Object existing = tempArray.get(i);
            if (removeSet.contains(existing)) {
                tempArray.remove(i);
            }
        }
        // convert back to JSONArray
        jsonArray = new JSONArray();
        for(Object o : tempArray) {
            jsonArray.put(o);
        }
        return jsonArray;
    }

    private static Object[] toArray(JSONArray jsonArray) throws JSONException {
        Object[] values = new Object[jsonArray.length()];
        for(int i = 0; i < jsonArray.length(); i++) {
            values[i] = normalizeFloats(jsonArray.get(i));
        }
        return values;
    }

}
