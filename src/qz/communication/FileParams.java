package qz.communication;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Kyle on 2/28/2018.
 */
public class FileParams {
    public final boolean shared, sandbox;
    public final Path originalPath;

    FileParams(Path originalPath, boolean shared, boolean sandbox) {
        this.originalPath = originalPath;
        this.shared = shared;
        this.sandbox = sandbox;
    }

    public static FileParams fromJSON(JSONObject json) throws JSONException {
        return new FileParams(Paths.get(json.getString("path")),
                              json.optBoolean("shared"),
                              json.optBoolean("sandbox")
        );
    }
}
