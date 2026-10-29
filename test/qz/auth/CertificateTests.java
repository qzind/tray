package qz.auth;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;

public class CertificateTests {
    @Test
    public void testStandard() throws Exception {
        assertNames("standard-chain.pem",
                "QZ Test Multi Intermediate Leaf",
                "QZ Test Intermediate Two",
                "QZ Test Intermediate One");
    }

    @Test
    public void testLegacy() throws Exception {
        assertNames("legacy-chain.pem",
                "QZ Test Legacy Leaf",
                "QZ Test Intermediate One");
    }

    @Test
    public void testSingle() throws Exception {
        assertNames("leaf.pem", "QZ Test Multi Intermediate Leaf");
    }

    @Test
    public void testStandardRoundTrip() throws Exception {
        assertRoundTrip("standard-chain.pem", "QZ Test Multi Intermediate Leaf");
    }

    @Test
    public void testLegacyRoundTrip() throws Exception {
        assertRoundTrip("legacy-chain.pem", "QZ Test Legacy Leaf");
    }

    @Test
    public void testEmbeddedRoot() throws Exception {
        int rootCount = Certificate.rootCAs.size();
        Certificate certificate = new Certificate(read("standard-chain-with-root.pem"));
        String row = certificate.data();

        Assert.assertEquals(Certificate.rootCAs.size(), rootCount);
        Assert.assertEquals(row.split("\\t", -1).length, Certificate.saveFields.length);
        Assert.assertFalse(row.contains("BEGIN CERTIFICATE"));
        Assert.assertFalse(row.contains("QZ Test Intermediate One"));
        Assert.assertFalse(row.contains("QZ Test Intermediate Two"));
        Assert.assertFalse(row.contains("Root CA"));
        Assert.assertEquals(load(row).getCommonName(), "QZ Test Multi Intermediate Leaf");
    }

    private void assertNames(String resource, String... expected) throws Exception {
        List<X509Certificate> certificates = Certificate.parseCertificates(read(resource));

        Assert.assertEquals(certificates.size(), expected.length);
        for(int i = 0; i < expected.length; i++) {
            Assert.assertEquals(commonName(certificates.get(i)), expected[i]);
        }
    }

    private void assertRoundTrip(String resource, String commonName) throws Exception {
        Certificate source = new Certificate(read(resource));
        Certificate reloaded = load(source.data());

        assertIdentity(reloaded, source);
        Assert.assertEquals(reloaded.getCommonName(), commonName);
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
        try(InputStream input = getClass().getResourceAsStream("resources/certificate-chain/" + resource)) {
            Assert.assertNotNull(input, "Missing certificate fixture " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String commonName(X509Certificate certificate) {
        X500Name subject = X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded());
        return subject.getRDNs(BCStyle.CN)[0].getFirst().getValue().toString();
    }
}
