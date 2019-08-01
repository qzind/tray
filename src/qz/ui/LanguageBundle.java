package qz.ui;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class LanguageBundle{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LanguageBundle.class);
    private final Map<String, String> translationMap;

    public LanguageBundle(String bundleDirectory, Locale locale) throws IOException, JSONException {
        URL fileLocation = ClassLoader.getSystemResource(bundleDirectory + "_" + locale.toString() + ".json");

        JSONObject root = new JSONObject(IOUtils.toString(fileLocation));
        JSONArray translations = root.getJSONArray("translations");

        SortedMap<String, String> tempMap = new TreeMap<>();

        for (int i = 0; i < translations.length(); i++) {
            JSONArray pair = translations.getJSONArray(i);
            tempMap.put(pair.getString(0), pair.getString(1));
        }
        translationMap = Collections.unmodifiableSortedMap(tempMap);
    }

    public String getString(String id) {
        return translationMap.get(id);
    }
}

