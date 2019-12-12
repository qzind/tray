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

import qz.common.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Wrap handling of X509Certificate, PrivateKey and KeyStore conversion
 */
public class KeyPairWrapper {
    public enum Type {CA, SSL}

    private Type type;
    private PrivateKey key;
    private char[] password;
    private X509Certificate cert;
    private KeyStore keyStore; // for SSL

    public KeyPairWrapper(Type type) {
        this.type = type;
    }

    public KeyPairWrapper(Type type, KeyPair keyPair, X509Certificate cert) {
        this.type = type;
        this.key = keyPair.getPrivate();
        this.cert = cert;
    }

    /**
     * Load from disk
     */
    public void init(File keyFile, char[] password) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(keyFile.getName().endsWith(".jks") ? "JKS" : "PKCS12");
        keyStore.load(new FileInputStream(keyFile), password);
        init(keyStore, password);
    }

    /**
     * Load from memory
     */
    public void init(KeyStore keyStore, char[] password) throws GeneralSecurityException {
        this.keyStore = keyStore;
        KeyStore.ProtectionParameter param = new KeyStore.PasswordProtection(password);
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(getAlias(), param);
        // the entry we assume is always wrong for pkcs12 imports, search for it instead
        if(entry == null) {
            Enumeration<String> enumerator = keyStore.aliases();
            while(enumerator.hasMoreElements()) {
                String alias = enumerator.nextElement();
                if(keyStore.isKeyEntry(alias)) {
                    this.password = password;
                    this.key = ((KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, param)).getPrivateKey();
                    this.cert = (X509Certificate)keyStore.getCertificate(alias);
                    return;
                }
            }
            throw new GeneralSecurityException("Could not initialize the KeyStore for internal use");
        }

        this.password = password;
        this.key = entry.getPrivateKey();
        this.cert = (X509Certificate)keyStore.getCertificate(getAlias());
    }

    public X509Certificate getCert() {
        return cert;
    }

    public PrivateKey getKey() {
        return key;
    }

    public String getPasswordString() {
        return new String(password);
    }

    public char[] getPassword() {
        return password;
    }

    public static String getAlias(Type type) {
        switch(type) {
            case SSL:
                return Constants.PROPS_FILE; // "qz-tray"
            case CA:
            default:
                return "root-ca";
        }
    }

    public String getAlias() {
       return getAlias(getType());
    }

    public String propsPrefix() {
        switch(type) {
            case SSL:
                return "wss";
            case CA:
            default:
                return "ca";
        }
    }

    public Type getType() {
        return type;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }
}