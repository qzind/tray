package qz.ws.substitutions;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;

public class SubstitutionsTests {
    public static void main(String ... args) throws JSONException, IOException {
        Substitutions substitutions = new Substitutions(
                SubstitutionsTests.class.getResourceAsStream("resources/substitutions.json")
        );
        JSONObject base = substitutions.replace(
                SubstitutionsTests.class.getResourceAsStream("resources/printRequest.json")
        );

        System.out.println(base);
    }
}
