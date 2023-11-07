package qz.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.certificate.CertificateManager;
import qz.installer.certificate.KeyPairWrapper;

import java.util.Properties;

import static qz.installer.certificate.KeyPairWrapper.Type.CA;
import static qz.installer.certificate.KeyPairWrapper.Type.SSL;

/**
 * Convenience class for searching for preferences on a user, app and <code>System.getProperty(...)</code> level
 */
public class PrefsSearch {
    private static final Logger log = LogManager.getLogger(PrefsSearch.class);
    private static Properties appProps = null;

    private static String getProperty(String[] names, String defaultVal, boolean searchSystemProperties, Properties ... propArray) {
        String returnVal;

        // If none are provided, ensure we have some types of properties to iterate over
        if(propArray.length == 0) {
            if(appProps == null) {
                appProps = CertificateManager.loadProperties(new KeyPairWrapper(SSL), new KeyPairWrapper(CA));
            }
            propArray = new Properties[]{ appProps };
        }

        for(String n : names) {
            // First, honor System property
            if (searchSystemProperties && (returnVal = System.getProperty(n)) != null) {
                log.info("Picked up system property {}={}", n, returnVal);
                return returnVal;
            }

            for(Properties props : propArray) {
                // Second, honor properties file(s)
                if (props != null) {
                    if ((returnVal = props.getProperty(n)) != null) {
                        log.info("Picked up property {}={}", n, returnVal);
                        return returnVal;
                    }
                }
            }
        }

        // Last, return default property
        return defaultVal;
    }

    /*
     * Typed String[] helper implementations
     */
    private static int getInt(String[] names, int defaultVal, boolean searchSystemProperties, Properties ... propsArray) {
        try {
            return Integer.parseInt(getProperty(names, "", searchSystemProperties, propsArray));
        } catch(NumberFormatException ignore) {}
        return defaultVal;
    }

    private static boolean getBoolean(String[] names, boolean defaultVal, boolean searchSystemProperties, Properties ... propsArray) {
        return Boolean.parseBoolean(getProperty(names, "" + defaultVal, searchSystemProperties, propsArray));
    }

    /*
     * Typed ArgValue implementations
     */
    public static String getString(ArgValue argValue, boolean searchSystemProperties, Properties ... propsArray) {
        return getProperty(argValue.getMatches(), (String)argValue.getDefaultVal(), searchSystemProperties, propsArray);
    }

    public static int getInt(ArgValue argValue, boolean searchSystemProperties, Properties ... propsArray) {
        return getInt(argValue.getMatches(), (Integer)argValue.getDefaultVal(), searchSystemProperties, propsArray);
    }

    public static boolean getBoolean(ArgValue argValue, boolean searchSystemProperties, Properties ... propsArray) {
        return getBoolean(argValue.getMatches(), (Boolean)argValue.getDefaultVal(), searchSystemProperties, propsArray);
    }

    /*
     * Typed ArgValue implementations (searchSystemProperties = true)
     */
    public static String getString(ArgValue argValue, Properties ... propsArray) {
        return getString(argValue, true, propsArray);
    }

    public static int getInt(ArgValue argValue, Properties ... propsArray) {
        return getInt(argValue, true, propsArray);
    }

    public static boolean getBoolean(ArgValue argValue, Properties ... propsArray) {
        return getBoolean(argValue, true, propsArray);
    }
}
