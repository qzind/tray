package qz.auth;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.ssl.Base64;
import org.apache.commons.ssl.X509CertificateChainBuilder;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.App;
import qz.common.Constants;
import qz.utils.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.*;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by Steven on 1/27/2015. Package: qz.auth Project: qz-print
 * Wrapper to store certificate objects from
 */
public class Certificate {

    private static final Logger log = LogManager.getLogger(Certificate.class);
    private static final String QUIETLY_FAIL = "quiet";

    public enum Algorithm {
        SHA1("SHA1withRSA"),
        SHA256("SHA256withRSA"),
        SHA512("SHA512withRSA");

        String name;

        Algorithm(String name) {
            this.name = name;
        }
    }

    public static ArrayList<Certificate> rootCAs = new ArrayList<>();
    public static Certificate builtIn;
    private static CertPathValidator validator;
    private static CertificateFactory factory;
    private static boolean trustBuiltIn = false;
    // id-at-description used for storing renewal information
    private static ASN1ObjectIdentifier RENEWAL_OF = new ASN1ObjectIdentifier("2.5.4.13");

    public static final String[] saveFields = new String[] {"fingerprint", "commonName", "organization", "validFrom", "validTo", "valid"};

    public static final String SPONSORED_CN_PREFIX = "Sponsored:";

    // Valid date range allows UI to only show "Expired" text for valid certificates
    private static final Instant UNKNOWN_MIN = LocalDateTime.MIN.toInstant(ZoneOffset.UTC);
    private static final Instant UNKNOWN_MAX = LocalDateTime.MAX.toInstant(ZoneOffset.UTC);
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_PARSE = DateTimeFormatter.ofPattern("uuuu-MM-dd['T'][ ]HH:mm:ss[.n]['Z']"); //allow parsing of both ISO and custom formatted dates

    private X509Certificate theCertificate;
    private boolean sponsored;
    private String fingerprint;
    private String commonName;
    private String organization;
    private Instant validFrom;
    private Instant validTo;

    //used by review sites UI only
    private boolean expired = false;
    private boolean valid = false;
    private boolean rootCA = false; // TODO: Move to constructor?


    //Pre-set certificate for use when missing
    public static final Certificate UNKNOWN;

    static {
        HashMap<String,String> map = new HashMap<>();
        map.put("fingerprint", "UNKNOWN REQUEST");
        map.put("commonName", "An anonymous request");
        map.put("organization", "Unknown");
        map.put("validFrom", UNKNOWN_MIN.toString());
        map.put("validTo", UNKNOWN_MAX.toString());
        map.put("valid", "false");
        UNKNOWN = Certificate.loadCertificate(map);
    }

    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
            validator = CertPathValidator.getInstance("PKIX");
            factory = CertificateFactory.getInstance("X.509");
            builtIn = new Certificate("-----BEGIN CERTIFICATE-----\n" +
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

            builtIn.valid = true;
            setTrustBuiltIn(true);
            scanAdditionalCAs();
        }
        catch(NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
    }

    public static void scanAdditionalCAs() {
        ArrayList<Map.Entry<Path, String>> certPaths = new ArrayList<>();
        // First, look for "authcert.override", "-DtrustedRootCert"
        certPaths.addAll(FileUtilities.parseDelimitedPaths(PrefsSearch.getString(ArgValue.AUTHCERT_OVERRIDE, App.getTrayProperties())));

        // Second, look for "override.crt" within App directory
        certPaths.add(new AbstractMap.SimpleEntry<>(SystemUtilities.getJarParentPath().resolve(Constants.OVERRIDE_CERT), QUIETLY_FAIL));

        for(Map.Entry<Path, String> certPath : certPaths) {
            if(certPath.getKey() != null) {
                if (certPath.getKey().toFile().exists()) {
                    try {
                        Certificate caCert = new Certificate(FileUtilities.readLocalFile(certPath.getKey()));
                        caCert.rootCA = true;
                        caCert.valid = true;
                        if(!rootCAs.contains(caCert)) {
                            log.debug("Adding CA certificate: CN={}, O={} ({})",
                                      caCert.getCommonName(), caCert.getOrganization(), caCert.getFingerprint());
                            rootCAs.add(caCert);
                        } else {
                            log.warn("CA cert exists, skipping: {}", certPath.getKey());
                        }
                    }
                    catch(Exception e) {
                        log.error("Error loading CA cert: {}", certPath.getKey(), e);
                    }
                } else if(!certPath.getValue().equals(QUIETLY_FAIL)) {
                    log.warn("CA cert \"{}\" was provided, but could not be found, skipping.", certPath.getKey());
                }
            }
        }
    }

