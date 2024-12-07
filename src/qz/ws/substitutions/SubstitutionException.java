package qz.ws.substitutions;

import org.codehaus.jettison.json.JSONException;

public class SubstitutionException extends JSONException {
    public SubstitutionException(String message) {
        super(message);
    }
}
