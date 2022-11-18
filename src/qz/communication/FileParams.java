package qz.communication;

import org.apache.commons.ssl.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.ByteUtilities;
import qz.utils.FileUtilities;

import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * Created by Kyle on 2/28/2018.
 */
public class FileParams {
    private static final Logger log = LogManager.getLogger(FileParams.class);

    public enum Flavor {
        BASE64, FILE, HEX, PLAIN
    }

    private Path path;
    private String data;
    private Flavor flavor;

    private boolean shared;
    private boolean sandbox;

    private OpenOption appendMode;


    public FileParams(JSONObject params) throws JSONException {
        path = Paths.get(params.getString("path"));
        data = params.optString("data", "");
        flavor = Flavor.valueOf(params.optString("flavor", "PLAIN").toUpperCase(Locale.ENGLISH));

        shared = params.optBoolean("shared", true);
        sandbox = params.optBoolean("sandbox", true);

        appendMode = params.optBoolean("append")? StandardOpenOption.APPEND:StandardOpenOption.TRUNCATE_EXISTING;
    }

    public Path getPath() {
        return path;
    }

    public static String toString(Flavor flavor, byte[] bytes) {
        switch(flavor) {
            case BASE64:
                return Base64.encodeBase64String(bytes);
            case HEX:
                return ByteUtilities.bytesToHex(bytes);
            case FILE:
                log.warn("FileParams.toString(...) does not support {}, defaulting to {}", Flavor.FILE, Flavor.PLAIN);
            case PLAIN:
            default:
                return new String(bytes);
        }
    }

    public String toString(byte[] bytes) {
        return toString(flavor, bytes);
    }

    public byte[] getData() throws IOException {
        switch(flavor) {
            case BASE64:
                return Base64.decodeBase64(data);
            case FILE:
                return FileUtilities.readRawFile(data);
            case HEX:
                return ByteUtilities.hexStringToByteArray(data.trim());
            case PLAIN:
            default:
                return data.getBytes();
        }
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