    public Certificate(Path path) throws IOException, CertificateException {
        this(new String(Files.readAllBytes(path), Charsets.UTF_8));
    }

    /** Decodes a certificate and intermediate certificate from the given string */
    public Certificate(String in) throws CertificateException {
        try {
            //Strip beginning and end
            String[] split = in.split("--START INTERMEDIATE CERT--");
            byte[] serverCertificate = Base64.decodeBase64(split[0].replaceAll(X509Constants.BEGIN_CERT, "").replaceAll(X509Constants.END_CERT, ""));

            X509Certificate theIntermediateCertificate;
            if (split.length == 2) {
                byte[] intermediateCertificate = Base64.decodeBase64(split[1].replaceAll(X509Constants.BEGIN_CERT, "").replaceAll(X509Constants.END_CERT, ""));
                theIntermediateCertificate = (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(intermediateCertificate));
            } else {
                theIntermediateCertificate = null; //Self-signed
            }

            //Generate cert
            theCertificate = (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(serverCertificate));
            commonName = getSubjectX509Principal(theCertificate, BCStyle.CN);
            if(commonName.isEmpty()) {
                throw new CertificateException("Common Name cannot be blank.");
            }
            // Remove "Sponsored: " from CN, we'll swap the trusted icon instead <3
            if(commonName.startsWith(SPONSORED_CN_PREFIX)) {
                commonName = commonName.split(SPONSORED_CN_PREFIX)[1].trim();
                sponsored = true;
            } else {
                sponsored = false;
            }
            fingerprint = makeThumbPrint(theCertificate);
            organization = getSubjectX509Principal(theCertificate, BCStyle.O);
            validFrom = theCertificate.getNotBefore().toInstant();
            validTo = theCertificate.getNotAfter().toInstant();

            // Check trust anchor against all root certs
            Certificate foundRoot = null;
            if(!this.rootCA) {
                for(Certificate rootCA : rootCAs) {
                    HashSet<X509Certificate> chain = new HashSet<>();
                    try {
                        chain.add(rootCA.theCertificate);
                        if (theIntermediateCertificate != null) { chain.add(theIntermediateCertificate); }
                        X509Certificate[] x509Certificates = X509CertificateChainBuilder.buildPath(theCertificate, chain);

                        Set<TrustAnchor> anchor = new HashSet<>();
                        anchor.add(new TrustAnchor(rootCA.theCertificate, null));
                        PKIXParameters params = new PKIXParameters(anchor);
                        params.setRevocationEnabled(false); // TODO: Re-enable, remove proprietary CRL
                        validator.validate(factory.generateCertPath(Arrays.asList(x509Certificates)), params);
                        foundRoot = rootCA;
                        valid = true;
                        log.debug("Successfully chained certificate: CN={}, O={} ({})", getCommonName(), getOrganization(), getFingerprint());
                        break; // if successful, don't attempt another chain
                    }
                    catch(Exception e) {
                        log.warn("Problem building certificate chain (normal if multiple CAs are in use)");
                    }
                }
            }

            // Check for expiration
            Instant now = Instant.now();
            if (expired = (validFrom.isAfter(now) || validTo.isBefore(now))) {
                log.warn("Certificate is expired: CN={}, O={} ({})", getCommonName(), getOrganization(), getFingerprint());
                valid = false;
            }

            // If cert matches a rootCA trust it blindly
            // If cert is chained to a 3rd party rootCA, trust it blindly as well
            Iterator<Certificate> allCerts = rootCAs.iterator();
            while(allCerts.hasNext()) {
                Certificate cert = allCerts.next();
                if(cert.equals(this) || (cert.equals(foundRoot) && !cert.equals(builtIn))) {
                    log.debug("Adding {} to {} list", cert.toString(), Constants.ALLOW_FILE);
                    if(!isSaved()) {
                        FileUtilities.printLineToFile(Constants.ALLOW_FILE, data());
                    }
                    valid = true;
                    break;
                }
            }

            readRenewalInfo();
            CRL qzCrl = CRL.getInstance();
            if (qzCrl.isLoaded()) {
                if (qzCrl.isRevoked(getFingerprint()) || (theIntermediateCertificate != null && qzCrl.isRevoked(makeThumbPrint(theIntermediateCertificate)))) {
                    log.error("Certificate has been revoked and can no longer be used: CN={}, O={} ({})", getCommonName(), getOrganization(), getFingerprint());
                    valid = false;
                }
            } else {
                //Assume nothing is revoked, because we can't get the CRL
                log.warn("Failed to retrieve QZ CRL, skipping CRL check");
            }
        }
        catch(Exception e) {
            CertificateException certificateException = new CertificateException();
            certificateException.initCause(e);
            throw certificateException;
        }
    }

