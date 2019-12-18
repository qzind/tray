package qz.installer;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import qz.installer.certificate.CertificateChainBuilder;
import qz.installer.certificate.ExpiryTask;
import qz.installer.certificate.CertificateManager;

import java.io.IOException;
import java.io.StringReader;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HashMap;

public class InstallerTests {

    public static void main(String ... args) throws Exception {
        // runInstallerTests();
        runExpiryTests();
    }

    public static void runInstallerTests() throws Exception {
        CertificateChainBuilder.SSL_CERT_AGE = 1;
        Installer installer = Installer.getInstance();
        // installer.install();
        CertificateManager certificateManager = installer.certGen(true);
        new ExpiryTask(certificateManager).schedule(1000, 1000);
        Thread.sleep(5000);
        installer.removeCerts();
    }
    public static void runExpiryTests() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        String[] testCerts = { QZ_INDUSTRIES_CERT, CA_CERT_ORG_CERT, LETS_ENCRYPT_CERT };

        HashMap<ExpiryTask.CertProvider, String> certmap = new HashMap<>();
        certmap.put(ExpiryTask.CertProvider.INTERNAL, QZ_INDUSTRIES_CERT);
        certmap.put(ExpiryTask.CertProvider.CA_CERT_ORG, CA_CERT_ORG_CERT);
        certmap.put(ExpiryTask.CertProvider.LETS_ENCRYPT, LETS_ENCRYPT_CERT);


