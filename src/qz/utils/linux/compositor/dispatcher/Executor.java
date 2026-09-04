package qz.utils.linux.compositor.dispatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shell executor with built-in pattern matcher
 */
public class Executor {
    private static final Logger log = LogManager.getLogger(Executor.class);

    private static boolean debug = false;

    String regexPattern;
    String[] execute;

    Executor(String regexPattern, String ... execute) {
        this.regexPattern = regexPattern;
        this.execute = execute;
    }

    public static void setDebug(boolean debug) {
        Executor.debug = debug;
    }

    String getString() {
        if (this.regexPattern != null && this.execute != null && this.execute.length > 0) {
            Pattern pattern = Pattern.compile(this.regexPattern, Pattern.DOTALL);
            String output = ShellUtilities.executeRaw(execute, !debug);

            if (!output.isBlank()) {
                Matcher matcher = pattern.matcher(output);
                if (matcher.find()) {
                    // If capturing groups are defined, return group 1; otherwise return full match
                    String found = (matcher.groupCount() > 0 ? matcher.group(1).trim() : matcher.group(0).trim());
                    if(debug) {
                        log.debug("Found '{}' matching regex '{}'", found, regexPattern);
                    }
                    return found;
                }
            }
        }
        return null;
    }

    Integer getInteger() {
        try {
            return Integer.parseInt(getString());
        } catch(NumberFormatException nfe) {
            log.warn("Unable to parse integer from '{}' {}", getString(), nfe.getMessage());
        }
        return null;
    }

    Double getDouble() {
        try {
            return Double.parseDouble(getString());
        } catch(NumberFormatException nfe) {
            log.warn("Unable to parse double from '{}' {}", getString(), nfe.getMessage());
        }
        return null;
    }

    /**
     * Appends the specified string to tne end of the params and returns
     * if the command was successful
     */
    boolean executeWithParam(String input) {
        List<String> execute = new ArrayList<>(Arrays.asList(this.execute));
        execute.add(input);
        return ShellUtilities.execute(execute.toArray(new String[0]), !debug);
    }
}
