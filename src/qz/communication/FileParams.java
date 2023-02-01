package qz.communication;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.ByteUtilities;
import qz.utils.PrintingUtilities.Flavor;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Created by Kyle on 2/28/2018.
 */
public class FileParams {
    private Path path;
    private String data;
    private Flavor flavor;

    private boolean shared;
    private boolean sandbox;

    private OpenOption appendMode;


    public FileParams(JSONObject params) throws JSONException {
        path = Paths.get(params.getString("path"));
        data = params.optString("data", "");
        flavor = Flavor.parse(params, Flavor.PLAIN);

        shared = params.optBoolean("shared", true);
        sandbox = params.optBoolean("sandbox", true);

        appendMode = params.optBoolean("append")? StandardOpenOption.APPEND:StandardOpenOption.TRUNCATE_EXISTING;
    }

    public Path getPath() {
        return path;
    }

    public String toString(byte[] bytes) {
        return ByteUtilities.toString(flavor, bytes);
    }

    public byte[] getData() throws IOException {
        return flavor.read(data);
    }

    public Flavor getFlavor() {
        return flavor;
    }

    public boolean isShared() {
        return shared;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public OpenOption getAppendMode() {
        return appendMode;
    }

}
