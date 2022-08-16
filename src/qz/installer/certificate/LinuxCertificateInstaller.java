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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.installer.Installer;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.UnixUtilities;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static qz.installer.Installer.PrivilegeLevel.*;

/**
 * @author Tres Finocchiaro
 */
public class LinuxCertificateInstaller extends NativeCertificateInstaller {
    private static final Logger log = LogManager.getLogger(LinuxCertificateInstaller.class);

    private static String[] NSSDB_URLS = {
            // Conventional cert store
            "sql:" + System.getenv("HOME") + "/.pki/nssdb",

            // Snap-specific cert stores
            "sql:" + System.getenv("HOME") + "/snap/chromium/current/.pki/nssdb",
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
            log.warn("Command \"certutil\" needs to run as USER.  We'll try again on launch. Ignore warnings about SYSTEM store.");
        }
    }

    public boolean remove(List<String> idList) {
        boolean success = true;
        if(certType == SYSTEM) return false;
        for(String nickname : idList) {
            for(String nssdb : NSSDB_URLS) {
                success = success && ShellUtilities.execute("certutil", "-d", nssdb, "-D", "-n", nickname);
            }
        }
        return success;
    }

    public List<String> find() {
        ArrayList<String> nicknames = new ArrayList<>();
        if(certType == SYSTEM) return nicknames;
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
        } catch(IOException e) {
            log.warn("Could not get certificate nicknames", e);
        }
        return nicknames;
    }

    public boolean verify(File ignore) { return true; } // no easy way to validate a cert, assume it's installed

    public boolean add(File certFile) {
        if(certType == SYSTEM) return false;
        // Create directories as needed
        boolean success = true;
        for(String nssdb : NSSDB_URLS) {
            String[] parts = nssdb.split(":", 2);
            if (parts.length > 1) {
                File folder = new File(parts[1]);
                // If .pki/nssdb doesn't exist yet, don't create it! Per https://github.com/qzind/tray/issues/1003
                if(folder.exists() && folder.isDirectory()) {
                    if (!ShellUtilities.execute("certutil", "-d", nssdb, "-A", "-t", "TC", "-n", Constants.ABOUT_COMPANY, "-i", certFile.getPath())) {
                        log.warn("Something went wrong creating {}. HTTPS will fail on browsers which depend on it.", nssdb);
                        success = false;
                    }
                }
            }
        }
        return success;
    }

    private boolean findCertutil() {
        if (!ShellUtilities.execute("which", "certutil")) {
            if (UnixUtilities.isUbuntu() && certType == SYSTEM && promptCertutil()) {
                return ShellUtilities.execute("apt-get", "install", "-y", "libnss3-tools");
            } else {
                log.warn("A critical component, \"certutil\" wasn't found and cannot be installed automatically. HTTPS will fail on browsers which depend on it.");
            }
        }
        return false;
    }

    private boolean promptCertutil() {
        // Assume silent or headless installs want certutil
        if(Installer.IS_SILENT || GraphicsEnvironment.isHeadless()) {
            return true;
        }
        try {
            SystemUtilities.setSystemLookAndFeel();
            return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "A critical component, \"certutil\" wasn't found.  Attempt to fetch it now?");
        } catch(Throwable ignore) {}
        return true;
    }
}
