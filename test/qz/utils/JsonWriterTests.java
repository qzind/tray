package qz.utils;

import org.codehaus.jettison.json.JSONException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class JsonWriterTests {

    private static final Logger log = LogManager.getLogger(JsonWriterTests.class);

    private static String DEFAULT_PATH = "/Applications/Firefox.app/Contents/Resources/distribution/policies.json";
    private static String DEFAULT_DATA = "{ \"policies\": { \"Certificates\": { \"ImportEnterpriseRoots\": true } } }";
    private static boolean DEFAULT_OVERWRITE = false;
    private static boolean DEFAULT_DELETE = false;

    public static void main(String... args) {
        String usingPath = DEFAULT_PATH;
        if (args.length > 0) {
            usingPath = args[0];
        }
        String usingData = DEFAULT_DATA;
        if (args.length > 1) {
            usingData = args[1];
        }
        boolean usingOverwrite = DEFAULT_OVERWRITE;
        if (args.length > 2) {
            usingOverwrite = Boolean.parseBoolean(args[2]);
        }
        boolean usingDeletion = DEFAULT_DELETE;
        if (args.length > 3) {
            usingDeletion = Boolean.parseBoolean(args[3]);
        }

        try {
            JsonWriter.write(usingPath, usingData, usingOverwrite, usingDeletion);
        }
        catch(JSONException jsone) {
            log.error("Failed to read JSON", jsone);
        }
        catch(IOException ioe) {
            log.error("Failed to access file", ioe);
        }
    }
}
