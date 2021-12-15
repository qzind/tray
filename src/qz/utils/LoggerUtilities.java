package qz.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerUtilities {

    /**
     * Helper method for parse warnings
     *
     * @param expectedType Expected entry type
     * @param name         Option name
     * @param actualValue  Invalid value passed
     */
    public static void optionWarn(Logger log, String expectedType, String name, Object actualValue) {
        if (actualValue == null || String.valueOf(actualValue).isEmpty()) { return; } //no need to report an unsupplied value
        log.warn("Cannot read {} as a {} for {}, using default", actualValue, expectedType, name);
    }

    /** Gets a correctly cast root logger to add appenders on top */
    public static org.apache.logging.log4j.core.Logger getRootLogger() {
        return (org.apache.logging.log4j.core.Logger)LogManager.getRootLogger();
    }

}