    private void readRenewalInfo() throws Exception {
        Vector values = PrincipalUtil.getSubjectX509Principal(theCertificate).getValues(RENEWAL_OF);
        Iterator renewals = values.iterator();

        while(renewals.hasNext()) {
            String renewalInfo = String.valueOf(renewals.next());

            String renewalPrefix = "renewal-of-";
            if (!renewalInfo.startsWith(renewalPrefix)) {
                log.warn("Malformed renewal info: {}", renewalInfo);
                continue;
            }
            String previousFingerprint = renewalInfo.substring(renewalPrefix.length());
            if (previousFingerprint.length() != 40) {
                log.warn("Malformed renewal fingerprint: {}", previousFingerprint);
                continue;
            }

            // Add this certificate to the whitelist if the previous certificate was whitelisted

            // First, handle shared directory
            File sharedFile = FileUtilities.getFile(Constants.ALLOW_FILE, false);
            if (existsInAnyFile(previousFingerprint, sharedFile) && !isSaved(false)) {
                if(!FileUtilities.printLineToFile(Constants.ALLOW_FILE, data(), false)) {
                    // Fallback to local directory if shared is not writable
                    FileUtilities.printLineToFile(Constants.ALLOW_FILE, data(), /* fallback */ true);
                }
            }

            // Second, handle local directory
            File localFile = FileUtilities.getFile(Constants.ALLOW_FILE, true);
            if (existsInAnyFile(previousFingerprint, localFile) && !isSaved(true)) {
                FileUtilities.printLineToFile(Constants.ALLOW_FILE, data(), true);
            }
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
            cert.validFrom = Instant.from(LocalDateTime.from(DATE_PARSE.parse(data.get("validFrom"))).atZone(ZoneOffset.UTC));
            cert.validTo = Instant.from(LocalDateTime.from(DATE_PARSE.parse(data.get("validTo"))).atZone(ZoneOffset.UTC));
        }
        catch(DateTimeException e) {
            cert.validFrom = UNKNOWN_MIN;
            cert.validTo = UNKNOWN_MAX;

            log.warn("Unable to parse certificate date: {}", e.getMessage());
        }

        cert.valid = Boolean.parseBoolean(data.get("valid"));

        return cert;
    }

    /**
     * Checks given signature for given data against this certificate,
     * ensuring it is properly signed
     *
     * @param signature the signature appended to the data, base64 encoded
     * @param data      the data to check
     * @return true if signature valid, false if not
     */
    public boolean isSignatureValid(Algorithm algorithm, String signature, String data) {
        if (!signature.isEmpty()) {
            //On errors, assume failure.
            try {
                Signature verifier = Signature.getInstance(algorithm.name);
                verifier.initVerify(theCertificate.getPublicKey());
                verifier.update(StringUtils.getBytesUtf8(DigestUtils.sha256Hex(data)));

                return verifier.verify(Base64.decodeBase64(signature));
            }
            catch(GeneralSecurityException e) {
                log.error("Unable to verify signature", e);
            }
        }

        return false;
    }

