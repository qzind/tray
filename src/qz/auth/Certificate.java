package qz.auth;

import com.estontorise.simplersa.RSAKeyImpl;
import com.estontorise.simplersa.RSAToolFactory;
import com.estontorise.simplersa.interfaces.RSAKey;
import com.estontorise.simplersa.interfaces.RSATool;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.ssl.X509CertificateChainBuilder;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.jce.PrincipalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.iharder.Base64;
import qz.common.Constants;
import qz.utils.ByteUtilities;
import qz.utils.FileUtilities;
import qz.ws.PrintSocketServer;

import javax.security.cert.CertificateParsingException;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

/**
 * Created by Steven on 1/27/2015. Package: qz.auth Project: qz-print
 * Wrapper to store certificate objects from
 */
public class Certificate {

    private static final Logger log = LoggerFactory.getLogger(Certificate.class);

    public static Certificate trustedRootCert = null;
    public static final String[] saveFields = new String[] {"fingerprint", "commonName", "organization", "validFrom", "validTo", "valid"};

    private static boolean overrideTrustedRootCert = false;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private X509Certificate theCertificate;
    private String fingerprint;
    private String commonName;
    private String organization;
    private Date validFrom;
    private Date validTo;

    private boolean valid = false; //used by review sites UI only


    //Pre-set certificates for various situations that could arise with bad security requests
    public static final Certificate UNKNOWN;
    public static final Certificate EXPIRED;
    public static final Certificate UNSIGNED;

    static {
        HashMap<String,String> map = new HashMap<>();
        map.put("fingerprint", "UNKNOWN REQUEST");
        map.put("commonName", "An anonymous request");
        map.put("organization", "Unknown");
        map.put("validFrom", "0000-00-00 00:00:00");
        map.put("validTo", "0000-00-00 00:00:00");
        map.put("valid", "false");
        UNKNOWN = Certificate.loadCertificate(map);

        map.put("fingerprint", "EXPIRED REQUEST");
        map.put("commonName", ""); //filled in per request
        map.put("organization", ""); //filled in per request
        EXPIRED = Certificate.loadCertificate(map);

        map.put("fingerprint", "UNSIGNED REQUEST");
        UNSIGNED = Certificate.loadCertificate(map);
    }

