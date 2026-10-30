package qz.auth;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class CertificateSerializationTests {
    @Test
    public void testStandard() throws Exception {
        assertRoundTrip("chain-standard.txt");
    }

    @Test
    public void testLegacy() throws Exception {
        assertRoundTrip("chain-legacy.txt");
    }

    @Test
    public void testChainNotSerialized() throws Exception {
        Certificate certificate = new Certificate(read("chain-standard-with-root.txt"));
        String row = certificate.data();

        Assert.assertEquals(row.split("\\t", -1).length, Certificate.saveFields.length);
        Assert.assertFalse(row.contains("BEGIN CERTIFICATE"));
        Assert.assertFalse(row.contains("QZ Test Intermediate"));
        Assert.assertFalse(row.contains("QZ Test Root CA"));
    }

    private void assertRoundTrip(String resource) throws Exception {
        Certificate source = new Certificate(read(resource));
        Certificate reloaded = load(source.data());

        assertIdentity(reloaded, source);
        Assert.assertEquals(reloaded.isValid(), source.isTrusted());
        Assert.assertEquals(reloaded.data(), source.data());
    }

    private void assertIdentity(Certificate actual, Certificate expected) {
        Assert.assertEquals(actual.getFingerprint(), expected.getFingerprint());
        Assert.assertEquals(actual.getCommonName(), expected.getCommonName());
        Assert.assertEquals(actual.getOrganization(), expected.getOrganization());
        Assert.assertEquals(actual.getValidFrom(), expected.getValidFrom());
        Assert.assertEquals(actual.getValidTo(), expected.getValidTo());
    }

    private Certificate load(String row) {
        String[] values = row.split("\\t", -1);
        HashMap<String,String> data = new HashMap<>();

        Assert.assertEquals(values.length, Certificate.saveFields.length);
        for(int i = 0; i < values.length; i++) {
            data.put(Certificate.saveFields[i], values[i]);
        }
        return Certificate.loadCertificate(data);
    }

    private String read(String resource) throws IOException {
        try(InputStream input = getClass().getResourceAsStream(
                "resources/certificate-chain/" + resource)) {
            Assert.assertNotNull(input, "Missing certificate fixture " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
