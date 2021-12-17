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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MacCertificateInstaller extends NativeCertificateInstaller {
    private static final Logger log = LogManager.getLogger(MacCertificateInstaller.class);

    public static final String USER_STORE = System.getProperty("user.home") + "/Library/Keychains/login.keychain"; // aka login.keychain-db
    public static final String SYSTEM_STORE = "/Library/Keychains/System.keychain";
    private String certStore;

    public MacCertificateInstaller(Installer.PrivilegeLevel certType) {
        setInstallType(certType);
    }

    public boolean add(File certFile) {
        if (certStore.equals(USER_STORE)) {
            // This will prompt the user
            return ShellUtilities.execute("security", "add-trusted-cert", "-r", "trustRoot", "-k", certStore, certFile.getPath());
        } else {
            return ShellUtilities.execute("security", "add-trusted-cert", "-d", "-r", "trustRoot", "-k", certStore, certFile.getPath());
        }
    }

    public boolean remove(List<String> idList) {
        boolean success = true;
        for (String certId : idList) {
            success = success && ShellUtilities.execute("security", "delete-certificate", "-Z", certId, certStore);
        }
        return success;
    }

    public List<String> find() {
        ArrayList<String> hashList = new ArrayList<>();
        try {
            Process p = Runtime.getRuntime().exec(new String[] {"security", "find-certificate", "-e", Constants.ABOUT_EMAIL, "-Z", certStore});
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("SHA-1") && line.contains(":")) {
                    hashList.add(line.split(":", 2)[1].trim());
                }
            }
            in.close();
        } catch(IOException e) {
            log.warn("Could not get certificate list", e);
        }
        return hashList;
    }

    public boolean verify(File certFile) {
        return ShellUtilities.execute( "security", "verify-cert", "-c", certFile.getPath());
    }

    public void setInstallType(Installer.PrivilegeLevel type) {
        if (type == Installer.PrivilegeLevel.USER) {
            certStore = USER_STORE;
        } else {
            certStore = SYSTEM_STORE;
        }
    }

    public Installer.PrivilegeLevel getInstallType() {
        if (certStore == USER_STORE) {
            return Installer.PrivilegeLevel.USER;
        } else {
            return Installer.PrivilegeLevel.SYSTEM;
        }
    }
}
