package qz.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.printer.action.*;

public class PrintingUtilities {

    private PrintingUtilities() {}

    public enum Type {
        HTML, IMAGE, PDF, RAW
    }

    public enum Format {
        BASE64, FILE, IMAGE, PLAIN, HEX, XML
    }


    public static PrintProcessor getPrintProcessor(JSONArray printData) throws JSONException {
        JSONObject data = printData.optJSONObject(0);

        Type type;
        if (data == null) {
            type = Type.RAW;
        } else {
            type = Type.valueOf(data.optString("type", "RAW").toUpperCase());
        }

        switch(type) {
            case HTML:
                return new PrintHTML();
            case IMAGE: default:
                return new PrintImage();
            case PDF:
                return new PrintPDF();
            case RAW:
                return new PrintRaw();
        }
    }

}