    /** Checks if the certificate has been added to the specified allow file */
    public boolean isSaved(boolean local) {
        File allowed = FileUtilities.getFile(Constants.ALLOW_FILE, local);
        return existsInAnyFile(getFingerprint(), allowed);
    }

    /** Checks if the certificate has been added to any allow file */
    public boolean isSaved() {
        return isSaved(false) || isSaved(true);
    }

    /** Checks if the certificate has been added to the local block file */
    public boolean isBlocked() {
        File blocks = FileUtilities.getFile(Constants.BLOCK_FILE, true);
        File blocksShared = FileUtilities.getFile(Constants.BLOCK_FILE, false);
        return existsInAnyFile(getFingerprint(), blocksShared, blocks);
    }

    private static boolean existsInAnyFile(String fingerprint, File... files) {
        for(File file : files) {
            if (file == null) { continue; }

            try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while((line = br.readLine()) != null) {
                    if (line.contains("\t")) {
                        String print = line.substring(0, line.indexOf("\t"));
                        if (print.equals(fingerprint)) {
                            return true;
                        }
                    }
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
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
        if (validFrom.isAfter(UNKNOWN_MIN)) {
            return DATE_FORMAT.format(validFrom.atZone(ZoneOffset.UTC));
        } else {
            return "Not Provided";
        }
    }

    public String getValidTo() {
        if (validTo.isBefore(UNKNOWN_MAX)) {
            return DATE_FORMAT.format(validTo.atZone(ZoneOffset.UTC));
        } else {
            return "Not Provided";
        }
    }

    public Instant getValidFromDate() {
        return validFrom;
    }

    public Instant getValidToDate() {
        return validTo;
    }

    /**
     * Validates certificate against embedded cert.
     */
    public boolean isTrusted() {
        return isValid() && !isExpired();
    }

    public boolean isSponsored() {
        return sponsored;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isExpired() {
        return expired;
    }


    public static String makeThumbPrint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(cert.getEncoded());
        return ByteUtilities.toHexString(md.digest(), false);
    }

    private String data(boolean assumeTrusted) {
        return getFingerprint() + "\t" +
                getCommonName() + "\t" +
                getOrganization() + "\t" +
                getValidFrom() + "\t" +
                getValidTo() + "\t" +
                // Used by equals(), may fail if it hasn't been trusted yet
                (assumeTrusted ? true : isTrusted());
    }

    public String data() {
        return data(false);
    }

    @Override
    public String toString() {
        return getOrganization() + " (" + getCommonName() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Certificate) {
            return ((Certificate)obj).data(true).equals(data(true));
        }
        return super.equals(obj);
    }

    public static void setTrustBuiltIn(boolean trustBuiltIn) {
        if(trustBuiltIn) {
            if (!rootCAs.contains(builtIn)) {
                log.debug("Adding internal CA certificate: CN={}, O={} ({})",
                          builtIn.getCommonName(), builtIn.getOrganization(), builtIn.getFingerprint());
                builtIn.rootCA = true;
                builtIn.valid = true;
                rootCAs.add(0, builtIn);
            }
        } else {
            if (rootCAs.contains(builtIn)) {
                log.debug("Removing internal CA certificate: CN={}, O={} ({})",
                          builtIn.getCommonName(), builtIn.getOrganization(), builtIn.getFingerprint());
                rootCAs.remove(builtIn);
            }
        }
        Certificate.trustBuiltIn = trustBuiltIn;
    }

    public static boolean isTrustBuiltIn() {
        return trustBuiltIn;
    }

    public static boolean hasAdditionalCAs() {
        return rootCAs.size() > (isTrustBuiltIn() ? 1 : 0);
    }

    private static String getSubjectX509Principal(X509Certificate cert, ASN1ObjectIdentifier key) {
        try {
            Vector v = PrincipalUtil.getSubjectX509Principal(cert).getValues(key);
            if(v.size() > 0) {
                return String.valueOf(v.get(0));
            }
        } catch(CertificateEncodingException e) {
            log.warn("Certificate encoding exception occurred", e);
        }
        return "";
    }

}
