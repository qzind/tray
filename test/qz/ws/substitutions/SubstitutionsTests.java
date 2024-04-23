package qz.ws.substitutions;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class SubstitutionsTests {
    public static String[] JSON_TEST_MATCH = new String[4];
    public static String[] JSON_TEST_REPLACE = new String[4];
    static {
        JSON_TEST_MATCH[0] = "{\n" +
                "    \"config\": {\n" +
                "        \"size\": {\n" +
                "            \"width\": \"4\",\n" +
                "            \"height\": \"6\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        JSON_TEST_MATCH[1] = "{\n" +
                "    \"printer\": \"PDFwriter\"\n" +
                "}";
        JSON_TEST_MATCH[2] = "{\n" +
                "    \"data\": {\n" +
                "            \"options\": {\n" +
                "              \"pageWidth\": \"8.5\",\n" +
                "              \"pageHeight\": \"11\"\n" +
                "            }\n" +
                "    }\n" +
                "}";

        JSON_TEST_MATCH[3] = "{\n" +
                "    \"config\": {\n" +
                "        \"copies\": 1" +
                "    }" +
                "}";

        JSON_TEST_REPLACE[0] = "{\n" +
                "    \"config\": {\n" +
                "        \"size\": {\n" +
                "            \"width\": \"100\",\n" +
                "            \"height\": \"150\"\n" +
                "        },\n" +
                "        \"units\": \"mm\"\n" +
                "    }\n" +
                "}";
        JSON_TEST_REPLACE[1] = "{\n" +
                "    \"printer\": \"XPS Document Writer\"\n" +
                "}";
        JSON_TEST_REPLACE[2] = "{\n" +
                "    \"data\": {\n" +
                "            \"options\": {\n" +
                "              \"pageWidth\": \"8.5\",\n" +
                "              \"pageHeight\": \"14\"\n" +
                "            }\n" +
                "    }\n" +
                "}";

        JSON_TEST_REPLACE[3] = "{\n" +
                "    \"config\": {\n" +
                "        \"copies\": 3" +
                "    }" +
                "}";
    }

    public static final String JSON_TEST_BASE = "{\n" +
            "  \"call\": \"print\",\n" +
            "  \"params\": {\n" +
            "    \"printer\": {\n" +
            "      \"name\": \"PDFwriter\"\n" +
            "    },\n" +
            "    \"options\": {\n" +
            "      \"bounds\": null,\n" +
            "      \"colorType\": \"color\",\n" +
            "      \"copies\": 1,\n" +
            "      \"density\": 0,\n" +
            "      \"duplex\": false,\n" +
            "      \"fallbackDensity\": null,\n" +
            "      \"interpolation\": \"bicubic\",\n" +
            "      \"jobName\": null,\n" +
            "      \"legacy\": false,\n" +
            "      \"margins\": 0,\n" +
            "      \"orientation\": null,\n" +
            "      \"paperThickness\": null,\n" +
            "      \"printerTray\": null,\n" +
            "      \"rasterize\": false,\n" +
            "      \"rotation\": 0,\n" +
            "      \"scaleContent\": true,\n" +
            "      \"size\": {\n" +
            "        \"width\": \"4\",\n" +
            "        \"height\": \"6\"\n" +
            "      },\n" +
            "      \"units\": \"in\",\n" +
            "      \"forceRaw\": false,\n" +
            "      \"encoding\": null,\n" +
            "      \"spool\": {}\n" +
            "    },\n" +
            "    \"data\": [\n" +
            "      {\n" +
            "        \"type\": \"pixel\",\n" +
            "        \"format\": \"pdf\",\n" +
            "        \"flavor\": \"file\",\n" +
            "        \"data\": \"https://demo.qz.io/assets/pdf_sample.pdf\",\n" +
            "        \"options\": {\n" +
            "          \"pageWidth\": \"8.5\",\n" +
            "          \"pageHeight\": \"11\",\n" +
            "          \"pageRanges\": \"\",\n" +
            "          \"ignoreTransparency\": false,\n" +
            "          \"altFontRendering\": false\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"signature\": \"\",\n" +
            "  \"timestamp\": 1713895560783,\n" +
            "  \"uid\": \"64t63d\",\n" +
            "  \"position\": {\n" +
            "    \"x\": 720,\n" +
            "    \"y\": 462.5\n" +
            "  },\n" +
            "  \"signAlgorithm\": \"SHA512\"\n" +
            "}";

    public static void main(String ... args) throws JSONException {
        JSONArray instructions = new JSONArray();

        /** TODO: Move the tests to a flat file **/
        JSONObject step1 = new JSONObject();
        step1.put("use", new JSONObject(JSON_TEST_REPLACE[0]));
        step1.put("for", new JSONObject(JSON_TEST_MATCH[0]));

        JSONObject step2 = new JSONObject();
        step2.put("use", new JSONObject(JSON_TEST_REPLACE[1]));
        step2.put("for", new JSONObject(JSON_TEST_MATCH[1]));

        JSONObject step3 = new JSONObject();
        step3.put("use", new JSONObject(JSON_TEST_REPLACE[2]));
        step3.put("for", new JSONObject(JSON_TEST_MATCH[2]));

        // TODO: Why doesn't this test fail on "options" with subkey of "copies"
        JSONObject step4 = new JSONObject();
        step4.put("use", new JSONObject(JSON_TEST_REPLACE[3]));
        step4.put("for", new JSONObject(JSON_TEST_MATCH[3]));

        instructions.put(step1).put(step2).put(step3).put(step4);
        Substitutions substitutions = new Substitutions(instructions.toString());

        JSONObject base = new JSONObject(JSON_TEST_BASE);
        substitutions.replace(base);

        System.out.println(base);
    }
}
