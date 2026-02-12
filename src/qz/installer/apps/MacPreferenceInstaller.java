package qz.installer.apps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.build.jlink.Parsable;
import qz.common.Sluggable;
import qz.utils.ShellUtilities;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class MacPreferenceInstaller {
    private static final Logger log = LogManager.getLogger(MacPreferenceInstaller.class);

    /*
     * Strict values for cli operations
         defaults [-currentHost | -host hostname] read [domain [key]]
         defaults [-currentHost | -host hostname] read-type domain key
         defaults [-currentHost | -host hostname] write domain { 'plist' | key 'value' }
         defaults [-currentHost | -host hostname] rename domain old_key new_key
         defaults [-currentHost | -host hostname] delete [domain [key]]
     */
    public enum PlistOperation implements Sluggable {
        READ,
        READ_TYPE,
        WRITE,
        RENAME,
        DELETE;

        @Override
        public String slug() {
            return Sluggable.slugOf(name());
        }
    }

    /*
     * Specifying value types for preference keys:
                 If no type flag is provided, defaults will assume the value is a string. For best results, use one of the
                 type flags, listed below.
     -string     Allows the user to specify a string as the value for the given preference key.
     -data       Allows the user to specify a bunch of raw data bytes as the value for the given preference key.  The data
                 must be provided in hexadecimal.
     -int[eger]  Allows the user to specify an integer as the value for the given preference key.
     -float      Allows the user to specify a floating point number as the value for the given preference key.
     -bool[ean]  Allows the user to specify a boolean as the value for the given preference key.  Value must be TRUE,
                 FALSE, YES, or NO.
     -date       Allows the user to specify a date as the value for the given preference key.
     -array      Allows the user to specify an array as the value for the given preference key:
                       defaults write somedomain preferenceKey -array element1 element2 element3
                 The specified array overwrites the value of the key if the key was present at the time of the write. If
                 the key was not present, it is created with the new value.
     */
    public enum PlistEntryType implements Sluggable {
        STRING("string"),
        DATA("data"),
        INTEGER("int", "integer"),
        FLOAT("float"),
        BOOLEAN("bool", "boolean"),
        DATE("date"),
        ARRAY("array"),
        MISSING(),
        UNKNOWN();

        public final String[] matches;
        private final String slug;
        private final String valueType;

        PlistEntryType(String ... matches) {
            this.matches = matches;
            this.slug = Sluggable.slugOf(name());
            // "-array" can be destructive, so we'll prefer "-array-add" instead
            this.valueType = slug.equals("array") ? "-array-add" : String.format("-%s", slug); // -boolean, -string, etc
        }

        public static PlistEntryType parse(String input) {
            if(input == null || input.isBlank()) {
                return MISSING;
            }
            String[] split = trim(input).split(" ");
            if(split.length > 0) {
                return Parsable.parse(PlistEntryType.class, split[split.length - 1]);
            }
            // should never get here... perhaps apple's added a new type
            return UNKNOWN;
        }

        public static String fromString(String value) {
            return trim(value);
        }

        public static byte[] fromData(String rawValue) {
            throw new UnsupportedOperationException("Sorry, parsing bytes from cli is not yet supported");
        }

        public static Integer fromInteger(String rawValue) {
            return Integer.parseInt(trim(rawValue));
        }

        public static Float fromFloat(String rawValue) {
            return Float.parseFloat(trim(rawValue));
        }

        public static Boolean fromBoolean(String rawValue) {
            return "1".equals(trim(rawValue));
        }

        public static Boolean fromDate(String rawValue) {
            throw new UnsupportedOperationException("Sorry, parsing dates from cli is not yet supported");
        }

        public static boolean fromUnknown(String rawValue) {
            throw new UnsupportedOperationException("Sorry, parsing unknowns from cli is not yet supported");
        }

        // For simplicity's sake (at least for now) assume all arrays are one-level, one-dimensional
        // and only contain string values
        public static ArrayList<String> fromArray(String haystack) {
            ArrayList<String> values = new ArrayList<>();
            String[] lines = trim(haystack).split("[\r?\n]+");
            for(String line : lines) {
                String value = trim(line);
                if(value.isBlank() || value.startsWith("(") || value.startsWith(")")) {
                    continue;
                }
                if(value.endsWith(",")) {
                    value = value.substring(0, value.length() - 1);
                }
                if(value.endsWith("\"")) {
                    value = value.substring(0, value.length() - 1);
                }
                if(value.startsWith("\"")) {
                    value = value.replaceFirst("\"", "");
                }
                values.add(value);
            }
            return values;
        }

        /**
         * Read the array, but with special assumption that duplicated entries aren't wanted or warranted
         */
        private static HashSet<String> fromUniqueArray(String haystack) {
            return new HashSet<>(fromArray(haystack));
        }

        private static String trim(String value) {
            return value == null ? null: value.trim();
        }

        @Override
        public String slug() {
            return slug;
        }

        public String getValueType() {
            return valueType;
        }
    }

    protected static String[] defaultsCliPrepare(Path plist, PlistOperation operation, String entry, String ... more) {
        String[] cli = { "/usr/bin/defaults", operation.slug(), plist.toString(), entry };
        return Stream.concat(Arrays.stream(cli), Arrays.stream(more)).toArray(String[]::new);
    }

    /**
     * Get a value from cli, return the <code>String</code> response
     */
    protected static String defaultsCliGet(Path plist, PlistOperation operation, String entry) {
        return ShellUtilities.executeRaw(defaultsCliPrepare(plist, operation, entry));
    }

    /**
     * Put a value from cli, return if successful
     */
    protected static boolean defaultsCliPut(Path plist, PlistOperation operation, String entry, PlistEntryType type, Object value) {
        switch(operation) {
            case RENAME:
                // type is omitted
                return ShellUtilities.execute(defaultsCliPrepare(plist, operation, entry, value.toString()));
            case DELETE:
                // value and type are omitted
                return ShellUtilities.execute(defaultsCliPrepare(plist, operation, entry));
        }
        String stringVal = value.toString();
        switch(type) {
            case UNKNOWN:
                type = PlistEntryType.STRING;
                break;
            case BOOLEAN:
                // man pages say "TRUE", not "true"; both seem to work; we'll err on the side of caution
                stringVal = stringVal.toUpperCase(Locale.ENGLISH);
                break;
            default:
        }
        return ShellUtilities.execute(defaultsCliPrepare(plist, operation, entry, type.getValueType(), stringVal));
    }

    private static PlistEntryType defaultsReadType(Path plist, String entry) {
        return PlistEntryType.parse(defaultsCliGet(plist, PlistOperation.READ_TYPE, entry));
    }

    private static String defaultsRead(Path plist, String entry) {
        return defaultsCliGet(plist, PlistOperation.READ, entry);
    }

    private static boolean defaultsDelete(Path plist, String entry) {
        return defaultsCliPut(plist, PlistOperation.DELETE, entry, PlistEntryType.UNKNOWN, null);
    }

    private static boolean defaultsWrite(Path plist, String entry, PlistEntryType type, Object value) {
        return defaultsCliPut(plist, PlistOperation.WRITE, entry, type, value);
    }

    @SuppressWarnings("unused")
    private static boolean defaultsRename(Path plist, String entry, String newName) {
        return defaultsCliPut(plist, PlistOperation.RENAME, entry, PlistEntryType.UNKNOWN, newName);
    }

    public static Collection<String> getArray(Path plist, String entry, boolean unique) {
        if(unique) {
            return PlistEntryType.fromUniqueArray(defaultsRead(plist, entry));
        } else {
            return PlistEntryType.fromArray(defaultsRead(plist, entry));
        }
    }

    public static boolean appendArray(Path plist, String entry, Collection<String> values, boolean dedupe) {
        Collection<String> existingItems = getArray(plist, entry, dedupe);
        Collection<String> newItems;

        if(dedupe) {
            // clear out the value(s) that were there before
            if(!existingItems.isEmpty()) {
                if(!defaultsDelete(plist, entry)) {
                    log.warn("An error occurred deleting '{}' from {}", entry, plist);
                    return false;
                }
            }
            newItems = new HashSet<>(existingItems);
        } else {
            newItems = new ArrayList<>();
        }
        newItems.addAll(values);

        // preparing multiple array values for cli injection is error-prone, instead do one at a time
        for(String value : newItems) {
            if(!defaultsCliPut(plist, PlistOperation.WRITE, entry, PlistEntryType.ARRAY, value)) {
                log.warn("An error occurred writing '{}': '{}' to {}", entry, value, plist);
                return false;
            }
        }
        return true;
    }

    /*public boolean install(Path plist, String policyName, Object ... values) {
        log.info("Installing macOS Preference(s) {} to {}...", policyName, plist);
        for(Object value : values) {
            String stringified = value.toString();
            String found = ShellUtilities.executeRaw(new String[]{"/usr/bin/defaults", "read", plist.toString(), policyName}, true);
            if(found.contains(value)) {
                log.info("Preference {} '{}' already exists at location {}, skipping", policyName, value, location);
                continue;
            }
            ShellUtilities.execute("/usr/bin/defaults", "write", location, policyName, "-array-add", value);
        }
    }*/

    public static void main(String ... args) {
        String entry = "URLAllowlist"; // array
        //entry = "UserAccountID"; // integer
        //entry = "NSNavPanelExpandedStateForSaveMode"; // boolean
        //entry = "ConfigurationCoPilotEnabled"; // integer
        Path plist = Path.of("/Users/owner/Library/Preferences/com.teamviewer.teamviewer.preferences.plist");
        plist = Path.of("/Users/owner/Library/Preferences/com.google.Chrome.plist");
        String val = "qz://";

        HashSet<String> entries = PlistEntryType.fromUniqueArray(defaultsRead(plist, entry));
        if(!entries.isEmpty()) {
            if (!defaultsDelete(plist, entry)) {
                log.error("Something went wrong deleting '{}' from  {}", entry, plist);
                return;
            }
        }

        entries.add(val);
        if(!appendArray(plist, entry, entries, true)) {
            log.error("Something went wrong writing '{}': '{}' to {}", entry, val, plist);
            return;
        }

        PlistEntryType type = defaultsReadType(plist, entry);
        switch(type) {
            case STRING:
                String stringVal = PlistEntryType.fromString(defaultsRead(plist, entry));
                log.info("{} ({}): {}", entry, type.slug(), stringVal);
                break;
            case INTEGER:
                Integer intVal = PlistEntryType.fromInteger(defaultsRead(plist, entry));
                log.info("{} ({}): {}", entry, type.slug(), intVal);
                break;
            case FLOAT:
                Float floatVal = PlistEntryType.fromFloat(defaultsRead(plist, entry));
                log.info("{} ({}): {}", entry, type.slug(), floatVal);
                break;
            case BOOLEAN:
                Boolean boolVal = PlistEntryType.fromBoolean(defaultsRead(plist, entry));
                log.info("{} ({}): {}", entry, type.slug(), boolVal);
                break;
            case ARRAY:
                // Unit tests can use fromArray to prevent deduping
                ArrayList<String> arrayVal = PlistEntryType.fromArray(defaultsRead(plist, entry));
                log.info("{} ({}):", entry, type.slug());
                arrayVal.forEach(System.out::println);
                break;
            case MISSING:
                log.info("Preference entry '{}' is missing from plist file {}", entry, plist);
                break;
            case DATA: // not supported
            case DATE: // not supported
            default:
                log.info("Preference entry type '{}' is not yet supported at this time.", entry);
                break;
        }
    }
}
