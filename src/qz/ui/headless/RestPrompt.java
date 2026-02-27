package qz.ui.headless;

import org.codehaus.jettison.json.JSONObject;

public class RestPrompt implements Endpoint.Promptable {
    @Override
    public JSONObject prompt(Endpoint endpoint, JSONObject data) {
        throw new UnsupportedOperationException("Not yet supported");
    }
}
