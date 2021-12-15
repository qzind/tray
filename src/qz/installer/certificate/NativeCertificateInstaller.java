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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.Installer;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.cert.X509Certificate;
import java.util.List;

public abstract class NativeCertificateInstaller {
    private static final Logger log = LogManager.getLogger(NativeCertificateInstaller.class);
    protected static NativeCertificateInstaller instance;

    public static NativeCertificateInstaller getInstance() {
        return getInstance(SystemUtilities.isAdmin() ? Installer.PrivilegeLevel.SYSTEM : Installer.PrivilegeLevel.USER);
    }
    public static NativeCertificateInstaller getInstance(Installer.PrivilegeLevel type) {
        if (instance == null) {
            switch(SystemUtilities.getOsType()) {
                case WINDOWS:
                    instance = new WindowsCertificateInstaller(type);
                    break;
                case MAC:
                    instance = new MacCertificateInstaller(type);
                    break;
                case LINUX:
                default:
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
            File certFile = File.createTempFile(KeyPairWrapper.getAlias(KeyPairWrapper.Type.CA) + "-", CertificateManager.DEFAULT_CERTIFICATE_EXTENSION);
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
        if(SystemUtilities.isJar()) {
            if (remove(find())) {
                log.info("Certificate removed from {} store using {}", store, helper);
            } else {
                log.warn("Could not remove certificate from {} store using {}", store, helper);
            }
        } else {
            log.info("Skipping {} store certificate removal, IDE detected.", store, helper);
        }
        if (add(certFile)) {
            log.info("Certificate added to {} store using {}", store,  helper);
            return true;
        } else {
            log.warn("Could not install certificate to {} store using {}", store, helper);
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
