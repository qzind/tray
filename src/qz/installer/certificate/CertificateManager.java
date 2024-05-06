/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer.certificate;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorException;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.installer.Installer;
import qz.utils.ArgValue;
import qz.utils.FileUtilities;
import qz.utils.MacUtilities;
import qz.utils.SystemUtilities;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import static qz.utils.FileUtilities.*;
import static qz.installer.certificate.KeyPairWrapper.Type.*;

/**
 * Stores and maintains reading and writing of certificate related files
 */
public class CertificateManager {
    static List<Path> SAVE_LOCATIONS = new ArrayList<>();
    static {
        // Workaround for JDK-8266929
        // See also https://github.com/qzind/tray/issues/814
        SystemUtilities.clearAlgorithms();

        // Skip shared location if running from IDE or build directory
        // Prevents corrupting the version installed per https://github.com/qzind/tray/issues/1200
        if(SystemUtilities.isJar() && SystemUtilities.isInstalled()) {
            // Skip install location if running from sandbox (must remain sealed)
            if(!SystemUtilities.isMac() || !MacUtilities.isSandboxed()) {
                SAVE_LOCATIONS.add(SystemUtilities.getJarParentPath());
            }
            SAVE_LOCATIONS.add(SHARED_DIR);
        }
        SAVE_LOCATIONS.add(USER_DIR);
    }
    private static final Logger log = LogManager.getLogger(CertificateManager.class);

    public static String DEFAULT_KEYSTORE_FORMAT = "PKCS12";
    public static String DEFAULT_KEYSTORE_EXTENSION = ".p12";
    public static String DEFAULT_CERTIFICATE_EXTENSION = ".crt";
    private static int DEFAULT_PASSWORD_BITS = 100;

    private boolean needsInstall;
    private SslContextFactory.Server sslContextFactory;
    private KeyPairWrapper sslKeyPair;
    private KeyPairWrapper caKeyPair;

    private Properties properties;
    private char[] password;

    /**
     * For internal certs
     */
    public CertificateManager(boolean forceNew, String ... hostNames) throws IOException, GeneralSecurityException, OperatorException {
        Security.addProvider(new BouncyCastleProvider());
        sslKeyPair = new KeyPairWrapper(SSL);
        caKeyPair = new KeyPairWrapper(CA);

        if (!forceNew) {
            // order is important: ssl, ca
            properties = loadProperties(sslKeyPair, caKeyPair);
        }

        if(properties == null) {
            log.warn("Warning, SSL properties won't be loaded from disk... we'll try to create them...");

            CertificateChainBuilder cb = new CertificateChainBuilder(hostNames);
            caKeyPair = cb.createCaCert();
            sslKeyPair = cb.createSslCert(caKeyPair);

            // Create CA
            properties = createKeyStore(CA)
                    .writeCert(CA)
                    .writeKeystore(null, CA);

            // Create SSL
            properties = createKeyStore(SSL)
                    .writeCert(SSL)
                    .writeKeystore(properties, SSL);

            // Save properties
            saveProperties();
        }
    }

    /**
     * For trusted PEM-formatted certs
     */
    public CertificateManager(File trustedPemKey, File trustedPemCert) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        needsInstall = false;
        sslKeyPair = new KeyPairWrapper(SSL);

        // Assumes ssl/privkey.pem, ssl/fullchain.pem
        properties = createTrustedKeystore(trustedPemKey, trustedPemCert)
                .writeKeystore(properties, SSL);

