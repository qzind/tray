package qz.ws.substitutions;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.ArgValue;
import qz.utils.ByteUtilities;
import qz.utils.FileUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Substitutions {
    protected static final Logger log = LogManager.getLogger(Substitutions.class);

    public static final String FILE_NAME = "substitutions.json";


    private static final Path DEFAULT_SUBSTITUTIONS_PATH = FileUtilities.SHARED_DIR.resolve(FILE_NAME);

    // Global toggle (should match ArgValue.SECURITY_SUBSTITUTIONS_ENABLE)
    private static boolean enabled = true;

    // Subkeys that are restricted for writing because they can materially impact the content
    private static boolean strict = true;
    private static final HashMap<Type, String[]> parlous = new HashMap<>();
    static {
        parlous.put(Type.OPTIONS, new String[] {"copies"});
        parlous.put(Type.DATA, new String[] {"data"});
    }
    private final ArrayList<Rule> rules;
    private static Substitutions INSTANCE;

    public Substitutions(Path path) throws IOException, JSONException {
        this(Files.newInputStream(path.toFile().toPath()));
    }

    public Substitutions(InputStream in) throws IOException, JSONException {
        this(IOUtils.toString(in, StandardCharsets.UTF_8));
    }

    public Substitutions(String serialized) throws JSONException {
        rules = new ArrayList<>();

        JSONArray instructions = new JSONArray(serialized);
        for(int i = 0; i < instructions.length(); i++) {
            JSONObject step = instructions.optJSONObject(i);
            if(step != null) {
                rules.add(new Rule(step));
            }
        }
    }

    public JSONObject replace(InputStream in) throws IOException, JSONException {
        return replace(new JSONObject(IOUtils.toString(in, StandardCharsets.UTF_8)));
    }

    public JSONObject replace(JSONObject base) throws JSONException {
        for(Rule rule : rules) {
            if (find(base, rule.match, rule.caseSensitive)) {
                log.debug("Matched {}JSON substitution rule: {}", rule.caseSensitive ? "case-sensitive " : "", rule);
                replace(base, rule.replace);
            } else {
                log.debug("Unable to match {}JSON substitution rule: {}", rule.caseSensitive ? "case-sensitive " : "", rule);
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

        if (strict) {
            // Second pass of sanitization before we replace
            for(Iterator it = jsonReplace.keys(); it.hasNext(); ) {
                Type type = Type.parse(it.next());
                if(type == null || type.isReadOnly()) continue;
                // Good, let's make sure there are no exceptions
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
        find(base, replace, false, true);
    }

    private static void removeRestrictedSubkeys(JSONObject jsonObject, Type type) {
        if(jsonObject == null) {
            return;
        }

        String[] parlousFieldNames = parlous.get(type);
        if(parlousFieldNames == null) return;

        for (String parlousFieldName : parlousFieldNames) {
            JSONObject toCheck = jsonObject.optJSONObject(type.getKey());
            if (toCheck != null && toCheck.has(parlousFieldName)) {
                log.warn("Use of { \"{}\": { \"{}\": ... } } is restricted, removing", type.getKey(), parlousFieldName);
                jsonObject.remove(parlousFieldName);
            }
        }
    }

    private static void removeRestrictedSubkeys(JSONArray jsonArray, Type type) {
        if(jsonArray == null) {
            return;
        }

        ArrayList<Object> toRemove = new ArrayList<>();
        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject;
            if ((jsonObject = jsonArray.optJSONObject(i)) != null) {
                String[] parlousFieldNames = parlous.get(type);
                for (String parlousFieldName : parlousFieldNames) {
                    if (jsonObject.has(parlousFieldName)) {
                        log.warn("Use of { \"{}\": { \"{}\": ... } } is restricted, removing", type.getKey(), parlousFieldName);
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

        JSONObject nested = new JSONObject();
        for(Type key : Type.values()) {
            // If any alts/aliases key are used, switch them out for the standard key
            for(String alt : key.getAlts()) {
                if ((cache = match.optJSONObject(alt)) != null) {
                    match.put(key.getKey(), cache);
                    match.remove(alt);
                    break;
                }
            }

            // Special handling for nesting of "printer", "options", "data" within "params"
            if((cache = match.opt(key.getKey())) != null) {
                switch(key) {
                    case PRINTER:
                        JSONObject name = new JSONObject();
                        name.put("name", cache);
                        nested.put(key.getKey(), name);
                        break;
                    default:
                        nested.put(key.getKey(), cache);
                }
                match.remove(key.getKey());
            }
        }
        if(nested.length() > 0) {
            match.put("params", nested);
        }

        // Special handling for "data" being provided as an object instead of an array
        if((cache = match.opt("params")) != null) {
            if (cache instanceof JSONObject) {
                JSONObject params = (JSONObject)cache;
                if((cache = params.opt("data")) != null) {
                    if (cache instanceof JSONArray) {
                        // If "data" is already an array, skip
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
                    String nextKey = it.next().toString();
                    Object newMatch = jsonMatch.get(nextKey);

                    // Check if the key exists, recurse if needed
                    if(jsonBase.has(nextKey) && !jsonBase.isNull(nextKey)) {
                        Object newBase = jsonBase.get(nextKey);

                        if(replace && isPrimitive(newMatch)) {
                            // Overwrite value, don't recurse
                            jsonBase.put(nextKey, newMatch);
                            continue;
                        } else if(find(newBase, newMatch, caseSensitive, replace)) {
                            continue;
                        }
                    } else if(replace) {
                        // Key doesn't exist, or it's null so we'll merge it in
                        jsonBase.put(nextKey, newMatch);
                        continue;
                    }
                    return false; // wasn't found
                }
                return true; // assume found
            } else {
                return false; // mismatched types
            }
        } else if (base instanceof JSONArray) {
            boolean found = false;
            if(match instanceof JSONArray) {
                JSONArray matchArray = (JSONArray)match;
                JSONArray baseArray = (JSONArray)base;
                for(int i = 0; i < matchArray.length(); i++) {
                    Object newMatch = matchArray.get(i);
                    for(int j = 0; j < baseArray.length(); j++) {
                        Object newBase = baseArray.get(j);
                        if(find(newBase, newMatch, caseSensitive, replace)) {
                            found = true;
                            if(!replace) {
                                return true;
                            }
                        }
                    }
                }
            }
            return found;
        } else {
            // Treat as primitives
            if(match instanceof Number || base instanceof Number) {
                return ByteUtilities.numberEquals(match, base);
            }
            // Fallback: Cast both to String
            if(caseSensitive) {
                return match.toString().equals(base.toString());
            }
            return match.toString().equalsIgnoreCase(base.toString());
        }
    }

    public static void setEnabled(boolean enabled) {
        Substitutions.enabled = enabled;
    }

    public static void setStrict(boolean strict) {
        Substitutions.strict = strict;
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

    private class Rule {
        private boolean caseSensitive;
        private JSONObject match, replace;

        Rule(JSONObject json) throws JSONException {
            JSONObject replaceJSON = json.optJSONObject("use");
            if(replaceJSON != null) {
                sanitize(replaceJSON);
                replace = replaceJSON;
            }

            JSONObject matchJSON = json.optJSONObject("for");
            if(matchJSON != null) {
                caseSensitive = matchJSON.optBoolean("caseSensitive", false);
                matchJSON.remove("caseSensitive");
                sanitize(matchJSON);
                match = matchJSON;
            }

            if(match == null || replace == null) {
                throw new SubstitutionException("Mismatched instructions; Each \"use\" must have a matching \"for\".");
            }
        }

        @Override
        public String toString() {
            return  "for: " + match + ", use: " + replace;
        }
    }
}
