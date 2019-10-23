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

import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.installer.Installer;
import qz.installer.certificate.firefox.FirefoxCertificateInstaller;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.cert.X509Certificate;
import java.util.List;

import static qz.installer.certificate.KeyPairWrapper.Type.CA;

public abstract class NativeCertificateInstaller {
    private static final Logger log = LoggerFactory.getLogger(NativeCertificateInstaller.class);
    protected static NativeCertificateInstaller instance;

    public static NativeCertificateInstaller getInstance() {
        return getInstance(SystemUtilities.isAdmin() ? Installer.PrivilegeLevel.SYSTEM : Installer.PrivilegeLevel.USER);
    }
    public static NativeCertificateInstaller getInstance(Installer.PrivilegeLevel type) {
        if (instance == null) {
            if (SystemUtilities.isWindows()) {
                instance = new WindowsCertificateInstaller(type);
            } else if(SystemUtilities.isMac()) {
                instance = new MacCertificateInstaller(type);
            } else {
                instance = new LinuxCertificateInstaller(type);
            }
        }
        return instance;
    }

    /**
     * Install a certificate from memory
     */
    public boolean install(X509Certificate cert) {
        try {
            File certFile = File.createTempFile(KeyPairWrapper.getAlias(KeyPairWrapper.Type.CA) + "-", PropertiesLoader.DEFAULT_CERTIFICATE_EXTENSION);
            JcaMiscPEMGenerator generator = new JcaMiscPEMGenerator(cert);
            JcaPEMWriter writer = new JcaPEMWriter(new OutputStreamWriter(Files.newOutputStream(certFile.toPath(), StandardOpenOption.CREATE)));
            writer.writeObject(generator.generate());
            writer.close();

            return install(certFile);
        } catch(IOException e) {
            log.warn("Could not install cert from temp file", e);
        }
        return false;
    }

    /**
     * Install a certificate from disk
     */
    public boolean install(File certFile) {
        String helper = instance.getClass().getSimpleName();
        String store = instance.getInstallType().name();
        if (remove(find())) {
            log.info("Certificate removed from {} store using {}", store, helper);
        } else {
            log.warn("Could not remove certificate from {} store using {}", store, helper);
        }
        if (add(certFile)) {
            log.info("Certificate added to {} store using {}", store,  helper);
            return true;
        } else {
            log.warn("Could not install certificate to {} store using {}", store, helper);
            if(this instanceof WindowsCertificateInstaller && !Installer.IS_SILENT) {
                log.warn("Windows detected, prompting instructions...");
                ((WindowsCertificateInstaller)this ).addPrompt(certFile);
            }
        }
        return false;
    }

    public abstract boolean add(File certFile);
    public abstract boolean remove(List<String> idList);
    public abstract List<String> find();
    public abstract boolean verify(File certFile);
    public abstract void setInstallType(Installer.PrivilegeLevel certType);
    public abstract Installer.PrivilegeLevel getInstallType();
}
