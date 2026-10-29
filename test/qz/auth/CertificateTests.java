package qz.auth;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
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

    private void assertNames(String resource, String... expected) throws Exception {
        List<X509Certificate> certificates = Certificate.parseCertificates(read(resource));

        Assert.assertEquals(certificates.size(), expected.length);
        for(int i = 0; i < expected.length; i++) {
            Assert.assertEquals(commonName(certificates.get(i)), expected[i]);
        }
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
