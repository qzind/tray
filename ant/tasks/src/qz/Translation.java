package qz;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;

public class Translation {

    private HashMap<String, String> oldTranslationMap;
    private HashMap<String, String> newTranslationMap;
    private Path path;
    private boolean hasNewElement = false;

    public Translation(Path path) throws IOException, JSONException {
        this.path = path;
        newTranslationMap = new HashMap<>();
        oldTranslationMap = readJSON();
    }

    public void put(String key) {
        String value = oldTranslationMap.get(key);

        //If the translation does not exist, make a new one and set the value to english
        if (value == null) {
            hasNewElement = true;
            newTranslationMap.put(key, key);
        } else {
            newTranslationMap.put(key, value);
        }
    }

    public boolean store() throws IOException, JSONException {
        if (hasNewElement || (newTranslationMap.size() != oldTranslationMap.size())) {
            writeJSON(createJSONObject(newTranslationMap));
            return true;
        }
        return false;
    }

    public Path getPath() {
        return path;
    }

    private HashMap<String,String> readJSON() throws JSONException, IOException {
        String rawJSON = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        JSONObject root = new JSONObject(rawJSON);
        JSONArray translations = root.getJSONArray("translations");
        HashMap<String,String> returnMap = new HashMap<>();

        for(int i = 0; i < translations.length(); i++) {
            JSONArray pair = translations.getJSONArray(i);
            returnMap.put(pair.getString(0), pair.getString(1));
        }

        return returnMap;
    }

    private void writeJSON(JSONObject jsonOut) throws IOException, JSONException {
        Files.write(path, jsonOut.toString(4).getBytes(StandardCharsets.UTF_8));
    }
    private JSONObject createJSONObject(HashMap<String,String> map) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("_lastUpdated", new Date().toString());

        JSONArray pairArray = new JSONArray();
        map.forEach((s1, s2) -> {
            pairArray.put(new JSONArray().put(s1).put(s2));
        });

        return obj.put("translations", pairArray);
    }
}
