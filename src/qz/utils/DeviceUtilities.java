package qz.utils;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Utilities used by both Serial and USB/HID
 */
public class DeviceUtilities {

    private static final Logger log = LogManager.getLogger(DeviceUtilities.class);

    /**
     * Pull data from a json object and convert it to bytes based on type
     */
    public static byte[] getDataBytes(JSONObject params, Charset charset) throws JSONException, IOException {
        if (charset == null) { charset = StandardCharsets.UTF_8; }

        byte[] bytesToSend = null;

        JSONObject metadata = params.optJSONObject("data");
        if (metadata == null) {
            metadata = new JSONObject();
            metadata.put("data", params.get("data"));
            metadata.put("type", "PLAIN");
        }

        // Flavor is called "type" in this API
        PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.parse(metadata.optString("type"), PrintingUtilities.Flavor.PLAIN);

        switch(flavor) {
            case PLAIN:
                // Special handling for raw bytes
                if (metadata.optJSONArray("data") == null) {
                    bytesToSend = characterBytes(metadata.getString("data"), charset);
                } else {
                    JSONArray fromSource = metadata.getJSONArray("data");
                    bytesToSend = new byte[fromSource.length()];
                    for(int i = 0; i < fromSource.length(); i++) {
                        bytesToSend[i] = (byte)fromSource.getInt(i);
                    }
                }
                break;
            default:
                bytesToSend = flavor.read(metadata.getString("data"));
        }

        return bytesToSend;
    }


    /**
     * Turn a string into a character byte array.
     * First attempting to take the entire string as a character literal (for non-printable unicode).
     */
    public static byte[] characterBytes(String convert, Charset charset) {
        if (convert.length() > 2) {
            try {
                //try to interpret entire string as single char representation (such as "\u0000" or "0xFFFF")
                char literal = (char)Integer.parseInt(convert.substring(2), 16);
                return StringUtils.getBytesUtf8(String.valueOf(literal));
            }
            catch(NumberFormatException ignore) {}
        }

        //try escaping string using Apache (to get strings like "\r" as characters)
        return StringEscapeUtils.unescapeJava(convert).getBytes(charset);
    }

}
