package qz.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import qz.build.jlink.Parsable;
import qz.common.Sluggable;
import qz.installer.apps.locator.MacAppLocator;
import qz.installer.apps.policy.PolicyInstaller;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
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
            String key = null;
            List<Object> nestedArray = null;
            for(String line : lines) {
                String value = trim(line);
                if(value.isBlank() || value.startsWith("{") || value.startsWith("}")) {
                    continue;
                }
                /*if(!value.contains(" = ")) {
                    log.warn("Skipping dictionary line '{}', it doesn't contain ' = '", value);
                    continue;
                }*/
                String[] parts = value.split(" = ", 2);
                String rawValue;
                if(parts.length == 2) {
                    key = parts[0].trim();
                    rawValue = parts[1].trim();
                } else {
                    rawValue = parts[0].trim();
                }

                if(rawValue.equals("(")) {
                    // nested array
                    nestedArray = new ArrayList<>();
                } else if (nestedArray != null) {
                    if (!rawValue.startsWith(")")) {
                        // append next value
                        nestedArray.add(parseObject(rawValue, false));
                    } else {
                        // end of array
                        values.put(key, nestedArray.toArray());
                    }
                } else if(key != null) {
                    values.put(key, parseObject(rawValue, true));
                }
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

            try {
                // Float
                if (raw.contains(".")) {
                    return fromFloat(raw);
                }

                // Int
                return fromInteger(raw);
            } catch(NumberFormatException e) {
                // Nested objects don't use double quotes.  Why apple?
                // Fallback on string, I guess
                return raw;
            }
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
        try {
            Object o = mapEntry.getValue();

            // Convert to nested XML format
            if(o instanceof Map || o instanceof Object[]) {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                Element element = createElement(doc, mapEntry.getValue());
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty("omit-xml-declaration", "yes");
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(element), new StreamResult(writer));
                return ShellUtilities.execute(defaultsCliPrepare(plist, PlistOperation.WRITE, entry, PlistEntryType.DICT.getValueType(), mapEntry.getKey(), writer.toString()));
            }
            // add type for primitives
            String valueType = PlistEntryType.getType(o).getValueType();
            return ShellUtilities.execute(defaultsCliPrepare(plist, PlistOperation.WRITE, entry, PlistEntryType.DICT.getValueType(), mapEntry.getKey(), valueType, o.toString()));

        } catch(ParserConfigurationException | TransformerException e) {
            log.warn("An unexpected error occurred trying to prepare the dictionary write command", e);
        }
        return false;
    }

    public static Element createElement(Document doc, Object o) {
        PlistEntryType type = PlistEntryType.getType(o);
        Element element = doc.createElement(type.slug);
        switch(type) {
            case BOOLEAN:
                // <true/>|<false/>
                element = doc.createElement(o.toString());
                break;
            case FLOAT:
                element = doc.createElement("real");
            case STRING:
            case INTEGER:
                element.setTextContent(o.toString());
                break;
            case ARRAY:
                Object[] array = (Object[])o;
                for(Object item : array) {
                    element.appendChild(createElement(doc, item));
                }
                break;
            case DICT:
                Map<String,Object> map = PolicyInstaller.objectToMap(o);
                for(Map.Entry<String,Object> entry : map.entrySet()) {
                    Element key = doc.createElement("key");
                    key.setTextContent(entry.getKey());
                    element.appendChild(key);
                    element.appendChild(createElement(doc, entry.getValue()));
                }
                break;
            default:
                throw new UnsupportedOperationException(String.format("Can't create DOM element of type '%s' for value '%s'", type, o));
        }
        return element;
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

    public static Object[] getArray_OLD(Path plist, String entry) {
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

    public static HashMap<String, Object> getMap_OLD(Path plist, String entry) {
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

    public static Object[] getArray(Path plist, String entry) {
        Object o = parseMap(getRootNodeList(plist)).get(entry);
        if(o instanceof Object[]) {
            return (Object[])o;
        }
        return new Object[0];
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> getMap(Path plist, String entry) {
        HashMap<String, Object> o = parseMap(getRootNodeList(plist));
        if(o.get(entry) instanceof HashMap) {
            return (HashMap<String,Object>)o.get(entry);
        }
        return new HashMap<>();
    }

    private static NodeList getRootNodeList(Path plist)  {
        try {
            String rawXml = ShellUtilities.executeRaw(
                    "/usr/bin/plutil", "-convert", "xml1", "-o", "-", plist.toString());
            Document doc = MacAppLocator.createCompatibleDocument(new ByteArrayInputStream(rawXml.getBytes(StandardCharsets.UTF_8)));
            Element element = doc.getDocumentElement();
            NodeList root = element.getElementsByTagName("dict");
            if(root.getLength() > 0) {
                return root.item(0).getChildNodes();
            }
        }
        catch(ParserConfigurationException | IOException | SAXException e) {
            log.warn("An unexpected error occurred trying to parse the plist file", e);
        }
        return null;
    }

    public static HashMap<String, Object> parseMap(NodeList nodeList) {
        HashMap<String, Object> map = new HashMap<>();
        if(nodeList == null || nodeList.getLength() == 0) {
            return map;
        }
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if(item.getNodeName().equals("key")) {
                String key = item.getTextContent();
                Node valueItem = item.getNextSibling();
                if(valueItem.getNodeName().equals("dict")) {
                    map.put(key, parseMap(valueItem.getChildNodes()));
                } else if(valueItem.getNodeName().equals("array")) {
                    map.put(key, parseArray(valueItem.getChildNodes()));
                } else {
                    map.put(key, parseNodeValue(valueItem));
                }
            }
        }
        return map;
    }

    public static Object[] parseArray(NodeList nodeList) {
        List<Object> objectArray = new ArrayList<>();
        for(int i = 0; i < nodeList.getLength(); i++) {
            objectArray.add(parseNodeValue(nodeList.item(i)));
        }
        return objectArray.toArray();
    }

    public static Object parseNodeValue(Node node) {
        switch(node.getNodeName()) {
            case "dict":
                return parseMap(node.getChildNodes());
            case "array":
                return parseArray(node.getChildNodes());
            case "true":
                return true;
            case "false":
                return false;
            case "real":
                return Float.parseFloat(node.getTextContent());
            case "integer":
                return Integer.parseInt(node.getTextContent());
            case "date":
                return Instant.parse(node.getTextContent());
            case "data":
                return Base64.getDecoder().decode(node.getTextContent().replaceAll("\\s+", ""));
            case "string":
            default:
        }
        return node.getTextContent();
    }
}
