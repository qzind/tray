/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.installer.certificate.*;
import qz.installer.certificate.firefox.FirefoxCertificateInstaller;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.List;

import static qz.common.Constants.*;
import static qz.installer.certificate.KeyPairWrapper.Type.CA;
import static qz.utils.FileUtilities.*;

/**
 * Cross-platform wrapper for install steps
 * - Used by CommandParser via command line
 * - Used by PrintSocketServer at startup to ensure SSL is functioning
 */
public abstract class Installer {
    protected static final Logger log = LoggerFactory.getLogger(Installer.class);

    // Silence prompts within our control
    public static boolean IS_SILENT =  "1".equals(System.getenv(PROPS_FILE + "_silent"));


    public enum InstallType {
        PREINSTALL(""),
        INSTALL("install --dest /my/install/location [--silent]"),
        CERTGEN("certgen [--key key.pem --cert cert.pem] [--pfx cert.pfx --pass 12345] [--host \"list;of;hosts\""),
        UNINSTALL(""),
        SPAWN("spawn [params]");
        public String usage;
        InstallType(String usage) {
            this.usage = usage;
        }
        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum PrivilegeLevel {
        USER,
        SYSTEM
    }

    public abstract Installer removeLegacyStartup();
    public abstract Installer addAppLauncher();
    public abstract Installer addStartupEntry();
    public abstract Installer addSystemSettings();
    public abstract Installer removeSystemSettings();
    public abstract void spawn(List<String> args) throws Exception;

    public abstract Installer addUserSettings();

    public abstract void setDestination(String destination);
    public abstract String getDestination();

    private static Installer instance;

    public static Installer getInstance() {
        if(instance == null) {
            if(SystemUtilities.isWindows()) {
                instance = new WindowsInstaller();
            } else if(SystemUtilities.isMac()) {
                instance = new MacInstaller();
            } else {
                instance = new LinuxInstaller();
            }
        }
        return instance;
    }

    public static void install(String destination, boolean silent) throws Exception {
        IS_SILENT = silent;
        getInstance();
        if (destination != null) {
            instance.setDestination(destination);
        }
        install();
    }

    public static boolean preinstall() {
        log.info("Stopping running instances...");
        return TaskKiller.killAll();
    }

    public static void install() throws Exception {
        getInstance();
        log.info("Installing to {}", instance.getDestination());
        instance.deployApp()
                .removeLegacyStartup()
                .removeLegacyFiles()
                .addSharedDirectory()
                .addAppLauncher()
                .addStartupEntry()
                .addSystemSettings();
    }

    public static void uninstall() {
        log.info("Stopping running instances...");
        TaskKiller.killAll();
        getInstance();
        log.info("Uninstalling from {}", instance.getDestination());
        instance.removeSharedDirectory()
                .removeSystemSettings()
                .removeCerts();
    }

    public Installer deployApp() throws IOException {
        if (SystemUtilities.isMac()) {
            if (System.getenv("INSTALLER_TEMP") != null && !System.getenv("INSTALLER_TEMP").isEmpty()) {
                log.info("We're running from pkgbuild, skipping the deployApp() step");
                return this;
            }
        }

        Path src = SystemUtilities.detectAppPath();
        Path dest = Paths.get(getDestination());

        if(!Files.exists(dest)) {
            Files.createDirectories(dest);
        }

        FileUtils.copyDirectory(src.toFile(), dest.toFile());
        FileUtilities.setPermissionsRecursively(dest, false);
        return this;
    }


    public Installer removeLegacyFiles() {
        String[] dirs = { "demo/js/3rdparty", "utils", "auth" };
        String[] files = { "demo/js/qz-websocket.js", "windows-icon.ico", "Contents/Resources/apple-icon.icns" };
        for (String dir : dirs) {
            try {
                FileUtils.deleteDirectory(new File(instance.getDestination() + File.separator + dir));
            } catch(IOException ignore) {}
        }
        for (String file : files) {
            new File(instance.getDestination() + File.separator + file).delete();
        }
        return this;
    }

    public Installer addSharedDirectory() {
        try {
            Files.createDirectories(SHARED_DIR);
            FileUtilities.setPermissionsRecursively(SHARED_DIR, true);
            log.info("Created shared directory: {}", SHARED_DIR);
        } catch(IOException e) {
            log.warn("Could not create shared directory: {}", SHARED_DIR);
        }
        return this;
    }

    public Installer removeSharedDirectory() {
        try {
            FileUtils.deleteDirectory(SHARED_DIR.toFile());
            log.info("Deleted shared directory: {}", SHARED_DIR);
        } catch(IOException e) {
            log.warn("Could not delete shared directory: {}", SHARED_DIR);
        }
        return this;
    }

    /**
     * Checks, and if needed generates an SSL for the system
     */
    public CertificateManager certGen(boolean forceNew, String... hostNames) throws Exception {
        CertificateManager certificateManager = new CertificateManager(forceNew, hostNames);
        boolean needsInstall = certificateManager.needsInstall();
        try {
            // Check that the CA cert is installed
            X509Certificate caCert = certificateManager.getKeyPair(CA).getCert();
            NativeCertificateInstaller installer = NativeCertificateInstaller.getInstance();

            if (forceNew || needsInstall) {
                // Remove installed certs per request (usually the desktop installer, or failure to write properties)
                List<String> matchingCerts = installer.find();
                installer.remove(matchingCerts);
                matchingCerts.clear();
                installer.install(caCert);
                FirefoxCertificateInstaller.install(caCert, hostNames);
            } else {
                // Make sure the certificate is recognized by the system
                File tempCert = File.createTempFile(KeyPairWrapper.getAlias(KeyPairWrapper.Type.CA) + "-", CertificateManager.DEFAULT_CERTIFICATE_EXTENSION);
                CertificateManager.writeCert(caCert, tempCert); // temp cert
                if(!installer.verify(tempCert)) {
                    installer.install(caCert);
                    FirefoxCertificateInstaller.install(caCert, hostNames);
                }
            }
        }
        catch(Exception e) {
            log.error("Something went wrong obtaining the certificate.  HTTPS will fail.", e);
        }

        return certificateManager;
    }

    /**
     * Remove matching certs from user|system, then Firefox
     */
    public void removeCerts() {
        // System certs
        NativeCertificateInstaller instance = NativeCertificateInstaller.getInstance();
        instance.remove(instance.find());
        // Firefox certs
        FirefoxCertificateInstaller.uninstall();
    }
}