        for(String testCert : testCerts) {
            X509Certificate cert = loadCert(testCert);
            ExpiryTask.findCertProvider(cert);
            ExpiryTask.getExpiry(cert);
            ExpiryTask.parseHostNames(cert);
        }
    }

    public static X509Certificate loadCert(String cert) throws IOException {
        PEMParser reader = new PEMParser(new StringReader(cert));
        return (X509Certificate)reader.readObject();
    }

    private static String QZ_INDUSTRIES_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFDjCCA/agAwIBAgIGAW3W19xeMA0GCSqGSIb3DQEBCwUAMIGaMQswCQYDVQQG\n" +
            "EwJVUzELMAkGA1UECAwCTlkxEjAQBgNVBAcMCUNhbmFzdG90YTEbMBkGA1UECgwS\n" +
            "UVogSW5kdXN0cmllcywgTExDMRswGQYDVQQLDBJRWiBJbmR1c3RyaWVzLCBMTEMx\n" +
            "HDAaBgkqhkiG9w0BCQEWDXN1cHBvcnRAcXouaW8xEjAQBgNVBAMMCWxvY2FsaG9z\n" +
            "dDAeFw0xOTEwMTUyMzEyMTNaFw0yMjAxMTgwMDEyMTNaMIGaMQswCQYDVQQGEwJV\n" +
            "UzELMAkGA1UECAwCTlkxEjAQBgNVBAcMCUNhbmFzdG90YTEbMBkGA1UECgwSUVog\n" +
            "SW5kdXN0cmllcywgTExDMRswGQYDVQQLDBJRWiBJbmR1c3RyaWVzLCBMTEMxHDAa\n" +
            "BgkqhkiG9w0BCQEWDXN1cHBvcnRAcXouaW8xEjAQBgNVBAMMCWxvY2FsaG9zdDCC\n" +
            "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK8Hfp8Hujhr6OCTJYLPnluv\n" +
            "XgDi92eX8nkW+HkpWjgDwjv59VqIiycSGTxp5GCozvDF7zHbrSICVOlHa1iFXv3w\n" +
            "8EpWTIKxfqiNDZohnq38R1lVGwfPC97pzaqu5CWvjTmUD5T/Cl5RnZEvnKoXvxAA\n" +
            "9/Eikzz7TGr2BL56rJFmwYRosEd2tvyxV4o/m1t/PSU9cAi1GzWpuwRbmFl34cvV\n" +
            "tMPeWUz315zy8Qw9cz4ktb1O/H+5BWXdpb9DRUS9QG6sS1Esi9jIZ7rPjm+Gqj3P\n" +
            "mcsev9jVlex7C0eMG3QVLpOiurPxKYkGHH9F9W6PXvKEk/jWjFFxbpy380iqTb8C\n" +
            "AwEAAaOCAVYwggFSMIHMBgNVHSMEgcQwgcGAFCNVfcjxztjhZUuVHS5vsRDzVvhb\n" +
            "oYGgpIGdMIGaMQswCQYDVQQGEwJVUzELMAkGA1UECAwCTlkxEjAQBgNVBAcMCUNh\n" +
            "bmFzdG90YTEbMBkGA1UECgwSUVogSW5kdXN0cmllcywgTExDMRswGQYDVQQLDBJR\n" +
            "WiBJbmR1c3RyaWVzLCBMTEMxHDAaBgkqhkiG9w0BCQEWDXN1cHBvcnRAcXouaW8x\n" +
            "EjAQBgNVBAMMCWxvY2FsaG9zdIIGAW3W19ucMAwGA1UdEwEB/wQCMAAwDgYDVR0P\n" +
            "AQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAlBgNVHREE\n" +
            "HjAcgglsb2NhbGhvc3SCD2xvY2FsaG9zdC5xei5pbzAdBgNVHQ4EFgQUf2fwQ8IJ\n" +
            "pdlT4+ghS0BP/V91ix0wDQYJKoZIhvcNAQELBQADggEBAHFiDZ7jItbHjpxxOHYF\n" +
            "g6O61+7ETEPy0JGIPWxiysNCDfKyxuaVQ0UZ3/r6g5uQs3GjiQRIFxTmBk0hFTYB\n" +
            "ONS2P0ugyED+C5wJADDcILa8SAF0EwrFX/6f3TnG+Qvn3jBRUCnjKTMfpnSlgMTk\n" +
            "/wm1Jg10gUEXGHWGagw4YPVwMvBaWWYEFPC/emlONcAkZv4gfPZJ61bZgstqF+bZ\n" +
            "WQM1GF1TOO8x/2KgguTknxc1EI4SmWN3Zl58BY8sf95yribLmKFW2VwbOHqfs0/d\n" +
            "lFDMhix3cTURGvpyt+ZM4KXD9VkFpLIqRe1Qj02BPXS4GDNPQ+3xPbFOpvIKeYhf\n" +
            "cGk=\n" +
            "-----END CERTIFICATE-----";

    private static String CA_CERT_ORG_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIHnjCCBYagAwIBAgIDE4H4MA0GCSqGSIb3DQEBDQUAMHkxEDAOBgNVBAoTB1Jv\n" +
            "b3QgQ0ExHjAcBgNVBAsTFWh0dHA6Ly93d3cuY2FjZXJ0Lm9yZzEiMCAGA1UEAxMZ\n" +
            "Q0EgQ2VydCBTaWduaW5nIEF1dGhvcml0eTEhMB8GCSqGSIb3DQEJARYSc3VwcG9y\n" +
            "dEBjYWNlcnQub3JnMB4XDTE4MDMxNzExMTMxNloXDTIwMDMxNjExMTMxNlowYTEL\n" +
            "MAkGA1UEBhMCQVUxDDAKBgNVBAgTA05TVzEPMA0GA1UEBxMGU3lkbmV5MRQwEgYD\n" +
            "VQQKEwtDQWNlcnQgSW5jLjEdMBsGA1UEAxMUY29tbXVuaXR5LmNhY2VydC5vcmcw\n" +
            "ggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQDKY4Bz8s5f0AK56dGIl8y1\n" +
            "qnLyNhJr2pxJF9PInO33meBiCqpoTWpPHyIO51NGeySrlW35ZXUzp6tBMptXQict\n" +
            "J7PkQcSf+lEn1AmRtWHIFNf/uM5IlgoomKktbAkkK+PLOtDBuZ40sKnRY1ooJ9ZK\n" +
            "UnOrb5puz1D+JHp8JYxkPfknCNAZLeNPXqn9QqnpFKk8/c2CrVF8hShk/k5t2Dpr\n" +
            "Q0Et9FkPOYBru9p5LQXQBA5QKPg1ESAVKYxRLbR4tJ02we6rOKWgLCnETlMmdjky\n" +
            "NgaDG6dg79wNKu/uuYyQSXaAnJU67RGXNxIpudOlZ0c2+467mWDFaUHY4yzGTquq\n" +
            "OGhMDXJu2fe7kDcBP8qH9YeIhN1WSLSnN4cbIP9UVxZXNfZ0WnA2Drj8iGlpL48v\n" +
            "vBzuUD6EZ+WTeOkoapb0CRGAB+wdMQ6Tg+87tx8vUkhilk3NZ3kKRzOoDKiDisK9\n" +
            "/WFh8aU7Eq62V15TmzOOkCHmXME1KH2CuzG4MQzalFz8ahRQQnezEMt91uHvCZya\n" +
            "t5lcGr9W57FnYcxG6KqUO4iV6HWmJYXYhl5PfpEKzKktceH1PnuDptnE8mtdJW1T\n" +
            "8p43ubgcAGxEvsq6nbeY76b1xlIkq1/NEL3BPDSoz+Tnz5MwLKjHQcqA7Av/KRH3\n" +
            "VBnw4YI0VtGxZnz4wjyA8wIDAQABo4ICRTCCAkEwDAYDVR0TAQH/BAIwADAOBgNV\n" +
            "HQ8BAf8EBAMCA6gwNAYDVR0lBC0wKwYIKwYBBQUHAwIGCCsGAQUFBwMBBglghkgB\n" +
            "hvhCBAEGCisGAQQBgjcKAwMwMwYIKwYBBQUHAQEEJzAlMCMGCCsGAQUFBzABhhdo\n" +
            "dHRwOi8vb2NzcC5jYWNlcnQub3JnLzAxBgNVHR8EKjAoMCagJKAihiBodHRwOi8v\n" +
            "Y3JsLmNhY2VydC5vcmcvcmV2b2tlLmNybDCCAYEGA1UdEQSCAXgwggF0ghRjb21t\n" +
            "dW5pdHkuY2FjZXJ0Lm9yZ6AiBggrBgEFBQcIBaAWDBRjb21tdW5pdHkuY2FjZXJ0\n" +
            "Lm9yZ4Ibbm9jZXJ0LmNvbW11bml0eS5jYWNlcnQub3JnoCkGCCsGAQUFBwgFoB0M\n" +
            "G25vY2VydC5jb21tdW5pdHkuY2FjZXJ0Lm9yZ4IZY2VydC5jb21tdW5pdHkuY2Fj\n" +
            "ZXJ0Lm9yZ6AnBggrBgEFBQcIBaAbDBljZXJ0LmNvbW11bml0eS5jYWNlcnQub3Jn\n" +
            "ghBlbWFpbC5jYWNlcnQub3JnoB4GCCsGAQUFBwgFoBIMEGVtYWlsLmNhY2VydC5v\n" +
            "cmeCF25vY2VydC5lbWFpbC5jYWNlcnQub3JnoCUGCCsGAQUFBwgFoBkMF25vY2Vy\n" +
            "dC5lbWFpbC5jYWNlcnQub3JnghVjZXJ0LmVtYWlsLmNhY2VydC5vcmegIwYIKwYB\n" +
            "BQUHCAWgFwwVY2VydC5lbWFpbC5jYWNlcnQub3JnMA0GCSqGSIb3DQEBDQUAA4IC\n" +
            "AQBWaOcDYaF25eP9eJTBUItFKkK3ppq7eN0qT9qyrWVxhRMWtAYcjW8hfSOx5xPS\n" +
            "4bYL8RJz+1NNyzZqbyhvHt9JnCn1g2HllSD1HTHSMxZZrdjWq/9XxnmG55u2CUfo\n" +
            "hN1M0qmUJvvWv0T4YWMwhv94tKrThDXnvqa4S+JfnTZQTLPAVq+iTKr+bsdB7pkI\n" +
            "D59SJdE9tRsrb1wfbBbEpYw2LBZo7Jje4E9FmtnMraGxZtFsHhpZvYAnEt80eFts\n" +
            "ccSOlhqowW9Hqx0pg55Sq9Wrj9T+AxTx/6sAJL4qxm7CRjeIAqW5fksvA4yXgYaq\n" +
            "g6M2uIcRMEeafN8bHy1LOXkZDAcbusPfAGenMdE/p5B0K45Rlx3+dfNUjHyF4+ob\n" +
            "FOVNxgPcfCZ2lJrgvJbw9tBGqC13yPUlkywQ+7QSJgTPbWrnXLIu7fz5SmCxk5KD\n" +
            "zsq4F4YsaeBIYeHOsJLbqeqftm3eNBESphOvXlZKMGRMiThVWIaX5PIZB5OKgyE3\n" +
            "C5CvKcv5qv1CeI7qFtLkq28QKCqJJIfTDvArEq/O5P2d+yQetYkWN5mzCJqT/kB+\n" +
            "y74nu6kCBoZNWBZHDKeM6NkZD1/wI47S2A4cmE7SiGx3AcNRhmrXhvnSD7u7cGVD\n" +
            "b5yw6z+JqFRMqMm0SuSx5X2oKNKfnqY77fIx6dtY8F5Scg==\n" +
            "-----END CERTIFICATE-----\n" +
            " 1 s:/O=Root CA/OU=http://www.cacert.org/CN=CA Cert Signing Authority/emailAddress=support@cacert.org\n" +
            "   i:/O=Root CA/OU=http://www.cacert.org/CN=CA Cert Signing Authority/emailAddress=support@cacert.org\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIHPTCCBSWgAwIBAgIBADANBgkqhkiG9w0BAQQFADB5MRAwDgYDVQQKEwdSb290\n" +
            "IENBMR4wHAYDVQQLExVodHRwOi8vd3d3LmNhY2VydC5vcmcxIjAgBgNVBAMTGUNB\n" +
            "IENlcnQgU2lnbmluZyBBdXRob3JpdHkxITAfBgkqhkiG9w0BCQEWEnN1cHBvcnRA\n" +
            "Y2FjZXJ0Lm9yZzAeFw0wMzAzMzAxMjI5NDlaFw0zMzAzMjkxMjI5NDlaMHkxEDAO\n" +
            "BgNVBAoTB1Jvb3QgQ0ExHjAcBgNVBAsTFWh0dHA6Ly93d3cuY2FjZXJ0Lm9yZzEi\n" +
            "MCAGA1UEAxMZQ0EgQ2VydCBTaWduaW5nIEF1dGhvcml0eTEhMB8GCSqGSIb3DQEJ\n" +
            "ARYSc3VwcG9ydEBjYWNlcnQub3JnMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC\n" +
            "CgKCAgEAziLA4kZ97DYoB1CW8qAzQIxL8TtmPzHlawI229Z89vGIj053NgVBlfkJ\n" +
            "8BLPRoZzYLdufujAWGSuzbCtRRcMY/pnCujW0r8+55jE8Ez64AO7NV1sId6eINm6\n" +
            "zWYyN3L69wj1x81YyY7nDl7qPv4coRQKFWyGhFtkZip6qUtTefWIonvuLwphK42y\n" +
            "fk1WpRPs6tqSnqxEQR5YYGUFZvjARL3LlPdCfgv3ZWiYUQXw8wWRBB0bF4LsyFe7\n" +
            "w2t6iPGwcswlWyCR7BYCEo8y6RcYSNDHBS4CMEK4JZwFaz+qOqfrU0j36NK2B5jc\n" +
            "G8Y0f3/JHIJ6BVgrCFvzOKKrF11myZjXnhCLotLddJr3cQxyYN/Nb5gznZY0dj4k\n" +
            "epKwDpUeb+agRThHqtdB7Uq3EvbXG4OKDy7YCbZZ16oE/9KTfWgu3YtLq1i6L43q\n" +
            "laegw1SJpfvbi1EinbLDvhG+LJGGi5Z4rSDTii8aP8bQUWWHIbEZAWV/RRyH9XzQ\n" +
            "QUxPKZgh/TMfdQwEUfoZd9vUFBzugcMd9Zi3aQaRIt0AUMyBMawSB3s42mhb5ivU\n" +
            "fslfrejrckzzAeVLIL+aplfKkQABi6F1ITe1Yw1nPkZPcCBnzsXWWdsC4PDSy826\n" +
            "YreQQejdIOQpvGQpQsgi3Hia/0PsmBsJUUtaWsJx8cTLc6nloQsCAwEAAaOCAc4w\n" +
            "ggHKMB0GA1UdDgQWBBQWtTIb1Mfz4OaO873SsDrusjkY0TCBowYDVR0jBIGbMIGY\n" +
            "gBQWtTIb1Mfz4OaO873SsDrusjkY0aF9pHsweTEQMA4GA1UEChMHUm9vdCBDQTEe\n" +
            "MBwGA1UECxMVaHR0cDovL3d3dy5jYWNlcnQub3JnMSIwIAYDVQQDExlDQSBDZXJ0\n" +
            "IFNpZ25pbmcgQXV0aG9yaXR5MSEwHwYJKoZIhvcNAQkBFhJzdXBwb3J0QGNhY2Vy\n" +
            "dC5vcmeCAQAwDwYDVR0TAQH/BAUwAwEB/zAyBgNVHR8EKzApMCegJaAjhiFodHRw\n" +
            "czovL3d3dy5jYWNlcnQub3JnL3Jldm9rZS5jcmwwMAYJYIZIAYb4QgEEBCMWIWh0\n" +
            "dHBzOi8vd3d3LmNhY2VydC5vcmcvcmV2b2tlLmNybDA0BglghkgBhvhCAQgEJxYl\n" +
            "aHR0cDovL3d3dy5jYWNlcnQub3JnL2luZGV4LnBocD9pZD0xMDBWBglghkgBhvhC\n" +
            "AQ0ESRZHVG8gZ2V0IHlvdXIgb3duIGNlcnRpZmljYXRlIGZvciBGUkVFIGhlYWQg\n" +
            "b3ZlciB0byBodHRwOi8vd3d3LmNhY2VydC5vcmcwDQYJKoZIhvcNAQEEBQADggIB\n" +
            "ACjH7pyCArpcgBLKNQodgW+JapnM8mgPf6fhjViVPr3yBsOQWqy1YPaZQwGjiHCc\n" +
            "nWKdpIevZ1gNMDY75q1I08t0AoZxPuIrA2jxNGJARjtT6ij0rPtmlVOKTV39O9lg\n" +
            "18p5aTuxZZKmxoGCXJzN600BiqXfEVWqFcofN8CCmHBh22p8lqOOLlQ+TyGpkO/c\n" +
            "gr/c6EWtTZBzCDyUZbAEmXZ/4rzCahWqlwQ3JNgelE5tDlG+1sSPypZt90Pf6DBl\n" +
            "Jzt7u0NDY8RD97LsaMzhGY4i+5jhe1o+ATc7iwiwovOVThrLm82asduycPAtStvY\n" +
            "sONvRUgzEv/+PDIqVPfE94rwiCPCR/5kenHA0R6mY7AHfqQv0wGP3J8rtsYIqQ+T\n" +
            "SCX8Ev2fQtzzxD72V7DX3WnRBnc0CkvSyqD/HMaMyRa+xMwyN2hzXwj7UfdJUzYF\n" +
            "CpUCTPJ5GhD22Dp1nPMd8aINcGeGG7MW9S/lpOt5hvk9C8JzC6WZrG/8Z7jlLwum\n" +
            "GCSNe9FINSkYQKyTYOGWhlC0elnYjyELn8+CkcY7v2vcB5G5l1YjqrZslMZIBjzk\n" +
            "zk6q5PYvCdxTby78dOs6Y5nCpqyJvKeyRKANihDjbPIky/qbn3BHLt4Ui9SyIAmW\n" +
            "omTxJBzcoTWcFbLUvFUufQb1nA5V9FrWk9p2rSVzTMVD\n" +
            "-----END CERTIFICATE-----";

    private static String LETS_ENCRYPT_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIFTTCCBDWgAwIBAgISA/Qu8kKrD8kLzdY+/WPM8whbMA0GCSqGSIb3DQEBCwUA\n" +
            "MEoxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MSMwIQYDVQQD\n" +
            "ExpMZXQncyBFbmNyeXB0IEF1dGhvcml0eSBYMzAeFw0xOTA4MjgxMzQ0MzdaFw0x\n" +
            "OTExMjYxMzQ0MzdaMBYxFDASBgNVBAMTC2J1aWxkLnF6LmlvMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9Q/StADlpSnsShayw4SV4dIbiOiiEYwqBlB7\n" +
            "FYFF7LfZdREXlYBaTH46hUJI1ooUfsfnNTnYHac6tCEwr9wQnnobO7ACtuYENrVN\n" +
            "HiuzYtMGN90mqf2+PXhHb+xGpBrD36fmq4Ix3aIc5o4lKxFY4IstfbTbYDanF1Q4\n" +
            "qUIRUSdAJdgJqmJB2hwlFvjzeBGV4h6vgmiEsATawGoSDMLdWsFpiEnYLTfyvvhY\n" +
            "5L4e2O9roBOEQ/YJbWVrewh6LYs6s6SbbNkKttQNSGUFVeW6u8q5+yHi2chSXlwW\n" +
            "+o1SdjE6yw9laHp/nog5gyg95O2xm36YA3mRgfoAEfimwFwf2wIDAQABo4ICXzCC\n" +
            "AlswDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcD\n" +
            "AjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBTRuEPSdvHr2SkCIpArJG34rUOPLzAf\n" +
            "BgNVHSMEGDAWgBSoSmpjBH3duubRObemRWXv86jsoTBvBggrBgEFBQcBAQRjMGEw\n" +
            "LgYIKwYBBQUHMAGGImh0dHA6Ly9vY3NwLmludC14My5sZXRzZW5jcnlwdC5vcmcw\n" +
            "LwYIKwYBBQUHMAKGI2h0dHA6Ly9jZXJ0LmludC14My5sZXRzZW5jcnlwdC5vcmcv\n" +
            "MBYGA1UdEQQPMA2CC2J1aWxkLnF6LmlvMEwGA1UdIARFMEMwCAYGZ4EMAQIBMDcG\n" +
            "CysGAQQBgt8TAQEBMCgwJgYIKwYBBQUHAgEWGmh0dHA6Ly9jcHMubGV0c2VuY3J5\n" +
            "cHQub3JnMIIBAwYKKwYBBAHWeQIEAgSB9ASB8QDvAHYAb1N2rDHwMRnYmQCkURX/\n" +
            "dxUcEdkCwQApBo2yCJo32RMAAAFs2K+GMAAABAMARzBFAiAI6WH6tspPGgp6W3KI\n" +
            "n3Ihkb5OqS4KjGFbWNxsJq+/FgIhAJ0zLvFPdlivXpJd/Vn/+xKIBeAs9Ens2uxS\n" +
            "A34B35oyAHUAY/Lbzeg7zCzPC3KEJ1drM6SNYXePvXWmOLHHaFRL2I0AAAFs2K+F\n" +
            "FwAABAMARjBEAiAEYpsT6YoIByfh2SHOjuvICRUejlAHVS6bbPN+hvV+4gIgS6pt\n" +
            "7MtF6GA83AF3lVZPCSnUKp3VvqcEjchf493wHAowDQYJKoZIhvcNAQELBQADggEB\n" +
            "AH1Nr3BfiCG6iRUtGpaxoIv1J2XDmxAfz5kEtoErwo/oPTz2xY8UyYa1WFlCyJU1\n" +
            "JWvGrbpT3MQXbdrLsSyT2HQRwEKzXr/u8rRSj18cqggwi8T/f9HgZXjf4ly19uYU\n" +
            "5GqLBsPwO8BVzawr/bnI0viH1uVpcIQA/rW63LkOL8bMv16zW27mnoEAo8NG1YZU\n" +
            "IEuCfMH/wFfkbmcw549l2PqIidVqSvWPltLlGdkNJYobFvyg5ThWXNb57cNIMb1k\n" +
            "Egy5O7RqmVycOdt6//M5KrluWDUS/qi+7oAllGJ9AnFVDttmKuklrhGmwRv/ezN7\n" +
            "gUtpN5eb5M1XxvExz3fXxfM=\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEkjCCA3qgAwIBAgIQCgFBQgAAAVOFc2oLheynCDANBgkqhkiG9w0BAQsFADA/\n" +
            "MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n" +
            "DkRTVCBSb290IENBIFgzMB4XDTE2MDMxNzE2NDA0NloXDTIxMDMxNzE2NDA0Nlow\n" +
            "SjELMAkGA1UEBhMCVVMxFjAUBgNVBAoTDUxldCdzIEVuY3J5cHQxIzAhBgNVBAMT\n" +
            "GkxldCdzIEVuY3J5cHQgQXV0aG9yaXR5IFgzMIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
            "AQ8AMIIBCgKCAQEAnNMM8FrlLke3cl03g7NoYzDq1zUmGSXhvb418XCSL7e4S0EF\n" +
            "q6meNQhY7LEqxGiHC6PjdeTm86dicbp5gWAf15Gan/PQeGdxyGkOlZHP/uaZ6WA8\n" +
            "SMx+yk13EiSdRxta67nsHjcAHJyse6cF6s5K671B5TaYucv9bTyWaN8jKkKQDIZ0\n" +
            "Z8h/pZq4UmEUEz9l6YKHy9v6Dlb2honzhT+Xhq+w3Brvaw2VFn3EK6BlspkENnWA\n" +
            "a6xK8xuQSXgvopZPKiAlKQTGdMDQMc2PMTiVFrqoM7hD8bEfwzB/onkxEz0tNvjj\n" +
            "/PIzark5McWvxI0NHWQWM6r6hCm21AvA2H3DkwIDAQABo4IBfTCCAXkwEgYDVR0T\n" +
            "AQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAYYwfwYIKwYBBQUHAQEEczBxMDIG\n" +
            "CCsGAQUFBzABhiZodHRwOi8vaXNyZy50cnVzdGlkLm9jc3AuaWRlbnRydXN0LmNv\n" +
            "bTA7BggrBgEFBQcwAoYvaHR0cDovL2FwcHMuaWRlbnRydXN0LmNvbS9yb290cy9k\n" +
            "c3Ryb290Y2F4My5wN2MwHwYDVR0jBBgwFoAUxKexpHsscfrb4UuQdf/EFWCFiRAw\n" +
            "VAYDVR0gBE0wSzAIBgZngQwBAgEwPwYLKwYBBAGC3xMBAQEwMDAuBggrBgEFBQcC\n" +
            "ARYiaHR0cDovL2Nwcy5yb290LXgxLmxldHNlbmNyeXB0Lm9yZzA8BgNVHR8ENTAz\n" +
            "MDGgL6AthitodHRwOi8vY3JsLmlkZW50cnVzdC5jb20vRFNUUk9PVENBWDNDUkwu\n" +
            "Y3JsMB0GA1UdDgQWBBSoSmpjBH3duubRObemRWXv86jsoTANBgkqhkiG9w0BAQsF\n" +
            "AAOCAQEA3TPXEfNjWDjdGBX7CVW+dla5cEilaUcne8IkCJLxWh9KEik3JHRRHGJo\n" +
            "uM2VcGfl96S8TihRzZvoroed6ti6WqEBmtzw3Wodatg+VyOeph4EYpr/1wXKtx8/\n" +
            "wApIvJSwtmVi4MFU5aMqrSDE6ea73Mj2tcMyo5jMd6jmeWUHK8so/joWUoHOUgwu\n" +
            "X4Po1QYz+3dszkDqMp4fklxBwXRsW10KXzPMTZ+sOPAveyxindmjkW8lGy+QsRlG\n" +
            "PfZ+G6Z6h7mjem0Y+iWlkYcV4PIWL1iwBi8saCbGS5jN2p8M+X+Q7UNKEkROb3N6\n" +
            "KOqkqm57TH2H3eDJAkSnh6/DNFu0Qg==\n" +
            "-----END CERTIFICATE-----";
}
