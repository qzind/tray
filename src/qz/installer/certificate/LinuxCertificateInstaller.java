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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.installer.Installer;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;
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
    private static final Logger log = LoggerFactory.getLogger(LinuxCertificateInstaller.class);

    private static String NSSDB = "sql:" + System.getenv("HOME") + "/.pki/nssdb";

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
            success = success && ShellUtilities.execute("certutil", "-d", NSSDB, "-D", "-n", nickname);
        }
        return success;
    }

    public List<String> find() {
        ArrayList<String> nicknames = new ArrayList<>();
        if(certType == SYSTEM) return nicknames;
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"certutil", "-d", NSSDB, "-L"});
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith(Constants.ABOUT_COMPANY + " ")) {
                    nicknames.add(Constants.ABOUT_COMPANY);
                    break; // Stop reading input; nicknames can't appear more than once
                }
            }
            in.close();
        } catch(IOException e) {
            log.warn("Could not get certificate nicknames", e);
        }
        return nicknames;
    }

    public boolean verify(File ignore) { return true; } // no easy way to validate a cert, assume it's installed

    public boolean add(File certFile) {
        if(certType == SYSTEM) return false;
        // Create directories as needed
        String[] parts = NSSDB.split(":", 2);
        if(parts.length > 1) {
            new File(parts[1]).mkdirs();
            return ShellUtilities.execute("certutil", "-d", NSSDB, "-A", "-t", "TC", "-n", Constants.ABOUT_COMPANY, "-i", certFile.getPath());
        }
        log.warn("Something went wrong creating {}. HTTPS will fail on browsers which depend on it.", NSSDB);
        return false;
    }

    private boolean findCertutil() {
        if (!ShellUtilities.execute("which", "certutil")) {
            if (SystemUtilities.isUbuntu() && certType == SYSTEM && promptCertutil()) {
                return ShellUtilities.execute("apt-get", "install", "-y", "libnss3-tools");
            } else {
                log.warn("A critical component, \"certutil\" wasn't found and cannot be installed automatically. HTTPS will fail on browsers which depend on it.");
            }
        }
        return false;
    }

    private boolean promptCertutil() {
        // Assume silent installs want certutil
        if(Installer.IS_SILENT) {
            return true;
        }
        SystemUtilities.setSystemLookAndFeel();
        return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "A critical component, \"certutil\" wasn't found.  Attempt to fetch it now?");
    }
}
