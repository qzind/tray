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

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.util.encoders.Base64;
import qz.auth.X509Constants;
import qz.common.Constants;
import qz.installer.Installer;
import qz.utils.ByteUtilities;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.UnixUtilities;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static qz.installer.Installer.PrivilegeLevel.*;

/**
 * @author Tres Finocchiaro
 */
public class LinuxCertificateInstaller extends NativeCertificateInstaller {
    private static final Logger log = LogManager.getLogger(LinuxCertificateInstaller.class);
    private static final String CA_CERTIFICATES = "/usr/local/share/ca-certificates/";
    private static final String CA_CERTIFICATE_NAME = Constants.PROPS_FILE + "-root.crt"; // e.g. qz-tray-root.crt
    private static final String PK11_KIT_ID = "pkcs11:id=";

    private static String[] NSSDB_URLS = {
            // Conventional cert store
            "sql:" + System.getenv("HOME") + "/.pki/nssdb/",

            // Snap-specific cert stores
            "sql:" + System.getenv("HOME") + "/snap/chromium/current/.pki/nssdb/",
            "sql:" + System.getenv("HOME") + "/snap/brave/current/.pki/nssdb/",
            "sql:" + System.getenv("HOME") + "/snap/opera/current/.pki/nssdb/",
            "sql:" + System.getenv("HOME") + "/snap/opera-beta/current/.pki/nssdb/"
    };

    private Installer.PrivilegeLevel certType;

    public LinuxCertificateInstaller(Installer.PrivilegeLevel certType) {
        setInstallType(certType);
        findCertutil();
    }

    public Installer.PrivilegeLevel getInstallType() {
        return certType;
    }

    public void setInstallType(Installer.PrivilegeLevel certType) {
        this.certType = certType;
        if (this.certType == SYSTEM) {
            log.warn("Command \"certutil\" (required for certain browsers) needs to run as USER.  We'll try again on launch.");
        }
    }

    public boolean remove(List<String> idList) {
        boolean success = true;
        if(certType == SYSTEM) {
            boolean first = distrustUsingUpdateCaCertificates(idList);
            boolean second = distrustUsingTrustAnchor(idList);
            success = first || second;
        } else {
            for(String nickname : idList) {
                for(String nssdb : NSSDB_URLS) {
                    success = success && ShellUtilities.execute("certutil", "-d", nssdb, "-D", "-n", nickname);
                }
            }
        }
        return success;
    }

    public List<String> find() {
        ArrayList<String> nicknames = new ArrayList<>();
        if(certType == SYSTEM) {
            nicknames = findUsingTrustAnchor();
            nicknames.addAll(findUsingUsingUpdateCaCert());
        } else {
            try {
                for(String nssdb : NSSDB_URLS) {
                    Process p = Runtime.getRuntime().exec(new String[] {"certutil", "-d", nssdb, "-L"});
                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while((line = in.readLine()) != null) {
                        if (line.startsWith(Constants.ABOUT_COMPANY + " ")) {
                            nicknames.add(Constants.ABOUT_COMPANY);
                            break; // Stop reading input; nicknames can't appear more than once
                        }
                    }
                    in.close();
                }
            }
            catch(IOException e) {
                log.warn("Could not get certificate nicknames", e);
            }
        }
        return nicknames;
    }

    public boolean verify(File ignore) { return true; } // no easy way to validate a cert, assume it's installed

    public boolean add(File certFile) {
        boolean success = true;

        if(certType == SYSTEM) {
            // Attempt two common methods for installing the SSL certificate
            File systemCertFile;
            boolean first = (systemCertFile = trustUsingUpdateCaCertificates(certFile)) != null;
            boolean second = trustUsingTrustAnchor(systemCertFile, certFile);
            success = first || second;
        } else if(certType == USER) {
            // Install certificate to local profile using "certutil"
            for(String nssdb : NSSDB_URLS) {
                String[] parts = nssdb.split(":", 2);
                if (parts.length > 1) {
                    File folder = new File(parts[1]);
                    // If .pki/nssdb doesn't exist yet, don't create it! Per https://github.com/qzind/tray/issues/1003
                    if(folder.exists() && folder.isDirectory()) {
                        if (!ShellUtilities.execute("certutil", "-d", nssdb, "-A", "-t", "TC", "-n", Constants.ABOUT_COMPANY, "-i", certFile.getPath())) {
                            log.warn("Something went wrong creating {}. HTTPS will fail on certain browsers which depend on it.", nssdb);
                            success = false;
                        }
                    }
                }
            }
        }

        return success;
    }

