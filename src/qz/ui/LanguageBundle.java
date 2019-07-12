package qz.ui;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ListResourceBundle;
import java.util.Locale;

public class LanguageBundle extends ListResourceBundle {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LanguageBundle.class);
    private Object[][] translationArray;

    public LanguageBundle(String bundleDirectory, Locale locale) {
        super();
        URL fileLocation = ClassLoader.getSystemResource(bundleDirectory + "_" + locale.toString() + ".json");

        try {
            JSONObject root = new JSONObject(IOUtils.toString(fileLocation));
            JSONArray translations = root.getJSONArray("translations");
            translationArray = new Object[translations.length()][2];

            for (int i = 0; i < translations.length(); i++) {
                JSONArray pair = translations.getJSONArray(i);
                translationArray[i][0] = pair.getString(0);
                translationArray[i][1] = pair.getString(1);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        //Todo Remove this debugging log
        log.warn(locale.toString());
    }
    @Override
    protected Object[][] getContents() {
        return translationArray;
    }
}
