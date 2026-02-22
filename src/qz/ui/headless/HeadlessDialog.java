package qz.ui.headless;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.build.jlink.Parsable;
import qz.common.Constants;
import qz.utils.ArgValue;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public interface HeadlessDialog {
    Logger log = LogManager.getLogger(HeadlessDialog.class);

    class Endpoint {
        public enum Type implements Parsable<Type> {
            FILE_PROMPT,
            REST_URL;
        }

        private final URL url;
        private final Path path;
        private final Type type;

        public Endpoint(Type type, Path path) {
            this.type = type;
            this.path = path;
            this.url = null;
        }

        public Endpoint(Type type, URL url) {
            this.type = type;
            this.path = null;
            this.url = url;
        }

        public Type getType() {
            return type;
        }

        public Path getPath() {
            return path;
        }

        public URL getUrl() {
            return url;
        }

        public static Endpoint parse(String value) {
            try {
                String lower = value.toLowerCase(Locale.ENGLISH);
                Type type = lower.matches("https?:")? Type.REST_URL:Type.FILE_PROMPT;
                switch(type) {
                    case REST_URL:
                        return new Endpoint(type, new URL(value));
                    default:
                        return new Endpoint(type, Path.of(value));
                }
            }
            catch(Exception e) {
                log.warn("Unable to parse headless dialog endpoint {}.  If you're using {} in headless mode, certificates must be allowed manually via '{}'.", value, Constants.ABOUT_TITLE, ArgValue.ALLOW.getMatch(), e);
            }
            return null;
        }
    }

    void setEndpoint(String value);

    LinkedHashMap<String, Object>[] getFields() throws JSONException;

    static JSONArray serializeFields(HeadlessDialog dialog) throws JSONException {
            JSONArray jsonArray = new JSONArray();

            for(HashMap<String,Object> fieldMap : dialog.getFields()) {
                JSONObject obj = new JSONObject();
                for(Map.Entry<String, Object> entry : fieldMap.entrySet()) {
                    try {
                        obj.put(entry.getKey(), entry.getValue());
                    } catch(JSONException e) {
                        log.warn(e);
                    }
                }
                jsonArray.put(obj);
            }

            return jsonArray;
    }

    /**
     * Runs the provided action on the EDT unless headless
     */
    static void runSafely(boolean headless, Runnable action) {
        try {
            if (headless || SwingUtilities.isEventDispatchThread()) {
                action.run();
            } else {
                SwingUtilities.invokeAndWait(action);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.error(e);
        }
    }
}
