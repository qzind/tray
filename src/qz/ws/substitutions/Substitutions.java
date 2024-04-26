package qz.ws.substitutions;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.ArgValue;
import qz.utils.FileUtilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class Substitutions {
    protected static final Logger log = LogManager.getLogger(Substitutions.class);

    public static final String FILE_NAME = "substitutions.json";


    private static final Path DEFAULT_SUBSTITUTIONS_PATH = FileUtilities.SHARED_DIR.resolve(FILE_NAME);

    // Global toggle (should match ArgValue.SECURITY_SUBSTITUTIONS_ENABLE)
    private static boolean enabled = true;

    // Subkeys that are restricted for writing
    private static boolean restrictSubstitutions = true;
    private static HashMap<String, Type> restricted = new HashMap<>();
    static {
        restricted.put("copies", Type.OPTIONS);
        restricted.put("data", Type.DATA);
    }
    private ArrayList<JSONObject> matches, replaces;
    private ArrayList<Boolean> matchCase;
    private static Substitutions INSTANCE;

    public Substitutions(Path path) throws IOException, JSONException {
        this(new FileInputStream(path.toFile()));
    }

    public Substitutions(InputStream in) throws IOException, JSONException {
        this(IOUtils.toString(in, StandardCharsets.UTF_8));
    }

    public Substitutions(String serialized) throws JSONException {
        matches = new ArrayList<>();
        matchCase = new ArrayList<>();
        replaces = new ArrayList<>();

        JSONArray instructions = new JSONArray(serialized);
        for(int i = 0; i < instructions.length(); i++) {
            JSONObject step = instructions.optJSONObject(i);
            if(step != null) {
                JSONObject replace = step.optJSONObject("use");
                if(replace != null) {
                    sanitize(replace);
                    this.replaces.add(replace);
                }

                JSONObject match = step.optJSONObject("for");
                if(match != null) {
                    this.matchCase.add(match.optBoolean("caseSensitive", false));
                    match.remove("caseSensitive");
                    sanitize(match);
                    this.matches.add(match);
                }
            }
        }

        if(matches.size() != replaces.size()) {
            throw new SubstitutionException("Mismatched instructions; Each \"use\" must have a matching \"for\".");
        }
    }

    public JSONObject replace(InputStream in) throws IOException, JSONException {
        return replace(new JSONObject(IOUtils.toString(in, StandardCharsets.UTF_8)));
    }

    public JSONObject replace(JSONObject base) throws JSONException {
        for(int i = 0; i < matches.size(); i++) {
            if (find(base, matches.get(i), matchCase.get(i))) {
                log.debug("Matched {}JSON substitution rule: for: {}, use: {}",
                          matchCase.get(i) ? "case-sensitive " : "",
                          matches.get(i),
                          replaces.get(i));
                replace(base, replaces.get(i));
            } else {
                log.debug("Unable to match {}JSON substitution rule: for: {}, use: {}",
                          matchCase.get(i) ? "case-sensitive " : "",
                          matches.get(i),
                          replaces.get(i));
            }
        }
        return base;
    }

    public static boolean isPrimitive(Object o) {
        if(o instanceof JSONObject || o instanceof JSONArray) {
            return false;
        }
        return true;
    }

    public static void replace(JSONObject base, JSONObject replace) throws JSONException {
        JSONObject jsonBase = base.optJSONObject("params");
        JSONObject jsonReplace = replace.optJSONObject("params");
        if(jsonBase == null) {
            // skip, invalid base format for replacement
            return;
        }
        if(jsonReplace == null) {
            throw new SubstitutionException("Replacement JSON is missing \"params\": and is malformed");
        }

        // Second pass of sanitization before we replace
        for(Iterator it = jsonReplace.keys(); it.hasNext();) {
            Type type = Type.parse(it.next());
            if(type != null && !type.isReadOnly()) {
                // Good, let's make sure there are no exceptions
                if(restrictSubstitutions) {
                    switch(type) {
                        case DATA:
                            // Special handling for arrays
                            JSONArray jsonArray = jsonReplace.optJSONArray(type.getKey());
                            removeRestrictedSubkeys(jsonArray, type);
                            break;
                        default:
                            removeRestrictedSubkeys(jsonReplace, type);
                    }
                }
            }
        }
        find(base, replace, false, true);
    }

    private static void removeRestrictedSubkeys(JSONObject jsonObject, Type type) {
        if(jsonObject == null) {
            return;
        }
        for(Map.Entry<String, Type> entry : restricted.entrySet()) {
            if (type == entry.getValue()) {
                JSONObject toCheck = jsonObject.optJSONObject(type.getKey());
                if(toCheck != null && toCheck.has(entry.getKey())) {
                    log.warn("Use of { \"{}\": { \"{}\": ... } } is restricted, removing", type.getKey(), entry.getKey());
                    jsonObject.remove(entry.getKey());
                }
            }
        }
    }
    private static void removeRestrictedSubkeys(JSONArray jsonArray, Type type) {
        if(jsonArray == null) {
            return;
        }
        ArrayList<Object> toRemove = new ArrayList();
        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject;
            if ((jsonObject = jsonArray.optJSONObject(i)) != null) {
                for(Map.Entry<String, Type> entry : restricted.entrySet()) {
                    if (jsonObject.has(entry.getKey()) && type == entry.getValue()) {
                        log.warn("Use of { \"{}\": { \"{}\": ... } } is restricted, removing", type.getKey(), entry.getKey());
                        toRemove.add(jsonObject);
                    }
                }
            }
        }
        for(Object o : toRemove) {
            jsonArray.remove(o);
        }
    }

    public static void sanitize(JSONObject match) throws JSONException {
        // "options" ~= "config"
        Object cache;


        for(Type key : Type.values()) {
            // Sanitize alts/aliases
            for(String alt : key.getAlts()) {
                if ((cache = match.optJSONObject(alt)) != null) {
                    match.put(key.getKey(), cache);
                    match.remove(alt);
                    break;
                }
            }

            // Special handling for nesting of "printer", "options", "data" within "params"
            if((cache = match.opt(key.getKey())) != null) {
                JSONObject nested = new JSONObject();
                switch(key) {
                    case PRINTER:
                        JSONObject name = new JSONObject();
                        name.put("name", cache);
                        nested.put(key.getKey(), name);
                        break;
                    default:
                        nested.put(key.getKey(), cache);
                }

                match.put("params", nested);
                match.remove(key.getKey());
            }
        }

        // Special handling for "data" being provided as an object instead of an array
        if((cache = match.opt("params")) != null) {
            if (cache instanceof JSONObject) {
                JSONObject params = (JSONObject)cache;
                if((cache = params.opt("data")) != null) {
                    if (cache instanceof JSONArray) {
                        // correct
                    } else {
                        JSONArray wrapped = new JSONArray();
                        wrapped.put(cache);
                        params.put("data", wrapped);
                    }
                }
            }
        }
    }

    private static boolean find(Object base, Object match, boolean caseSensitive) throws JSONException {
        return find(base, match, caseSensitive, false);
    }
    private static boolean find(Object base, Object match, boolean caseSensitive, boolean replace) throws JSONException {
        if(base instanceof JSONObject) {
            if(match instanceof JSONObject) {
                JSONObject jsonMatch = (JSONObject)match;
                JSONObject jsonBase = (JSONObject)base;
                for(Iterator it = jsonMatch.keys(); it.hasNext(); ) {
                    Object next = it.next();
                    Object newMatch = jsonMatch.get(next.toString());

                    // Check if the key exists, recurse if needed
                    if(jsonBase.has(next.toString())) {
                        Object newBase = jsonBase.get(next.toString());

                        if(replace && isPrimitive(newMatch)) {
                            // Overwrite value, don't recurse
                            jsonBase.put(next.toString(), newMatch);
                            continue;
                        } else if(find(newBase, newMatch, caseSensitive, replace)) {
                            continue;
                        }
                    } else if(replace) {
                        // Key doesn't exist, so we'll merge it in
                        jsonBase.put(next.toString(), newMatch);
                    }
                    return false; // wasn't found
                }
                return true; // assume found
            } else {
                return false; // mismatched types
            }
        } else if (base instanceof JSONArray) {
            if(match instanceof JSONArray) {
                JSONArray matchArray = (JSONArray)match;
                JSONArray baseArray = (JSONArray)base;
                match:
                for(int i = 0; i < matchArray.length(); i++) {
                    Object newMatch = matchArray.get(i);
                    for(int j = 0; j < baseArray.length(); j++) {
                        Object newBase = baseArray.get(j);
                        if(find(newBase, newMatch, caseSensitive, replace)) {
                            continue match;
                        }
                    }
                    return false;
                }
                return true; // assume found
            } else {
                return false;
            }
        } else {
            // Treat as primitives
            if(!match.getClass().getName().equals("java.lang.String") && match.getClass().equals(base.getClass())) {
                // Same type
                return match.equals(base);
            } else {
                // Dissimilar types (e.g. "width": "8.5" versus "width": 8.5), cast both to String
                if(caseSensitive) {
                    return match.toString().equals(base.toString());
                }
                return match.toString().equalsIgnoreCase(base.toString());
            }
        }
    }

    public static void setEnabled(boolean enabled) {
        Substitutions.enabled = enabled;
    }

    public static void setRestrictSubstitutions(boolean restrictSubstitutions) {
        Substitutions.restrictSubstitutions = restrictSubstitutions;
    }

    /**
     * Returns a new instance of the <code>Substitutions</code> object from the default
     * <code>substitutions.json</code> file at <code>DEFAULT_SUBSTITUTIONS_PATH</code>,
     * or <code>null</code> if an error occurred.
     */
    public static Substitutions newInstance() {
        return newInstance(DEFAULT_SUBSTITUTIONS_PATH);
    }

    /**
     * Returns a new instance of the <code>Substitutions</code> object from the provided
     * <code>json</code> substitutions file, or <code>null</code> if an error occurred.
     */
    public static Substitutions newInstance(Path path) {
        Substitutions substitutions = null;
        try {
            substitutions = new Substitutions(path);
            log.info("Successfully parsed new substitutions file.");
        } catch(JSONException e) {
            log.warn("Unable to parse substitutions file, skipping", e);
        } catch(IOException e) {
            log.info("Substitutions file missing, skipping: {}", e.getMessage());
        }
        return substitutions;
    }

    public static Substitutions getInstance() {
        return getInstance(false);
    }

    public static Substitutions getInstance(boolean forceRefresh) {
        if(INSTANCE == null || forceRefresh) {
            INSTANCE = Substitutions.newInstance();
            if(!enabled) {
                log.warn("Substitution file was found, but substitutions are currently disabled via \"{}=false\"", ArgValue.SECURITY_SUBSTITUTIONS_ENABLE.getMatch());
            }
        }
        return INSTANCE;
    }

    public static boolean areActive() {
        return Substitutions.getInstance() != null && enabled;
    }
}