        // Save properties
        saveProperties();
    }

    /**
     * For trusted PKCS12-formatted certs
     */
    public CertificateManager(File pkcs12File, char[] password) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        needsInstall = false;
        sslKeyPair = new KeyPairWrapper(SSL);

        // Assumes direct pkcs12 import
        this.password = password;
        sslKeyPair.init(pkcs12File, password);

        // Save it back, but to a location we can find
        properties = writeKeystore(null, SSL);

        // Save properties
        saveProperties();
    }

    public void renewCertChain(String ... hostNames) throws Exception {
        CertificateChainBuilder cb = new CertificateChainBuilder(hostNames);
        sslKeyPair = cb.createSslCert(caKeyPair);
        createKeyStore(SSL).writeKeystore(properties, SSL);
        reloadSslContextFactory();
    }

    public KeyPairWrapper getSslKeyPair() {
        return sslKeyPair;
    }

    public KeyPairWrapper getCaKeyPair() {
        return caKeyPair;
    }

    public KeyPairWrapper getKeyPair(KeyPairWrapper.Type type) {
        switch(type) {
            case SSL:
                return sslKeyPair;
            case CA:
            default:
                return caKeyPair;
        }
    }

    public KeyPairWrapper getKeyPair(String alias) {
        for(KeyPairWrapper.Type type : KeyPairWrapper.Type.values()) {
            if (KeyPairWrapper.getAlias(type).equalsIgnoreCase(alias)) {
                return getKeyPair(type);
            }
        }
        return getKeyPair(KeyPairWrapper.Type.CA);
    }

    public Properties getProperties() {
        return properties;
    }

    private char[] getPassword() {
        if (password == null) {
            if(caKeyPair != null && caKeyPair.getPassword() != null) {
                // Reuse existing
                password = caKeyPair.getPassword();
            } else {
                // Create new
                BigInteger bi = new BigInteger(DEFAULT_PASSWORD_BITS, new SecureRandom());
                password = bi.toString(16).toCharArray();
                log.info("Created a random {} bit password: {}", DEFAULT_PASSWORD_BITS, new String(password));
            }
        }
        return password;
    }

    public SslContextFactory.Server configureSslContextFactory() {
        sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStore(sslKeyPair.getKeyStore());
        sslContextFactory.setKeyStorePassword(sslKeyPair.getPasswordString());
        sslContextFactory.setKeyManagerPassword(sslKeyPair.getPasswordString());
        return sslContextFactory;
    }

    public void reloadSslContextFactory() throws Exception {
        if(isSslActive()) {
            sslContextFactory.reload(sslContextFactory -> {
                sslContextFactory.setKeyStore(sslKeyPair.getKeyStore());
                sslContextFactory.setKeyStorePassword(sslKeyPair.getPasswordString());
                sslContextFactory.setKeyManagerPassword(sslKeyPair.getPasswordString());
            });
        } else {
            log.warn("SSL isn't active, can't reload");
        }
    }

    public boolean isSslActive() {
        return sslContextFactory != null;
    }

    public boolean needsInstall() {
        return needsInstall;
    }

    public CertificateManager createKeyStore(KeyPairWrapper.Type type) throws IOException, GeneralSecurityException {
        KeyPairWrapper keyPair = type == CA ? caKeyPair : sslKeyPair;
        KeyStore keyStore = KeyStore.getInstance(DEFAULT_KEYSTORE_FORMAT);
        keyStore.load(null, password);

        List<X509Certificate> chain = new ArrayList<>();
        chain.add(keyPair.getCert());

        // Add ca to ssl cert chain
        if (keyPair.getType() == SSL) {
            chain.add(caKeyPair.getCert());
        }
        keyStore.setEntry(caKeyPair.getAlias(), new KeyStore.TrustedCertificateEntry(caKeyPair.getCert()), null);
        keyStore.setKeyEntry(keyPair.getAlias(), keyPair.getKey(), getPassword(), chain.toArray(new X509Certificate[chain.size()]));
        keyPair.init(keyStore, getPassword());
        return this;
    }

    public CertificateManager createTrustedKeystore(File p12Store, String password) throws Exception {
        sslKeyPair = new KeyPairWrapper(SSL);
        sslKeyPair.init(p12Store, password.toCharArray());
        return this;
    }

    public CertificateManager createTrustedKeystore(File pemKey, File pemCert) throws Exception {
        sslKeyPair = new KeyPairWrapper(SSL);

        // Private Key
        PEMParser pem = new PEMParser(new FileReader(pemKey));
        Object parsedObject = pem.readObject();

        PrivateKeyInfo privateKeyInfo = parsedObject instanceof PEMKeyPair ? ((PEMKeyPair)parsedObject).getPrivateKeyInfo() : (PrivateKeyInfo)parsedObject;
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyInfo.getEncoded());
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PrivateKey key = factory.generatePrivate(privateKeySpec);

        List<X509Certificate> certs = new ArrayList<>();
        X509CertificateHolder certHolder = (X509CertificateHolder)pem.readObject();
        if(certHolder != null) {
            certs.add(new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder));
        }

        // Certificate
        pem = new PEMParser(new FileReader(pemCert));
        while((certHolder = (X509CertificateHolder)pem.readObject()) != null) {
            certs.add(new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder));
        }

        // Keystore
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null);

        for (int i = 0; i < certs.size(); i++) {
            ks.setCertificateEntry(sslKeyPair.getAlias() + "_" + i, certs.get(i));
        }

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null);
        keyStore.setKeyEntry(sslKeyPair.getAlias(), key, getPassword(), certs.toArray(new X509Certificate[certs.size()]));

        sslKeyPair.init(keyStore, getPassword());
        return this;
    }

    public static void writeCert(X509Certificate data, File dest) throws IOException {
        // PEMWriter doesn't always clear the file, explicitly delete it, see issue #796
        if(dest.exists()) {
            dest.delete();
        }
        JcaMiscPEMGenerator cert = new JcaMiscPEMGenerator(data);
        JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(Files.newOutputStream(dest.toPath(), StandardOpenOption.CREATE)));
        writer.writeObject(cert.generate());
        writer.close();
        FileUtilities.inheritParentPermissions(dest.toPath());
        log.info("Wrote Cert: \"{}\"", dest);
    }

    public CertificateManager writeCert(KeyPairWrapper.Type type) throws IOException {
        KeyPairWrapper keyPair = type == CA ? caKeyPair : sslKeyPair;
        File certFile = new File(getWritableLocation("ssl"), keyPair.getAlias() + DEFAULT_CERTIFICATE_EXTENSION);

        writeCert(keyPair.getCert(), certFile);
        FileUtilities.inheritParentPermissions(certFile.toPath());
        if(keyPair.getType() == CA) {
            needsInstall = true;
        }
        return this;
    }

    public Properties writeKeystore(Properties props, KeyPairWrapper.Type type) throws GeneralSecurityException, IOException {
        File sslDir = getWritableLocation("ssl");
        KeyPairWrapper keyPair = type == CA ? caKeyPair : sslKeyPair;

        File keyFile = new File(sslDir, keyPair.getAlias() + DEFAULT_KEYSTORE_EXTENSION);
        keyPair.getKeyStore().store(Files.newOutputStream(keyFile.toPath(), StandardOpenOption.CREATE), getPassword());
        FileUtilities.inheritParentPermissions(keyFile.toPath());
        log.info("Wrote {} Key: \"{}\"", DEFAULT_KEYSTORE_FORMAT, keyFile);

        if (props == null) {
            props = new Properties();
        }
        props.putIfAbsent(String.format("%s.keystore", keyPair.propsPrefix()), keyFile.toString());
        props.putIfAbsent(String.format("%s.storepass", keyPair.propsPrefix()), new String(getPassword()));
        props.putIfAbsent(String.format("%s.alias", keyPair.propsPrefix()), keyPair.getAlias());

        if (keyPair.getType() == SSL) {
            props.putIfAbsent(String.format("%s.host", keyPair.propsPrefix()), ArgValue.SECURITY_WSS_HOST.getDefaultVal());
        }


        return props;
    }

    public static File getWritableLocation(String ... suffixes) throws IOException {
        // Get an array of preferred directories
        ArrayList<Path> locs = new ArrayList<>();

        if (suffixes.length == 0) {
            locs.addAll(SAVE_LOCATIONS);
            // Last, fallback on a directory we won't ever see again :/
            locs.add(TEMP_DIR);
        } else {
            // Same as above, but with suffixes added (usually "ssl"), skipping the install location
            for(Path saveLocation : SAVE_LOCATIONS) {
                if(!saveLocation.equals(SystemUtilities.getJarParentPath())) {
                    locs.add(Paths.get(saveLocation.toString(), suffixes));
                }
            }
            // Last, fallback on a directory we won't ever see again :/
            locs.add(Paths.get(TEMP_DIR.toString(), suffixes));
        }

        // Find a suitable write location
        File path;
        for(Path loc : locs) {
            if (loc == null) continue;
            boolean isPreferred = locs.indexOf(loc) == 0;
            path = loc.toFile();
            path.mkdirs();
            if (path.canWrite()) {
                log.debug("Writing to {}", loc);
                if(!isPreferred) {
                    log.warn("Warning, {} isn't the preferred write location, but we'll use it anyway", loc);
                }
                return path;
            } else {
                log.debug("Can't write to {}, trying the next...", loc);
            }
        }
        throw new IOException("Can't find a suitable write location.  SSL will fail.");
    }

    public static Properties loadProperties(KeyPairWrapper... keyPairs) {
        log.info("Try to find SSL properties file...");


        Properties props = null;
        for(Path loc : SAVE_LOCATIONS) {
            if (loc == null) continue;
            try {
                for(KeyPairWrapper keyPair : keyPairs) {
                    props = loadKeyPair(keyPair, loc, props);
                }
                // We've loaded without Exception, return
                log.info("Found {}/{}.properties", loc, Constants.PROPS_FILE);
                return props;
            } catch(Exception ignore) {
                log.warn("Properties couldn't be loaded at {}, trying fallback...", loc, ignore);
            }
        }
        log.info("Could not get SSL properties from file.");
        return null;
    }

    public static Properties loadKeyPair(KeyPairWrapper keyPair, Path parent, Properties existing) throws Exception {
        Properties props;

        if (existing == null) {
            FileInputStream fis = null;
            try {
                props = new Properties();
                props.load(fis = new FileInputStream(new File(parent.toFile(), Constants.PROPS_FILE + ".properties")));
            } finally {
                if(fis != null) fis.close();
            }
        } else {
            props = existing;
        }

        String ks = props.getProperty(String.format("%s.keystore", keyPair.propsPrefix()));
        String pw = props.getProperty(String.format("%s.storepass", keyPair.propsPrefix()), "");

        if(ks == null || ks.trim().isEmpty()) {
            if(keyPair.getType() == SSL) {
                throw new IOException("Missing wss.keystore entry");
            } else {
                // CA is only needed for internal certs, return
                return props;
            }
        }
        File ksFile = Paths.get(ks).isAbsolute()? new File(ks):new File(parent.toFile(), ks);
        if (ksFile.exists()) {
            keyPair.init(ksFile, pw.toCharArray());
            return props;
        }
        return null;
    }

    private void saveProperties() throws IOException {
        File propsFile = new File(getWritableLocation(), Constants.PROPS_FILE + ".properties");
        Installer.persistProperties(propsFile, properties); // checks for props from previous install
        properties.store(new FileOutputStream(propsFile), null);
        FileUtilities.inheritParentPermissions(propsFile.toPath());
        log.info("Successfully created SSL properties file: {}", propsFile);
    }

    public static boolean emailMatches(X509Certificate cert) {
        return emailMatches(cert, false);
    }

    public static boolean emailMatches(X509Certificate cert, boolean quiet) {
        try {
            X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
            RDN[] emailNames = x500name.getRDNs(BCStyle.E);
            for(RDN emailName : emailNames) {
                AttributeTypeAndValue first = emailName.getFirst();
                if (first != null && first.getValue() != null && Constants.ABOUT_EMAIL.equals(first.getValue().toString())) {
                    if(!quiet) {
                        log.info("Email address {} found, assuming CertProvider is {}", Constants.ABOUT_EMAIL, ExpiryTask.CertProvider.INTERNAL);
                    }
                    return true;
                }
            }
        }
        catch(Exception ignore) {}
        if(!quiet) {
            log.info("Email address {} was not found.  Assuming the certificate is manually installed, we won't try to renew it.", Constants.ABOUT_EMAIL);
        }
        return false;
    }
}
