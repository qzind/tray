package qz.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 * Convenience class for searching for preferences on a user, app and <code>System.getProperty(...)</code> level
 */
public class PrefsSearch {
    protected static final Logger log = LoggerFactory.getLogger(PrefsSearch.class);

    public static String get(Properties user, Properties app, String name) {
        return get(user, app, name, null);
    }

    public static String get(Properties app, String name, String defaultVal, boolean searchSystemProperties) {
        return get(null, app, name, defaultVal, searchSystemProperties);
    }

    public static String get(Properties user, Properties app, String name, String defaultVal, String... altNames) {
        return get(user, app, name, defaultVal, true, altNames);
    }

    public static String get(Properties user, Properties app, String name, String defaultVal, boolean searchSystemProperties, String... altNames) {
        String returnVal;

        ArrayList<String> names = new ArrayList<>();
        names.add(name);

        if (altNames != null && altNames.length > 0) {
            names.addAll(Arrays.asList(altNames));
        }

        for(String n : names) {
            // First, honor System property
            if (searchSystemProperties && (returnVal = System.getProperty(n)) != null) {
                log.info("Picked up system property {}={}", n, returnVal);
                return returnVal;
            }

            // Second, honor user preference
            if (user != null) {
                if ((returnVal = user.getProperty(n)) != null) {
                    log.info("Picked up user preference {}={}", n, returnVal);
                    return returnVal;
                }
            }

            // Third, honor app property
            if (app != null) {
                if ((returnVal = app.getProperty(n)) != null) {
                    log.info("Picked up app property {}={}", n, returnVal);
                    return returnVal;
                }
            }
        }

        // Last, return default property
        return defaultVal;
    }
}
