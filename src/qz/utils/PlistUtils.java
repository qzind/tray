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
import javax.xml.transform.TransformerConfigurationException;
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
 * Helper for reading and writing macOS 'plist' preference files
 * Contains two main approaches:
 * - Primitive types: wrapper around macOS 'default read', 'defaults write', etc
 * - Complex types: leverage 'plutil' and parse as XML
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
        FLOAT("float", "real"),
        BOOLEAN("bool", "boolean", "false", "true"),
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

        public static PlistEntryType parseType(Node node) {
            return parseType(node.getNodeName());
        }

        public static PlistEntryType parseType(String input) {
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

        public Object parseValue(Node node) {
            switch(this) {
                case STRING:
                case DATA:
                case INTEGER:
                case FLOAT:
                case DATE:
                    return parseValue(node.getTextContent());
                case BOOLEAN: // <true/>, <false/>
                    return parseValue(node.getNodeName());
                case ARRAY:
                    return parseArray(node.getChildNodes());
                case DICT:
                    return parseMap(node.getChildNodes());
                case MISSING:
                    return null;
                default:
                    throw new UnsupportedOperationException(String.format("Parsing PlistEntryType '%s' is not yet supported", this));
            }
        }

        public Object parseValue(String input) {
            switch(this) {
                case STRING:
                    return fromString(input);
                case DATA:
                    return fromData(input);
                case INTEGER:
                    return fromInteger(input);
                case FLOAT:
                    return fromFloat(input);
                case BOOLEAN:
                    return fromBoolean(input);
                case DATE:
                    return fromDate(input);
                case MISSING:
                    return null;
                case ARRAY:
                case DICT:
                    throw new UnsupportedOperationException(String.format("PlistEntryType '%s' cannot be directly parsed.  Please use parseMap/parseArray instead.", this));
                default:
                    throw new UnsupportedOperationException(String.format("Parsing PlistEntryType '%s' is not yet supported", this));
            }
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
            return Base64.getDecoder().decode(removeWhiteSpace(rawValue));
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
            switch(trim(rawValue)) {
                case "true":
                case "1":
                    return true;
                case "false":
                case "0":
                default:
                    return false;
            }
        }

        @SuppressWarnings("unused")
        public static Instant fromDate(String rawValue) {
            return Instant.parse(trim(rawValue));
        }

        @SuppressWarnings("unused")
        public static boolean fromUnknown(String rawValue) {
            throw new UnsupportedOperationException("Sorry, parsing unknowns from cli is not yet supported");
        }

        private static Object parseObject(Node node) {
            return PlistEntryType.parseType(node).parseValue(node);
        }

        private static String removeWhiteSpace(String value) {
            return value == null ? value : value.replaceAll("\\s+", "");
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

    static Document doc;
    static Transformer transformer;
    static {
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "yes");
        } catch(ParserConfigurationException | TransformerConfigurationException e) {
            log.error("Something went critically wrong trying to initialize the XML parser.  Policy installation will fail.", e);
            doc = null;
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
        return PlistEntryType.parseType(defaultsCliGet(plist, PlistOperation.READ_TYPE, entry));
    }

    public static String defaultsRead(Path plist, String entry) {
        return defaultsCliGet(plist, PlistOperation.READ, entry);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean defaultsDelete(Path plist, String entry) {
        return defaultsCliPut(plist, PlistOperation.DELETE, entry, PlistEntryType.MISSING, null);
    }

    @SuppressWarnings("unused")
    public static boolean defaultsRename(Path plist, String entry, String newName) {
        return defaultsCliPut(plist, PlistOperation.RENAME, entry, PlistEntryType.MISSING, newName);
    }

    @SuppressWarnings("unused")
    public static boolean defaultsWrite(Path plist, String entry, PlistEntryType type, Object value) {
        return defaultsCliPut(plist, PlistOperation.WRITE, entry, type, value);
    }

    private static boolean defaultsWriteArray(Path plist, String entry, Object array) {
        Element element = createElement(doc, array);
        return ShellUtilities.execute(defaultsCliPrepare(plist, PlistOperation.WRITE, entry, PlistEntryType.ARRAY.getValueType(), serialize(element)));
    }

    private static boolean defaultsWriteDict(Path plist, String entry, Map.Entry<String, Object> mapEntry) {
        Element element = createElement(doc, mapEntry.getValue());
        return ShellUtilities.execute(defaultsCliPrepare(plist, PlistOperation.WRITE, entry, PlistEntryType.DICT.getValueType(), mapEntry.getKey(), serialize(element)));
    }

    public static String serialize(Element element) {
        StringWriter writer = new StringWriter();
        try {
            transformer.transform(new DOMSource(element), new StreamResult(writer));
        } catch(TransformerException e) {
            log.error("An exception occurred trying to transform the DOM Element '{}' to XML", element, e);
        }
        return writer.toString();
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
            if(!defaultsWriteDict(plist, entry, mapEntry)) {
                log.warn("An error occurred writing '{}': '{}:{}' to {}", entry, mapEntry.getKey(), mapEntry.getValue(), plist);
                return false;
            }
        }
        return true;
    }

    public static boolean writeArray(Path plist, String entry, Collection<Object> values) {
        for(Object value : values) {
            if(!defaultsWriteArray(plist, entry, value)) {
                log.warn("An error occurred writing '{}': '{}' to {}", entry, value, plist);
                return false;
            }
        }
        return true;
    }

    public static Object getValue(Path plist, String entry) {
        return defaultsReadType(plist, entry).parseValue(defaultsRead(plist, entry));
    }

    public static Object[] getArray(Path plist, String entry) {
        Object o = parseMap(getRootNodeList(plist), entry).get(entry);
        if(o instanceof Object[]) {
            return (Object[])o;
        }
        return new Object[0];
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> getMap(Path plist, String entry) {
        HashMap<String, Object> o = parseMap(getRootNodeList(plist), entry);
        if(o.get(entry) instanceof HashMap) {
            return (HashMap<String,Object>)o.get(entry);
        }
        return new HashMap<>();
    }

    private static NodeList getRootNodeList(Path plist)  {
        try {
            doc = MacUtilities.createXmlDocument(plist);
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
        return parseMap(nodeList, null);
    }

    public static HashMap<String, Object> parseMap(NodeList nodeList, String entry) {
        HashMap<String, Object> map = new HashMap<>();
        if(nodeList == null || nodeList.getLength() == 0) {
            return map;
        }
        for(int i = 0; i < nodeList.getLength(); i++) {
            Node item = nodeList.item(i);
            if(item.getNodeName().equals("key")) {
                String key = item.getTextContent();
                if(entry != null && !key.equals(entry)) {
                    continue; // skip unrelated items
                }
                Node valueItem = item.getNextSibling();
                if(valueItem.getNodeName().equals("dict")) {
                    map.put(key, parseMap(valueItem.getChildNodes()));
                } else if(valueItem.getNodeName().equals("array")) {
                    map.put(key, parseArray(valueItem.getChildNodes()));
                } else {
                    map.put(key, PlistEntryType.parseObject(valueItem));
                }
            }
        }
        return map;
    }

    public static Object[] parseArray(NodeList nodeList) {
        List<Object> objectArray = new ArrayList<>();
        for(int i = 0; i < nodeList.getLength(); i++) {
            objectArray.add(PlistEntryType.parseObject(nodeList.item(i)));
        }
        return objectArray.toArray();
    }
}