    private boolean findCertutil() {
        boolean installed = ShellUtilities.execute("which", "certutil");
        if (!installed) {
            if (certType == SYSTEM && promptCertutil()) {
                if(UnixUtilities.isUbuntu() || UnixUtilities.isDebian()) {
                    installed = ShellUtilities.execute("apt-get", "install", "-y", "libnss3-tools");
                } else if(UnixUtilities.isFedora()) {
                    installed =  ShellUtilities.execute("dnf", "install", "-y", "nss-tools");
                }
            }
        }
        if(!installed) {
            log.warn("A critical component, \"certutil\" wasn't found and cannot be installed automatically. HTTPS will fail on certain browsers which depend on it.");
        }
        return installed;
    }

    private boolean promptCertutil() {
        // Assume silent or headless installs want certutil
        if(Installer.IS_SILENT || GraphicsEnvironment.isHeadless()) {
            return true;
        }
        try {
            SystemUtilities.setSystemLookAndFeel(true);
            return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "A critical component, \"certutil\" wasn't found.  Attempt to fetch it now?");
        } catch(Throwable ignore) {}
        return true;
    }

    /**
     * Common technique for installing system-wide certificates on Debian-based systems (Ubuntu, etc.)
     *
     * This technique is only known to work for select browsers, such as Epiphany.  Browsers such as
     * Firefox and Chromium require different techniques.
     *
     * @return Full path to the destination file if successful, otherwise <code>null</code>
     */
    private File trustUsingUpdateCaCertificates(File certFile) {
        if(hasUpdateCaCertificatesCommand()) {
            File destFile = new File(CA_CERTIFICATES, CA_CERTIFICATE_NAME);
            log.debug("Copying SYSTEM SSL certificate {} to {}", certFile.getPath(), destFile.getPath());
            try {
                if (new File(CA_CERTIFICATES).isDirectory()) {
                    // Note: preserveFileDate=false per https://github.com/qzind/tray/issues/1011
                    FileUtils.copyFile(certFile, destFile, false);
                    if (destFile.isFile()) {
                        // Attempt "update-ca-certificates" (Debian)
                        if (!ShellUtilities.execute("update-ca-certificates")) {
                            log.warn("Something went wrong calling \"update-ca-certificates\" for the SYSTEM SSL certificate.");
                        } else {
                            return destFile;
                        }
                    }
                } else {
                    log.warn("{} is not a valid directory, skipping", CA_CERTIFICATES);
                }
            }
            catch(IOException e) {
                log.warn("Error copying SYSTEM SSL certificate file", e);
            }
        } else {
            log.warn("Skipping SYSTEM SSL certificate install using \"update-ca-certificates\", command missing or invalid");
        }
        return null;
    }

    /**
     * Common technique for installing system-wide certificates on Fedora-based systems
     *
     * Uses first existing non-null file provided
     */
    private boolean trustUsingTrustAnchor(File ... certFiles) {
        if (hasTrustAnchorCommand()) {
            for(File certFile : certFiles) {
                if (certFile == null || !certFile.exists()) {
                    continue;
                }
                // Install certificate to system using "trust anchor" (Fedora)
                if (ShellUtilities.execute("trust", "anchor", "--store", certFile.getPath())) {
                    return true;
                } else {
                    log.warn("Something went wrong calling \"trust anchor\" for the SYSTEM SSL certificate.");
                }
            }
        } else {
            log.warn("Skipping SYSTEM SSL certificate install using \"trust anchor\", command missing or invalid");
        }
        return false;
    }

    private boolean distrustUsingUpdateCaCertificates(List<String> paths) {
        if(hasUpdateCaCertificatesCommand()) {
            boolean deleted = false;
            for(String path : paths) {
                // Process files only; not "trust anchor" URIs
                if(!path.startsWith(PK11_KIT_ID)) {
                    File certFile = new File(path);
                    if (certFile.isFile() && certFile.delete()) {
                        deleted = true;
                    } else {
                        log.warn("SYSTEM SSL certificate {} does not exist, skipping", certFile.getPath());
                    }
                }
            }
            // Attempt "update-ca-certificates" (Debian)
            if(deleted) {
                if (ShellUtilities.execute("update-ca-certificates")) {
                    return true;
                } else {
                    log.warn("Something went wrong calling \"update-ca-certificates\" for the SYSTEM SSL certificate.");
                }
            }
        } else {
            log.warn("Skipping SYSTEM SSL certificate removal using \"update-ca-certificates\", command missing or invalid");
        }
        return false;
    }

    private boolean distrustUsingTrustAnchor(List<String> idList) {
        if(hasTrustAnchorCommand()) {
            for(String id : idList) {
                // only remove by id
                if (id.startsWith(PK11_KIT_ID) && !ShellUtilities.execute("trust", "anchor", "--remove", id)) {
                    log.warn("Something went wrong calling \"trust anchor\" for the SYSTEM SSL certificate.");
                }
            }
        } else {
            log.warn("Skipping SYSTEM SSL certificate removal using \"trust anchor\", command missing or invalid");
        }
        return false;
    }

    /**
     * Check for the presence of a QZ certificate in known locations (e.g. /usr/local/share/ca-certificates/
     * and return the path if found
     */
    private ArrayList<String> findUsingUsingUpdateCaCert() {
        ArrayList<String> found = new ArrayList<>();
        File[] systemCertFiles = { new File(CA_CERTIFICATES, CA_CERTIFICATE_NAME) };
        for(File file : systemCertFiles) {
            if(file.isFile()) {
                found.add(file.getPath());
            }
        }
        return found;
    }

    /**
     * Find QZ installed certificates in the "trust anchor" by searching by email.
     *
     * The "trust" utility identifies certificates as URIs:
     * Example:
     *    pkcs11:id=%7C%5D%02%84%13%D4%CC%8A%9B%81%CE%17%1C%2E%29%1E%9C%48%63%42;type=cert
     *    ... which is an encoded version of the cert's SubjectKeyIdentifier field
     * To identify a match:
     *    1. Extract all trusted certificates and look for a familiar email address
     *    2. If found, construct and store a "trust" compatible URI as the nickname
     */
    private ArrayList<String> findUsingTrustAnchor() {
        ArrayList<String> uris = new ArrayList<>();
        File tempFile = null;
        try {
            // Temporary location for system certificates
            tempFile = File.createTempFile("trust-extract-for-qz-", ".pem");
            // Delete before use: "trust extract" requires an empty file
            tempFile.delete();
            if(ShellUtilities.execute("trust", "extract", "--format", "pem-bundle", tempFile.getPath())) {
                BufferedReader reader = new BufferedReader(new FileReader(tempFile));
                String line;
                StringBuilder base64 = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if(line.startsWith(X509Constants.BEGIN_CERT)) {
                        // Beginning of a new certificate
                        base64.setLength(0);
                    } else if(line.startsWith(X509Constants.END_CERT)) {
                        // End of the existing certificate
                        byte[] certBytes = Base64.decode(base64.toString());
                        CertificateFactory factory = CertificateFactory.getInstance("X.509");
                        X509Certificate cert = (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(certBytes));
                        if(CertificateManager.emailMatches(cert, true)) {
                            byte[] extensionValue = cert.getExtensionValue(Extension.subjectKeyIdentifier.getId());
                            byte[] octets = DEROctetString.getInstance(extensionValue).getOctets();
                            SubjectKeyIdentifier subjectKeyIdentifier = SubjectKeyIdentifier.getInstance(octets);
                            byte[] keyIdentifier = subjectKeyIdentifier.getKeyIdentifier();
                            String hex = ByteUtilities.toHexString(keyIdentifier, true);
                            String uri = PK11_KIT_ID + hex.replaceAll("(.{2})", "%$1") + ";type=cert";
                            log.info("Found matching cert: {}", uri);

                            uris.add(uri);
                        }
                    } else {
                        base64.append(line);
                    }
                }

                reader.close();
            }
        } catch(IOException | CertificateException e) {
            log.warn("An error occurred finding preexisting \"trust anchor\" certificates", e);
        } finally {
            if(tempFile != null && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
        return uris;
    }

    private boolean hasUpdateCaCertificatesCommand() {
        return ShellUtilities.execute("which", "update-ca-certificates");
    }

    private boolean hasTrustAnchorCommand() {
        return ShellUtilities.execute("trust", "anchor", "--help");
    }
}
