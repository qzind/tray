package qz.utils;

import org.slf4j.Logger;

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

}
