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
import java.util.List;

public class CertificateTests {
    @Test
    public void testStandard() throws Exception {
        assertNames("chain-standard.txt",
                "QZ Test Multi Intermediate Leaf",
                "QZ Test Intermediate Two",
                "QZ Test Intermediate One");
    }

    @Test
    public void testLegacy() throws Exception {
        assertNames("chain-legacy.txt",
                "QZ Test Legacy Leaf",
                "QZ Test Intermediate One");
    }

    @Test
    public void testSingle() throws Exception {
        String certificateData = read("leaf-direct.crt");
        List<X509Certificate> certificates = Certificate.parseCertificates(certificateData);
        List<Certificate> previousRoots = new ArrayList<>(Certificate.rootCAs);
        Certificate previousBuiltIn = Certificate.builtIn;

        Assert.assertEquals(certificates.size(), 1);
        Assert.assertTrue(certificates.subList(1, certificates.size()).isEmpty());

        try {
            Certificate.rootCAs.clear();
            Certificate root = new Certificate(read("ca-root.crt"));
            Certificate.rootCAs.add(root);
            Certificate.builtIn = root;

            Certificate certificate = new Certificate(certificateData);

            Assert.assertTrue(certificate.isTrusted());
            Assert.assertEquals(Certificate.rootCAs.size(), 1);
            Assert.assertSame(Certificate.rootCAs.getFirst(), root);
        }
        finally {
            Certificate.rootCAs.clear();
            Certificate.rootCAs.addAll(previousRoots);
            Certificate.builtIn = previousBuiltIn;
        }
    }

    @Test
    public void testEmbeddedRoot() throws Exception {
        assertTrust(read("chain-standard-with-root.txt"),
                "ca-unrelated.crt", false);
    }

    @Test
    public void testExpired() throws Exception {
        Certificate certificate = new Certificate(read("chain-expired.txt"));

        Assert.assertTrue(certificate.isExpired());
        Assert.assertFalse(certificate.isTrusted());
    }

    @Test
    public void testTrustedChains() throws Exception {
        assertTrust(read("leaf-direct.crt"), "ca-root.crt", true);
        assertTrust(read("leaf-legacy.crt") + read("ca-intermediate-one.crt"),
                "ca-root.crt", true);
        assertTrust(read("chain-standard.txt"), "ca-root.crt", true);
    }

    @Test
    public void testUntrustedChains() throws Exception {
        assertTrust(read("leaf-direct.crt"), "ca-unrelated.crt", false);
        assertTrust(read("leaf-legacy.crt") + read("ca-intermediate-one.crt"),
                "ca-unrelated.crt", false);
        assertTrust(read("chain-standard.txt"), "ca-unrelated.crt", false);
    }

    @Test
    public void testMalformed() {
        String malformed = "-----BEGIN CERTIFICATE-----\ninvalid\n-----END CERTIFICATE-----\n";
        Assert.expectThrows(CertificateException.class, () -> new Certificate(malformed));
    }

    @Test
    public void testDuplicate() throws Exception {
        String bundle = read("chain-standard.txt") + read("ca-intermediate-two.crt");

        Assert.assertEquals(Certificate.parseCertificates(bundle).size(), 4);
        assertTrust(bundle, "ca-root.crt", true);
    }

    @Test
    public void testReversed() throws Exception {
        String bundle = read("ca-intermediate-one.crt")
                + read("ca-intermediate-two.crt") + read("leaf-standard.crt");

        Assert.assertEquals(commonName(Certificate.parseCertificates(bundle).getFirst()),
                "QZ Test Intermediate One");
        assertTrust(bundle, "ca-root.crt", true);
    }

    @Test
    public void testIncomplete() throws Exception {
        assertTrust(read("leaf-standard.crt") + read("ca-intermediate-two.crt"),
                "ca-root.crt", false);
    }

    @Test
    public void testSelfSigned() throws Exception {
        assertTrust(read("leaf-self-signed.crt"), "ca-root.crt", false);
    }

    @Test
    public void testUnrelated() throws Exception {
        assertTrust(read("leaf-standard.crt") + read("ca-unrelated.crt"),
                "ca-root.crt", false);
        assertTrust(read("chain-standard.txt") + read("ca-unrelated.crt"),
                "ca-root.crt", true);
    }

    private void assertNames(String resource, String... expected) throws Exception {
        List<X509Certificate> certificates = Certificate.parseCertificates(read(resource));

        Assert.assertEquals(certificates.size(), expected.length);
        for(int i = 0; i < expected.length; i++) {
            Assert.assertEquals(commonName(certificates.get(i)), expected[i]);
        }
    }

    private void assertTrust(String certificateData, String rootResource,
                             boolean trusted) throws Exception {
        List<Certificate> previousRoots = new ArrayList<>(Certificate.rootCAs);
        Certificate previousBuiltIn = Certificate.builtIn;

        try {
            Certificate.rootCAs.clear();
            Certificate root = new Certificate(read(rootResource));
            Certificate.rootCAs.add(root);
            Certificate.builtIn = root;

            Certificate certificate = new Certificate(certificateData);

            Assert.assertEquals(certificate.isTrusted(), trusted);
            Assert.assertEquals(Certificate.rootCAs.size(), 1);
            Assert.assertSame(Certificate.rootCAs.getFirst(), root);
        }
        finally {
            Certificate.rootCAs.clear();
            Certificate.rootCAs.addAll(previousRoots);
            Certificate.builtIn = previousBuiltIn;
        }
    }

    private String read(String resource) throws IOException {
        return readResource("resources/certificate-chain/" + resource);
    }

    private String readResource(String resource) throws IOException {
        try(InputStream input = getClass().getResourceAsStream(resource)) {
            Assert.assertNotNull(input, "Missing certificate fixture " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String commonName(X509Certificate certificate) {
        X500Name subject = X500Name.getInstance(certificate.getSubjectX500Principal().getEncoded());
        return subject.getRDNs(BCStyle.CN)[0].getFirst().getValue().toString();
    }
}
