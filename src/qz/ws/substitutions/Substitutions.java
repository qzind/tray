package qz.ws.substitutions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
public class Substitutions {
    protected static final Logger log = LogManager.getLogger(Substitutions.class);
    private static boolean restrictSubstitutions = true;
    private ArrayList<JSONObject> matches;
    private ArrayList<JSONObject> replaces;

    private static class SubstitutionException extends JSONException {
        public SubstitutionException(String message) {
            super(message);
        }
    }

    public Substitutions(String serialized) throws JSONException {
        matches = new ArrayList<>();
        replaces = new ArrayList<>();

        JSONArray instructions = new JSONArray(serialized);
        System.out.println(serialized);
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
                    sanitize(match);
                    this.matches.add(match);
                }
            }
        }
        if(matches.size() != replaces.size()) {
            throw new SubstitutionException("Mismatched instructions; Each \"use\" must have a matching \"for\".");
        }
    }

    public void replace(JSONObject base) throws JSONException {
        for(int i = 0; i < matches.size(); i++) {
            if (find(base, matches.get(i))) {
                System.out.println(" [YES MATCH]");
                replace(base, replaces.get(i));
            } else {
                System.out.println(" [NO MATCH]");
            }
        }
    }

    public static boolean isPrimitive(Object o) {
        if(o instanceof JSONObject || o instanceof JSONArray) {
            return false;
        }
        return true;
    }

    public static void replace(JSONObject base, JSONObject replace) throws JSONException {
        JSONObject baseParams = base.optJSONObject("params");
        JSONObject replaceParams = replace.optJSONObject("params");
        if(baseParams == null) {
            // skip, invalid base format for replacement
            return;
        }
        if(replaceParams == null) {
            throw new SubstitutionException("Replacement JSON is missing \"params\": and is malformed");
        }

        // Second pass of sanitization before we replace
        for(Iterator it = replaceParams.keys(); it.hasNext();) {
            RootKey root = RootKey.parse(it.next());
            if(root != null && root.isReplaceAllowed()) {
                // Good, let's make sure there are no exceptions
                if(restrictSubstitutions) {
                    switch(root) {
                        // Special handling for arrays
                        case DATA:
                            JSONArray data = (JSONArray)replaceParams.get(root.getKey());
                            ArrayList<Object> toRemove = new ArrayList();
                            for(int i = 0; i < data.length(); i++) {
                                JSONObject jsonObject;
                                if ((jsonObject = data.optJSONObject(i)) != null) {
                                    for(RestrictedKey restricted : root.getRestrictedSubkeys()) {
                                        if (jsonObject.has(restricted.getSubkey())) {
                                            log.warn("Use of {}: [{}:] is restricted, removing", root.getKey(), restricted.getSubkey());
                                            toRemove.add(jsonObject);
                                        }
                                    }
                                }
                            }
                            for(Object o : toRemove) {
                                data.remove(o);
                            }
                            break;
                        default:
                            for(RestrictedKey restricted : root.getRestrictedSubkeys()) {
                                if (replaceParams.has(restricted.getSubkey())) {
                                    log.warn("Use of {}: [{}:] is restricted, removing", root.getKey(), restricted.getSubkey());
                                    replaceParams.remove(restricted.getSubkey());
                                }
                            }
                    }
                }
            }
        }
        find(base, replace, true);
    }

    public static void sanitize(JSONObject match) throws JSONException {
        // "options" ~= "config"
        System.out.println("BEFORE: " + match);
        Object cache;


        for(RootKey key : RootKey.values()) {
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

        System.out.println("AFTER:  " + match);
    }

    private static boolean find(Object base, Object match) throws JSONException {
        return find(base, match, false);
    }
    private static boolean find(Object base, Object match, boolean replace) throws JSONException {
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
                        } else if(find(newBase, newMatch, replace)) {
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
                        if(find(newBase, newMatch, replace)) {
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
            return match.equals(base);
        }
    }
}
