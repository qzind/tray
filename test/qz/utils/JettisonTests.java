package qz.utils;

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
    @Test
    public static void NullTest() throws JSONException {
        JSONObject nullObject = new JSONObject();
        nullObject.put("key", (String)null);
        assert nullObject.opt("key") == null;
    }

    @Test
    @SuppressWarnings("SimplifiableAssertion")
    public static void NullObjectTests() throws JSONException {
        JSONObject nullObject = new JSONObject();
        nullObject.put("key", (String)null);

        // Jettison's 1.3.3 and older
        Assert.assertNull(nullObject.opt("key"));
        Assert.assertNotEquals(nullObject.opt("key"), JSONObject.NULL);

        // All Jettison versions
        Assert.assertTrue(JSONObject.NULL.equals(nullObject.opt("key")));
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
        JSONObject text1 = new JSONObject("{ \"key\": \"value\" }");
        Assert.assertEquals(text1.optString("key", "fallback"), "value");

        // weird, but that's how it's supposed to work
        JSONObject text2 = new JSONObject("{ \"key\": null }");
        Assert.assertEquals(text2.optString("key", "fallback"), "null");

        JSONObject text3 = new JSONObject();
        Assert.assertEquals(text3.optString("key", "fallback"), "fallback");
    }
}
