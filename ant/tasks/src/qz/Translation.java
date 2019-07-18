package qz;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;

public class Translation {

    private HashMap<String, String> oldTranslationMap;
    private HashMap<String, String> newTranslationMap;
    private Path path;

    public Translation(Path path) throws IOException, JSONException {
        this.path = path;
        newTranslationMap = new HashMap<>();
        oldTranslationMap = readJSON();
    }

    public void setProperty(String key, String value) { newTranslationMap.put(key, value);}

    public boolean containsKey(String key) {return newTranslationMap.containsKey(key);}

    public void store() throws IOException, JSONException {
        if (!newTranslationMap.equals(oldTranslationMap)) {
            writeJSON(createJSONObject(newTranslationMap));
        }
    }

    public Path getPath() {
        return path;
    }

    private HashMap<String,String> readJSON() throws JSONException, IOException {
        URL fileLocation = ClassLoader.getSystemResource(path.toString());

        JSONObject root = new JSONObject(IOUtils.toString(fileLocation));
        JSONArray translations = root.getJSONArray("translations");
        HashMap<String,String> returnMap = new HashMap<>();

        for(int i = 0; i < translations.length(); i++) {
            JSONArray pair = translations.getJSONArray(i);
            returnMap.put(pair.getString(0), pair.getString(1));
        }

        return returnMap;
    }

    private void writeJSON(JSONObject jsonOut) throws IOException, JSONException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(path.toFile()));
        jsonOut.write(bw);
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
