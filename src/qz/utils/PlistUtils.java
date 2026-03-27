package qz.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.build.jlink.Parsable;
import qz.common.Sluggable;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Wrapper around macOS 'default read', 'defaults write', etc
 */
public class PlistUtils {
    private static final Logger log = LogManager.getLogger(PlistUtils.class);

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
        DICT("dict", "dictionary"),
        MISSING(),
        UNKNOWN();

        public final String[] matches;
        private final String slug;
        private final String valueType;

        PlistEntryType(String ... matches) {
            this.matches = matches;
            this.slug = Sluggable.slugOf(name());
            // "-array" can be destructive, so we'll prefer "-array-add" instead
            this.valueType = slug.equals("array") || slug.equals("dict") ?
                    String.format("-%s-add", slug) : String.format("-%s", slug); // -boolean, -string, etc
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

        public static PlistEntryType getType(Object o) {
            if(o instanceof String) {
                return STRING;
            }
            if(o instanceof byte[]) {
                return DATA;
            }
            if(o instanceof Integer) {
                return INTEGER;
            }
            if(o instanceof Float) {
                return FLOAT;
            }
            if(o instanceof Boolean) {
                return BOOLEAN;
            }
            if(o instanceof Date) {
                return DATE;
            }
            if(o instanceof Object[]) {
                return ARRAY;
            }
            if(o instanceof Map) {
                return DICT;
            }
            if(o == null) {
                return MISSING;
            }
            return UNKNOWN;
        }

        @SuppressWarnings("unused")
        public static String fromString(String value) {
            return trim(value);
        }

        @SuppressWarnings("unused")
        public static byte[] fromData(String rawValue) {
            throw new UnsupportedOperationException("Sorry, parsing bytes from cli is not yet supported");
        }

        @SuppressWarnings("unused")
        public static Integer fromInteger(String rawValue) {
            return Integer.parseInt(trim(rawValue));
        }

        @SuppressWarnings("unused")
        public static Float fromFloat(String rawValue) {
            return Float.parseFloat(trim(rawValue));
        }

        @SuppressWarnings("unused")
        public static Boolean fromBoolean(String rawValue) {
            return "1".equals(trim(rawValue));
        }

        @SuppressWarnings("unused")
        public static Boolean fromDate(String rawValue) {
            throw new UnsupportedOperationException("Sorry, parsing dates from cli is not yet supported");
        }

        @SuppressWarnings("unused")
        public static boolean fromUnknown(String rawValue) {
            throw new UnsupportedOperationException("Sorry, parsing unknowns from cli is not yet supported");
        }

        // For simplicity's sake (at least for now) assume all arrays are one-level, one-dimensional
        // and only contain primitive types such as string, int, float, boolean
        public static ArrayList<Object> fromArray(String haystack) {
            ArrayList<Object> values = new ArrayList<>();
            String[] lines = trim(haystack).split("[\r?\n]+");
            for(String line : lines) {
                String rawValue = trim(line);
                if(rawValue.isBlank() || rawValue.startsWith("(") || rawValue.startsWith(")")) {
                    continue;
                }
                values.add(parseObject(rawValue, false));
            }
            return values;
        }

        /**
         * Converts the output of the terminal to a <code>HashMap&lt;String,Object&gt;</code>
         * For simplicity's sake (at least for now) assume all dictionaries are one-level, one-dimensional
         *
         * @param haystack The terminal output to parse
         */
        public static HashMap<String, Object> fromDictionary(String haystack) {
            HashMap<String, Object> values = new HashMap<>();
            String[] lines = trim(haystack).split("[\r?\n]+");
            for(String line : lines) {
                String value = trim(line);
                if(value.isBlank() || value.startsWith("{") || value.startsWith("}")) {
                    continue;
                }
                if(!value.contains(" = ")) {
                    log.warn("Skipping dictionary line '{}', it doesn't contain ' = '", value);
                    continue;
                }
                String[] parts = value.split(" = ", 2);
                if(parts.length < 2) {
                    log.warn("Skipping dictionary line '{}', it's not in an expected format ' = '", value);
                    continue;
                }

                String key = parts[0].trim();
                String rawValue = parts[1].trim();

                values.put(key, parseObject(rawValue, true));
            }
            return values;
        }

        /**
         * Attempts to parse terminal output to a String, Integer, Float or (conditionally) Boolean
         *
         * @param raw raw terminal output
         * @param assumeBooleans  Automatically converts zeros and ones to true/false due to limitations of <code>defaults read</code>
         */
        private static Object parseObject(String raw, boolean assumeBooleans) {
            // Dictionary entries end in ';', arrays end in ','
            if(raw.endsWith(";") || raw.endsWith(",")) {
                raw = raw.substring(0, raw.length() - 1);
            }

            // String
            if(raw.startsWith("\"")) {
                raw = raw.replaceFirst("\"", "");
                if(raw.endsWith("\"")) {
                    raw = raw.substring(0, raw.length() - 1);
                }
                return raw;
            }

            if(assumeBooleans) {
                // Boolean
                if (raw.equalsIgnoreCase("1") || raw.equalsIgnoreCase("0")) {
                    return fromBoolean(raw);
                }
            }

            // Float
            if(raw.contains(".")) {
                return fromFloat(raw);
            }

            // Int
            return fromInteger(raw);
        }

        /**
         * Read the array, but with special assumption that duplicated entries aren't wanted or warranted
         */
        private static HashSet<Object> fromUniqueArray(String haystack) {
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
                if(defaultsReadType(plist, entry) != PlistEntryType.MISSING) {
                    return ShellUtilities.execute(defaultsCliPrepare(plist, operation, entry));
                }
                return true; // nothing to delete
        }
        return ShellUtilities.execute(defaultsCliPrepare(plist, operation, entry, type.getValueType(), value.toString()));
    }

