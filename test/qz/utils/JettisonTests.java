package qz.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import static qz.installer.apps.policy.installer.LinuxPolicyInstaller.*;

/**
 * Jettison's API has changed over the years
 * This is an attempt to capture those API changes so we can eventually upgrade
 */
public class JettisonTests {
    private static final Logger log = LogManager.getLogger(JettisonTests.class);

    static boolean VERSION_1_3_4_OR_LOWER;
    static Object EXPLICIT_NULL;

    static {
        VERSION_1_3_4_OR_LOWER = false;
        try {
            // Null.isExplicitNull() was added in 1.3.5
            JSONObject.NULL.getClass().getDeclaredMethod("isExplicitNull");
            EXPLICIT_NULL = JSONObject.class.getDeclaredField("EXPLICIT_NULL").get(null);
        } catch(IllegalAccessException | NoSuchMethodException | NoSuchFieldException e) {
            VERSION_1_3_4_OR_LOWER = true;
            EXPLICIT_NULL = "ERROR: version too old for this field";
        }
        log.info("Jettison version is 1.3.4 or lower: {}", VERSION_1_3_4_OR_LOWER);
    }

    @Test
    public static void NullTest() throws JSONException {
        JSONObject nullObject = new JSONObject();
        nullObject.put("key", (String)null);
        assert nullObject.opt("key") == null;
    }

    @Test
    @SuppressWarnings("SimplifiableAssertion")
    public static void NullObjectTests() throws JSONException {
        //
        // Explicit null handling
        //

        // Parsing a null in newer Jettison versions will use JSONObject.NULL
        JSONObject explicitNull = new JSONObject("{ \"key\": null }");

        // All Jettison versions: isNull() should always work
        Assert.assertTrue(explicitNull.isNull("key"));

        if(VERSION_1_3_4_OR_LOWER) {
            // Legacy Jettison: JSObject.NULL was used (EXPLICIT_NULL didn't exist yet)
            Assert.assertEquals(explicitNull.opt("key"), JSONObject.NULL);
        } else {
            // Modern Jettison: Explicit nulls use JSObject.EXPLICIT_NULL
            Assert.assertEquals(explicitNull.opt("key"), EXPLICIT_NULL);
        }

        //
        // Implicit null handling
        //

        JSONObject implicitNull = new JSONObject().put("key", (String)null);

        // All Jettison versions - Implicit null will behave the same with both versions
        Assert.assertNull(implicitNull.opt("key"));

        // Same test as above, but iron out any assertNull() trust issues
        Assert.assertTrue(implicitNull.isNull("key"));

        // All Jettison versions - Can use NULL to determine equality as it checks both "== null" and "== this".
        Assert.assertTrue(JSONObject.NULL.equals(implicitNull.opt("key")));
    }

    @Test
    public static void FloatTest() throws JSONException {
        JSONObject doubleObject = new JSONObject();
        doubleObject.put("key", 1.2f);
        Assert.assertEquals(normalizeFloat(doubleObject.optDouble("key")), 1.2f);
    }

    @Test
    public static void StringTests() throws JSONException {
        JSONObject stringObject = new JSONObject();

        // ascii
        stringObject.put("key", "value");
        Assert.assertEquals(stringObject.opt("key"), "value");

        // unicode
        stringObject.put("key", "🖨️");
        Assert.assertEquals(stringObject.opt("key"), "\uD83D\uDDA8\uFE0F");
    }

    @Test
    public static void ArrayTests() {
        JSONArray jsonArray = new JSONArray();

        jsonArray.put("hello");
        jsonArray.put("world");

        jsonArray.remove(0);

        // Jettison 1.5.5 and older remove is broken
        Assert.assertNotEquals(jsonArray.length(), 1);

        jsonArray.remove("hello");
        Assert.assertEquals(jsonArray.length(), 1);
    }

    @Test
    public static void OptTests() throws JSONException {
        JSONObject keyValue = new JSONObject("{ \"key\": \"value\" }");
        Assert.assertEquals(keyValue.optString("key", "fallback"), "value");

        JSONObject explicitNull = new JSONObject("{ \"key\": null }");
        // FIXME:  This is NOT how opt should work!  https://github.com/jettison-json/jettison/issues/105
        if(VERSION_1_3_4_OR_LOWER) {
            Assert.assertEquals(explicitNull.optString("key", "fallback"), "null");
        } else {
            // This only works because JSONObject.NULL has handling for this
            Assert.assertEquals(explicitNull.optString("key", "fallback"), null);
        }

        JSONObject emptyFallback = new JSONObject();
        Assert.assertEquals(emptyFallback.optString("key", "fallback"), "fallback");
    }
}
