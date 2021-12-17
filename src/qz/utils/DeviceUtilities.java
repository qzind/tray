package qz.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Utilities used by both Serial and USB/HID
 */
public class DeviceUtilities {

    private static final Logger log = LogManager.getLogger(DeviceUtilities.class);


    public enum DataType {
        PLAIN, FILE, HEX, BASE64
    }


    public static DataType getDataType(JSONObject data) {
        if (data != null && !data.isNull("type")) {
            try {
                return DataType.valueOf(data.getString("type"));
            }
            catch(JSONException e) {
                log.warn("Cannot read {} as a value for data type, using default", data.opt("type"));
            }
        }

        return DataType.PLAIN;
    }

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

        switch(getDataType(metadata)) {
            case PLAIN:
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
            case FILE:
                bytesToSend = IOUtils.toByteArray(new URL(metadata.getString("data")));
                break;
            case HEX:
                bytesToSend = ByteUtilities.hexStringToByteArray(metadata.getString("data"));
                break;
            case BASE64:
                bytesToSend = Base64.decodeBase64(metadata.getString("data"));
                break;
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
