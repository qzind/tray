package qz.auth;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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

    @Test
    public void testTrustedChains() throws Exception {
        assertTrust(read("direct-leaf.pem"), "root-ca.pem", "QZ Test Direct Leaf", true);
        assertTrust(read("legacy-leaf.pem") + read("intermediate-one.pem"),
                "root-ca.pem", "QZ Test Legacy Leaf", true);
        assertTrust(read("standard-chain.pem"), "root-ca.pem", "QZ Test Multi Intermediate Leaf", true);
    }

    @Test
    public void testUntrustedChains() throws Exception {
        assertTrust(read("direct-leaf.pem"), "unrelated-ca.pem", "QZ Test Direct Leaf", false);
        assertTrust(read("legacy-leaf.pem") + read("intermediate-one.pem"),
                "unrelated-ca.pem", "QZ Test Legacy Leaf", false);
        assertTrust(read("standard-chain.pem"),
                "unrelated-ca.pem", "QZ Test Multi Intermediate Leaf", false);
    }

    @Test
    public void testMalformed() {
        String malformed = "-----BEGIN CERTIFICATE-----\ninvalid\n-----END CERTIFICATE-----\n";
        Assert.expectThrows(CertificateException.class, () -> new Certificate(malformed));
    }

    @Test
    public void testDuplicate() throws Exception {
        String bundle = read("standard-chain.pem") + read("intermediate-two.pem");

        Assert.assertEquals(Certificate.parseCertificates(bundle).size(), 4);
        assertTrust(bundle, "root-ca.pem", "QZ Test Multi Intermediate Leaf", true);
    }

    @Test
    public void testReversed() throws Exception {
        String bundle = read("intermediate-one.pem") + read("intermediate-two.pem") + read("leaf.pem");

        assertTrust(bundle, "root-ca.pem", "QZ Test Intermediate One", true);
    }

    @Test
    public void testIncomplete() throws Exception {
        assertTrust(read("leaf.pem") + read("intermediate-two.pem"),
                "root-ca.pem", "QZ Test Multi Intermediate Leaf", false);
    }

    @Test
    public void testSelfSigned() throws Exception {
        assertTrust(read("self-signed.pem"),
                "root-ca.pem", "QZ Test Self Signed Leaf", false);
    }

    @Test
    public void testUnrelated() throws Exception {
        assertTrust(read("leaf.pem") + read("unrelated-ca.pem"),
                "root-ca.pem", "QZ Test Multi Intermediate Leaf", false);
        assertTrust(read("standard-chain.pem") + read("unrelated-ca.pem"),
                "root-ca.pem", "QZ Test Multi Intermediate Leaf", true);
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

    private void assertTrust(String certificateData, String rootResource,
                             String commonName, boolean trusted) throws Exception {
        List<Certificate> previousRoots = new ArrayList<>(Certificate.rootCAs);
        Certificate previousBuiltIn = Certificate.builtIn;

        try {
            Certificate.rootCAs.clear();
            Certificate root = new Certificate(read(rootResource));
            Certificate.rootCAs.add(root);
            Certificate.builtIn = root;

            Certificate certificate = new Certificate(certificateData);

            Assert.assertEquals(certificate.getCommonName(), commonName);
            Assert.assertEquals(certificate.isTrusted(), trusted);
            Assert.assertEquals(Certificate.rootCAs.size(), 1);
            Assert.assertSame(Certificate.rootCAs.get(0), root);
        }
        finally {
            Certificate.rootCAs.clear();
            Certificate.rootCAs.addAll(previousRoots);
            Certificate.builtIn = previousBuiltIn;
        }
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
