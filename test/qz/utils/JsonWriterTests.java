package qz.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class JsonWriterTests {

    private static final Logger log = LogManager.getLogger(JsonWriterTests.class);

    private static File JSON_FILE;

    @BeforeClass
    public static void setupJsonFile() throws IOException {
        JSON_FILE = File.createTempFile("json-test-file", ".json");
        FileUtilities.configureAssetToFile(JsonWriterTests.class, "resources/json-test-file.json", new HashMap<>(), JSON_FILE);
    }

    @Test(priority = 1)
    public static void baselineTest() {
        Assert.assertTrue(JSON_FILE != null && JSON_FILE.exists() && JSON_FILE.canWrite());

        try {
            // Read a value we know already exists in the JSON file
            Assert.assertEquals(readJsonFile()
                                        .getJSONObject("policies")
                                        .getJSONObject("Certificates")
                                        .getJSONArray("Install")
                                        .get(0), "cert1.der");
        } catch(Exception e) {
            Assert.fail("Baseline test failed", e);
        }
    }


    @Test(priority = 2)
    public static void addValue() {
        try {
            // Add
            JSONObject importEnterpriseRootsTrue = new JSONObject().put(
                    "policies", new JSONObject().put(
                            "Certificates", new JSONObject().put(
                                    "ImportEnterpriseRoots", true)));

            JsonWriter.write(JSON_FILE.getPath(), importEnterpriseRootsTrue.toString(), true, false);
            Assert.assertTrue(readJsonFile()
                                        .getJSONObject("policies")
                                        .getJSONObject("Certificates")
                                        .getBoolean("ImportEnterpriseRoots"));
        } catch(Exception e) {
            Assert.fail("Unexpected error occurred while testing", e);
        }
    }

    @Test(priority = 3)
    public static void updateValue() {
        try {
            // Change
            JSONObject importEnterpriseRootsFalse = new JSONObject().put(
                    "policies", new JSONObject().put(
                            "Certificates", new JSONObject().put(
                                    "ImportEnterpriseRoots", false)));

            JsonWriter.write(JSON_FILE.getPath(), importEnterpriseRootsFalse.toString(), true, false);
            Assert.assertFalse(readJsonFile()
                                       .getJSONObject("policies")
                                       .getJSONObject("Certificates")
                                       .getBoolean("ImportEnterpriseRoots"));
        } catch(Exception e) {
            Assert.fail("Unexpected error occurred while testing", e);
        }
    }

    @Test(priority = 4)
    public static void deleteValue() {
        try {
            JSONObject importEnterpriseRootsDelete = new JSONObject().put(
                    "policies", new JSONObject().put(
                            "Certificates", new JSONObject().put(
                                    "ImportEnterpriseRoots", true)));
            // Delete
            JsonWriter.write(JSON_FILE.getPath(), importEnterpriseRootsDelete.toString(), true, true);
            Assert.assertNull(readJsonFile()
                                      .getJSONObject("policies")
                                      .getJSONObject("Certificates")
                                      .opt("ImportEnterpriseRoots"));
        } catch(Exception e) {
            Assert.fail("Unexpected error occurred while testing", e);
        }
    }

    @Test(priority = 5)
    public static void addArray() {
        try {
            JSONObject certificatesInstall = new JSONObject().put(
                    "policies", new JSONObject().put(
                            "Certificates", new JSONObject().put(
                                    "Install", new JSONArray().put(
                                            "cert2.der"))));
            // Add
            JsonWriter.write(JSON_FILE.getPath(), certificatesInstall.toString(), false, false);

            JSONArray installJson = readJsonFile()
                    .getJSONObject("policies")
                    .getJSONObject("Certificates")
                    .getJSONArray("Install");

            Assert.assertEquals(installJson.get(0), "cert1.der"); // existing
            Assert.assertEquals(installJson.get(2), "cert2.der"); // new
        } catch(Exception e) {
            Assert.fail("Unexpected error occurred while testing", e);
        }
    }

    @Test(priority = 6)
    public static void overwriteArray() {
        try {
            JSONObject certificatesInstall = new JSONObject().put(
                    "policies", new JSONObject().put(
                            "Certificates", new JSONObject().put(
                                    "Install", new JSONArray().put(
                                            "cert3.der"))));
            // Clobber
            JsonWriter.write(JSON_FILE.getPath(), certificatesInstall.toString(), true, false);

            JSONArray installJson = readJsonFile()
                    .getJSONObject("policies")
                    .getJSONObject("Certificates")
                    .getJSONArray("Install");

            Assert.assertEquals(installJson.get(0), "cert3.der");
            Assert.assertNull(installJson.opt(1));
        } catch(Exception e) {
            Assert.fail("Unexpected error occurred while testing", e);
        }
    }

    @Test(priority = 7)
    public static void deleteArray() {
        try {
            JSONObject certificatesDelete = new JSONObject().put(
                    "policies", new JSONObject().put(
                            "Certificates", new JSONArray().put(
                                    "Install")));
            // Delete
            JsonWriter.write(JSON_FILE.getPath(), certificatesDelete.toString(), false, true);

            JSONObject certs = readJsonFile()
                    .getJSONObject("policies")
                    .optJSONObject("Certificates");

            Assert.assertNull(certs);
        } catch(Exception e) {
            Assert.fail("Unexpected error occurred while testing", e);
        }
    }

    private static JSONObject readJsonFile() throws IOException, JSONException {
        return new JSONObject(FileUtilities.readLocalFile(JSON_FILE.toPath()));
    }

}