    static {
        try {
            String overridePath;
            Properties trayProperties = PrintSocketServer.getTrayProperties();
            if (trayProperties != null && trayProperties.containsKey("authcert.override")) {
                overridePath = trayProperties.getProperty("authcert.override");
            } else {
                overridePath = System.getProperty("trustedRootCert");
            }
            if (overridePath != null) {
                try {
                    trustedRootCert = new Certificate(FileUtilities.readLocalFile(overridePath));
                    overrideTrustedRootCert = true;
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }

            if (trustedRootCert == null) {
                trustedRootCert = new Certificate("-----BEGIN CERTIFICATE-----\n" +
                                                          "MIIELzCCAxegAwIBAgIJALm151zCHDxiMA0GCSqGSIb3DQEBCwUAMIGsMQswCQYD\n" +
                                                          "VQQGEwJVUzELMAkGA1UECAwCTlkxEjAQBgNVBAcMCUNhbmFzdG90YTEbMBkGA1UE\n" +
                                                          "CgwSUVogSW5kdXN0cmllcywgTExDMRswGQYDVQQLDBJRWiBJbmR1c3RyaWVzLCBM\n" +
                                                          "TEMxGTAXBgNVBAMMEHF6aW5kdXN0cmllcy5jb20xJzAlBgkqhkiG9w0BCQEWGHN1\n" +
                                                          "cHBvcnRAcXppbmR1c3RyaWVzLmNvbTAgFw0xNTAzMDEyMzM4MjlaGA8yMTE1MDMw\n" +
                                                          "MjIzMzgyOVowgawxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJOWTESMBAGA1UEBwwJ\n" +
                                                          "Q2FuYXN0b3RhMRswGQYDVQQKDBJRWiBJbmR1c3RyaWVzLCBMTEMxGzAZBgNVBAsM\n" +
                                                          "ElFaIEluZHVzdHJpZXMsIExMQzEZMBcGA1UEAwwQcXppbmR1c3RyaWVzLmNvbTEn\n" +
                                                          "MCUGCSqGSIb3DQEJARYYc3VwcG9ydEBxemluZHVzdHJpZXMuY29tMIIBIjANBgkq\n" +
                                                          "hkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuWsBa6uk+RM4OKBZTRfIIyqaaFD71FAS\n" +
                                                          "7kojAQ+ySMpYuqLjIVZuCh92o1FGBvyBKUFc6knAHw5749yhLCYLXhzWwiNW2ri1\n" +
                                                          "Jwx/d83Wnaw6qA3lt++u3tmiA8tsFtss0QZW0YBpFsIqhamvB3ypwu0bdUV/oH7g\n" +
                                                          "/s8TFR5LrDfnfxlLFYhTUVWuWzMqEFAGnFG3uw/QMWZnQgkGbx0LMcYzdqFb7/vz\n" +
                                                          "rTSHfjJsisUTWPjo7SBnAtNYCYaGj0YH5RFUdabnvoTdV2XpA5IPYa9Q597g/M0z\n" +
                                                          "icAjuaK614nKXDaAUCbjki8RL3OK9KY920zNFboq/jKG6rKW2t51ZQIDAQABo1Aw\n" +
                                                          "TjAdBgNVHQ4EFgQUA0XGTcD6jqkL2oMPQaVtEgZDqV4wHwYDVR0jBBgwFoAUA0XG\n" +
                                                          "TcD6jqkL2oMPQaVtEgZDqV4wDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\n" +
                                                          "AQEAijcT5QMVqrWWqpNEe1DidzQfSnKo17ZogHW+BfUbxv65JbDIntnk1XgtLTKB\n" +
                                                          "VAdIWUtGZbXxrp16NEsh96V2hjDIoiAaEpW+Cp6AHhIVgVh7Q9Knq9xZ1t6H8PL5\n" +
                                                          "QiYQKQgJ0HapdCxlPKBfUm/Mj1ppNl9mPFJwgHmzORexbxrzU/M5i2jlies+CXNq\n" +
                                                          "cvmF2l33QNHnLwpFGwYKs08pyHwUPp6+bfci6lRvavztgvnKroWWIRq9ZPlC0yVK\n" +
                                                          "FFemhbCd7ZVbrTo0NcWZM1PTAbvlOikV9eh3i1Vot+3dJ8F27KwUTtnV0B9Jrxum\n" +
                                                          "W9P3C48mvwTxYZJFOu0N9UBLLg==\n" +
                                                          "-----END CERTIFICATE-----");

                CRL.getInstance();  // Fetch the CRL
            }

            trustedRootCert.valid = true;
            log.debug("Using trusted root certificate: CN={}, O={} ({})",
                      trustedRootCert.getCommonName(), trustedRootCert.getOrganization(), trustedRootCert.getFingerprint());
        }
        catch(CertificateParsingException e) {
            e.printStackTrace();
        }
    }


    /** Decodes a certificate and intermediate certificate from the given string */
    @SuppressWarnings("deprecation")
    public Certificate(String in) throws CertificateParsingException {
        try {
            //Setup X.509
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            //Strip beginning and end
            String[] split = in.split("--START INTERMEDIATE CERT--");
            byte[] serverCertificate = Base64.decode(split[0].replaceAll(X509Constants.BEGIN_CERT, "").replaceAll(X509Constants.END_CERT, ""));

            X509Certificate theIntermediateCertificate;
            if (split.length == 2) {
                byte[] intermediateCertificate = Base64.decode(split[1].replaceAll(X509Constants.BEGIN_CERT, "").replaceAll(X509Constants.END_CERT, ""));
                theIntermediateCertificate = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(intermediateCertificate));
            } else {
                theIntermediateCertificate = null; //Self-signed
            }

            //Generate cert
            theCertificate = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(serverCertificate));
            commonName = String.valueOf(PrincipalUtil.getSubjectX509Principal(theCertificate).getValues(X509Name.CN).get(0));
            fingerprint = makeThumbPrint(theCertificate);
            organization = String.valueOf(PrincipalUtil.getSubjectX509Principal(theCertificate).getValues(X509Name.O).get(0));
            validFrom = theCertificate.getNotBefore();
            validTo = theCertificate.getNotAfter();

            if (trustedRootCert != null) {
                HashSet<X509Certificate> chain = new HashSet<>();
                try {
                    chain.add(trustedRootCert.theCertificate);
                    if (theIntermediateCertificate != null) { chain.add(theIntermediateCertificate); }
                    X509Certificate[] x509Certificates = X509CertificateChainBuilder.buildPath(theCertificate, chain);

                    for(X509Certificate x509Certificate : x509Certificates) {
                        if (x509Certificate.equals(trustedRootCert.theCertificate)) {
                            Date now = new Date();
                            valid = (getValidFromDate().compareTo(now) <= 0) && (getValidToDate().compareTo(now) > 0);
                        }
                    }
                }
                catch(Exception e) {
                    log.error("Problem building certificate chain", e);
                }
            }

            // Only do CRL checks on QZ-issued certificates
            if (trustedRootCert != null && !overrideTrustedRootCert) {
                CRL qzCrl = CRL.getInstance();
                if (qzCrl.isLoaded()) {
                    if (qzCrl.isRevoked(getFingerprint()) || theIntermediateCertificate == null || qzCrl.isRevoked(makeThumbPrint(theIntermediateCertificate))) {
                        log.warn("Problem verifying certificate with CRL");
                        valid = false;
                    }
                } else {
                    //Assume nothing is revoked, because we can't get the CRL
                    log.warn("Failed to retrieve QZ CRL, skipping CRL check");
                }
            }
        }
        catch(Exception e) {
            CertificateParsingException certificateParsingException = new CertificateParsingException();
            certificateParsingException.initCause(e);
            throw certificateParsingException;
        }
    }

    private Certificate() {}


    /**
     * Used to rebuild a certificate for the 'Saved Sites' screen without having to decrypt the certificates again
     */
    public static Certificate loadCertificate(HashMap<String,String> data) {
        Certificate cert = new Certificate();

        cert.fingerprint = data.get("fingerprint");
        cert.commonName = data.get("commonName");
        cert.organization = data.get("organization");

        try {
            cert.validFrom = cert.dateFormat.parse(data.get("validFrom"));
            cert.validTo = cert.dateFormat.parse(data.get("validTo"));
        }
        catch(ParseException e) {
            cert.validFrom = new Date(0);
            cert.validTo = new Date(0);

            log.error("Unable to parse certificate date", e);
        }

        cert.valid = Boolean.parseBoolean(data.get("valid"));

        return cert;
    }

    /**
     * Copies the company information from a valid certificate to show on an invalid signature certificate.
     *
     * @param copyFrom The valid certificate that failed a signature check
     */
    public void adjustStaticCertificate(Certificate copyFrom) {
        if (this != UNSIGNED && this != EXPIRED) {
            throw new UnsupportedOperationException("Cannot adjust a non-static certificate's values");
        }

        commonName = copyFrom.commonName;
        organization = copyFrom.organization;

        validFrom = copyFrom.validFrom;
        validTo = copyFrom.validTo;
        valid = false; //this is bad request, so never trusted
    }

    /**
     * Checks given signature for given data against this certificate,
     * ensuring it is properly signed
     *
     * @param signature the signature appended to the data, base64 encoded
     * @param data      the data to check
     * @return true if signature valid, false if not
     */
    public boolean isSignatureValid(String signature, String data) {
        if (!signature.isEmpty()) {
            RSATool tool = RSAToolFactory.getRSATool();
            RSAKey thePublicKey = new RSAKeyImpl(theCertificate.getPublicKey());

            //On errors, assume failure.
            try {
                String hash = DigestUtils.sha256Hex(data);
                return tool.verifyWithKey(StringUtils.getBytesUtf8(hash), Base64.decode(signature), thePublicKey);
            }
            catch(Exception e) {
                log.error("Unable to verify signature", e);
            }
        }

        return false;
    }

    /** Checks if the certificate has been added to the local trusted store */
    public boolean isSaved() {
        File allowed = FileUtilities.getFile(Constants.ALLOW_FILE);
        return existsInFile(allowed);
    }

    /** Checks if the certificate has been added to the local blocked store */
    public boolean isBlocked() {
        File blocks = FileUtilities.getFile(Constants.BLOCK_FILE);
        return existsInFile(blocks);
    }

    private boolean existsInFile(File file) {
        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                String print = line.substring(0, line.indexOf("\t"));
                if (print.equals(getFingerprint())) {
                    return true;
                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        return false;
    }


    public String getFingerprint() {
        return fingerprint;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getOrganization() {
        return organization;
    }

    public String getValidFrom() {
        return dateFormat.format(validFrom);
    }

    public String getValidTo() {
        return dateFormat.format(validTo);
    }

    public Date getValidFromDate() {
        return validFrom;
    }

    public Date getValidToDate() {
        return validTo;
    }

    /**
     * Validates certificate against embedded cert.
     */
    public boolean isTrusted() {
        return valid;
    }


    public static String makeThumbPrint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(cert.getEncoded());
        return ByteUtilities.bytesToHex(md.digest(), false);
    }

    public String data() {
        return getFingerprint() + "\t" +
                getCommonName() + "\t" +
                getOrganization() + "\t" +
                getValidFrom() + "\t" +
                getValidTo() + "\t" +
                isTrusted();
    }

    @Override
    public String toString() {
        return getOrganization() + " (" + getCommonName() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Certificate) {
            return ((Certificate)obj).data().equals(data());
        }
        return super.equals(obj);
    }
}
