package qz.ui.headless;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.swing.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static qz.ui.headless.Endpoint.*;

public abstract class HeadlessDialog {
    private final static Logger log = LogManager.getLogger(HeadlessDialog.class);

    private Endpoint endpoint;

    public void setEndpoint(String value) {
        if((endpoint = Endpoint.parse(value)) != null) {
            log.info("Headless endpoint configured as {} pointing to '{}'", endpoint.getType(), endpoint);
        }
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public static JSONObject serializeToJson(HeadlessDialog dialog) throws JSONException {
        return serializeToJson(dialog.serialize());
    }

    public JSONObject promptAndWait() throws JSONException {
        // log.info("\n{}\n", serializeToJson(this).toString(2));
        return createPromptable().prompt(endpoint, serializeToJson(this));
    }

    private Promptable createPromptable() {
        switch(endpoint.getType()) {
            case REST_URL:
                return new RestPrompt();
            case FILE_PROMPT:
            default:
                return new FilePrompt();
        }
    }

    /**
     * Recursive function for converting nested HashMaps to JSON
     */
    @SuppressWarnings("unchecked")
    public static JSONObject serializeToJson(HashMap<String, Object> serializedFields) {
        JSONObject obj = new JSONObject();
        for(Map.Entry<String, Object> entry : serializedFields.entrySet()) {
            try {
                Object o = entry.getValue();
                if(o instanceof HashMap) {
                    obj.put(entry.getKey(), serializeToJson((HashMap<String, Object>)o));
                } else {
                    obj.put(entry.getKey(), entry.getValue());
                }
            } catch(JSONException e) {
                log.warn(e);
            }
        }
        return obj;
    }

    /**
     * Runs the provided action on the EDT unless headless
     */
    public static void runSafely(boolean headless, Runnable action) {
        try {
            if (headless || SwingUtilities.isEventDispatchThread()) {
                action.run();
            } else {
                SwingUtilities.invokeAndWait(action);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            log.warn("A runnable was cancelled due to an unhandled exception {}", e.getLocalizedMessage());
        }
    }

    public abstract LinkedHashMap<String,Object> serialize() throws JSONException;

    public abstract JDialog getDialog();
}