    public static PlistEntryType defaultsReadType(Path plist, String entry) {
        return PlistEntryType.parse(defaultsCliGet(plist, PlistOperation.READ_TYPE, entry));
    }

    public static String defaultsRead(Path plist, String entry) {
        return defaultsCliGet(plist, PlistOperation.READ, entry);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean defaultsDelete(Path plist, String entry) {
        return defaultsCliPut(plist, PlistOperation.DELETE, entry, PlistEntryType.MISSING, null);
    }

    @SuppressWarnings("unused")
    public static boolean defaultsWrite(Path plist, String entry, PlistEntryType type, Object value) {
        return defaultsCliPut(plist, PlistOperation.WRITE, entry, type, value);
    }

    private static boolean defaultWriteDictionary(Path plist, String entry, Map.Entry<String, Object> mapEntry) {
        String mapValueType = PlistEntryType.getType(mapEntry.getValue()).getValueType();
        return ShellUtilities.execute(defaultsCliPrepare(plist, PlistOperation.WRITE, entry, PlistEntryType.DICT.getValueType(), mapEntry.getKey(), mapValueType, mapEntry.getValue().toString()));
    }

    private static boolean defaultsWriteArrayAdd(Path plist, String entry, Object value) {
        // wrap in double quotes to prevent bugs with values such as "[*.]qz.io"
        return defaultsCliPut(plist, PlistUtils.PlistOperation.WRITE, entry, PlistUtils.PlistEntryType.ARRAY, String.format("\"%s\"", value));
    }

    @SuppressWarnings("unused")
    public static boolean defaultsRename(Path plist, String entry, String newName) {
        return defaultsCliPut(plist, PlistOperation.RENAME, entry, PlistEntryType.MISSING, newName);
    }

    public static Collection<Object> getArray(Path plist, String entry, boolean unique) {
        if(unique) {
            return PlistEntryType.fromUniqueArray(defaultsRead(plist, entry));
        } else {
            return PlistEntryType.fromArray(defaultsRead(plist, entry));
        }
    }

    public static boolean write(Path plist, String entry, Object value) {
        if(delete(plist, entry)) {
           return defaultsWrite(plist, entry, PlistEntryType.getType(value), value);
        }
        return false;
    }

    public static boolean delete(Path plist, String entry) {
        PlistEntryType type = defaultsReadType(plist, entry);
        if(type != PlistEntryType.MISSING) {
            if(!defaultsDelete(plist, entry)) {
                log.warn("An error occurred deleting '{}' from {}", entry, plist);
                return false;
            }
        }
        return true;
    }

    public static boolean writeMap(Path plist, String entry, Map<String, Object> values) {
        for(Map.Entry<String,Object> mapEntry : values.entrySet()) {
            if(!defaultWriteDictionary(plist, entry, mapEntry)) {
                log.warn("An error occurred writing '{}': '{}:{}' to {}", entry, mapEntry.getKey(), mapEntry.getValue(), plist);
                return false;
            }
        }
        return true;
    }

    /**
     * Blindly write the array to the specified location
     */
    public static boolean writeArray(Path plist, String entry, Collection<Object> values) {
        // preparing multiple array values for cli injection is error-prone, instead do one at a time
        for(Object value : values) {
            if(!defaultsWriteArrayAdd(plist, entry, value)) {
                log.warn("An error occurred writing '{}': '{}' to {}", entry, value, plist);
                return false;
            }
        }
        return true;
    }

    public static Object getValue(Path plist, String entry) {
        PlistEntryType type = defaultsReadType(plist, entry);
        switch(type) {
            case STRING:
                return PlistEntryType.fromString(defaultsRead(plist, entry));
            case INTEGER:
                return PlistEntryType.fromInteger(defaultsRead(plist, entry));
            case FLOAT:
                return PlistEntryType.fromFloat(defaultsRead(plist, entry));
            case BOOLEAN:
                return PlistEntryType.fromBoolean(defaultsRead(plist, entry));
            case MISSING:
                break;
            case ARRAY: // wrong function call
            case DATA: // not yet supported
            case DATE: // not yet supported
            default:
                log.info("Preference entry type '{}' is not yet supported for getValue() at this time.", entry);
        }
        return null;
    }

    public static Object[] getArray(Path plist, String entry) {
        PlistEntryType type = defaultsReadType(plist, entry);
        switch(type) {
            case ARRAY:
                return PlistEntryType.fromArray(defaultsRead(plist, entry)).toArray();
            case MISSING:
                break;
            case DATA: // not yet supported
            case DATE: // not yet supported
            default:
                log.info("Preference entry type '{}' is not an array.", entry);
        }
        return null;
    }

    public static HashMap<String, Object> getMap(Path plist, String entry) {
        PlistEntryType type = defaultsReadType(plist, entry);
        switch(type) {
            case DICT:
                return PlistEntryType.fromDictionary(defaultsRead(plist, entry));
            case MISSING:
                break;
            case DATA: // not yet supported
            case DATE: // not yet supported
            default:
                log.info("Preference entry type '{}' is not a dictionary.", entry);
        }
        return null;
    }
}
