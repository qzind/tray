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

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.util.Calendar;
import java.util.Locale;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import qz.common.Constants;
import qz.utils.SystemUtilities;

import static qz.installer.certificate.KeyPairWrapper.Type.*;

public class CertificateChainBuilder {
    public static final String[] DEFAULT_HOSTNAMES = {"localhost", "localhost.qz.io" };

    private static int KEY_SIZE = 2048;
    public static int CA_CERT_AGE = 7305; // 20 years
    public static int SSL_CERT_AGE = 825; // Per https://support.apple.com/HT210176

    private String[] hostNames;

    public CertificateChainBuilder(String ... hostNames) {
        Security.addProvider(new BouncyCastleProvider());
        if(hostNames.length > 0) {
            this.hostNames = hostNames;
        } else {
            this.hostNames = DEFAULT_HOSTNAMES;
        }
    }

    public KeyPairWrapper createCaCert() throws IOException, GeneralSecurityException, OperatorException {
        KeyPair keyPair = createRsaKey();

        X509v3CertificateBuilder builder = createX509Cert(keyPair, CA_CERT_AGE, hostNames);

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(1))
                .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign + KeyUsage.cRLSign))
                .addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));

        // Signing
        ContentSigner sign = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(keyPair.getPrivate());
        X509CertificateHolder certHolder = builder.build(sign);

        // Convert to java-friendly format
        return new KeyPairWrapper(CA, keyPair, new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder));
    }

    public KeyPairWrapper createSslCert(KeyPairWrapper caKeyPairWrapper) throws IOException, GeneralSecurityException, OperatorException {
        KeyPair sslKeyPair = createRsaKey();
        X509v3CertificateBuilder builder = createX509Cert(sslKeyPair, SSL_CERT_AGE, hostNames);

        JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();

        builder.addExtension(Extension.authorityKeyIdentifier, false,  utils.createAuthorityKeyIdentifier(caKeyPairWrapper.getCert()))
                .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
                .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature + KeyUsage.keyEncipherment))
                .addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}))
                .addExtension(Extension.subjectAlternativeName, false, buildSan(hostNames))
                .addExtension(Extension.subjectKeyIdentifier, false, utils.createSubjectKeyIdentifier(sslKeyPair.getPublic()));

        // Signing
        ContentSigner sign = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(caKeyPairWrapper.getKey());
        X509CertificateHolder certHolder = builder.build(sign);

        // Convert to java-friendly format
        return new KeyPairWrapper(SSL, sslKeyPair, new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder));
    }

    private static KeyPair createRsaKey() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    private static X509v3CertificateBuilder createX509Cert(KeyPair keyPair, int age, String ... hostNames) {
        String cn = hostNames.length > 0? hostNames[0]:DEFAULT_HOSTNAMES[0];
        X500Name name = new X500NameBuilder()
                .addRDN(BCStyle.C, Constants.ABOUT_COUNTRY)
                .addRDN(BCStyle.ST, Constants.ABOUT_STATE)
                .addRDN(BCStyle.L, Constants.ABOUT_CITY)
                .addRDN(BCStyle.O, Constants.ABOUT_COMPANY)
                .addRDN(BCStyle.OU, Constants.ABOUT_COMPANY)
                .addRDN(BCStyle.EmailAddress, Constants.ABOUT_EMAIL)
                .addRDN(BCStyle.CN, cn)
                .build();
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Calendar notBefore = Calendar.getInstance(Locale.ENGLISH);
        Calendar notAfter = Calendar.getInstance(Locale.ENGLISH);
        notBefore.add(Calendar.DAY_OF_YEAR, -1);
        notAfter.add(Calendar.DAY_OF_YEAR, age - 1);

        SystemUtilities.swapLocale();
        X509v3CertificateBuilder x509builder = new JcaX509v3CertificateBuilder(name, serial, notBefore.getTime(), notAfter.getTime(), name, keyPair.getPublic());
        SystemUtilities.restoreLocale();
        return x509builder;
    }

    /**
     * Builds subjectAlternativeName extension; iterates and detects IPv4 or hostname
     */
    private static GeneralNames buildSan(String ... hostNames) {
        GeneralName[] gn = new GeneralName[hostNames.length];
        for (int i = 0; i < hostNames.length; i++) {
            int gnType = isIp(hostNames[i]) ? GeneralName.iPAddress : GeneralName.dNSName;
            gn[i] = new GeneralName(gnType, hostNames[i]);
        }
        return GeneralNames.getInstance(new DERSequence(gn));
    }

    private static boolean isIp(String ip) {
        try {
            String[] split = ip.split("\\.");
            if (split.length != 4) return false;
            for (int i = 0; i < 4; ++i) {
                int p = Integer.parseInt(split[i]);
                if (p > 255 || p < 0) return false;
            }
            return true;
        } catch (Exception ignore) {}
        return false;
    }
}