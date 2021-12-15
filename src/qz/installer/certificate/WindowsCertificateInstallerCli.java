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
import qz.utils.WindowsUtilities;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Command Line technique for installing certificates on Windows
 * Fallback class for when JNA is not available (e.g. Windows on ARM)
 */
@SuppressWarnings("UnusedDeclaration") //Library class
public class WindowsCertificateInstallerCli extends NativeCertificateInstaller {
    private static final Logger log = LogManager.getLogger(WindowsCertificateInstallerCli.class);
    private Installer.PrivilegeLevel certType;

    public WindowsCertificateInstallerCli(Installer.PrivilegeLevel certType) {
        setInstallType(certType);
    }

    public boolean add(File certFile) {
        if (WindowsUtilities.isWindowsXP()) return false;
        if (certType == Installer.PrivilegeLevel.USER) {
            // This will prompt the user
            return ShellUtilities.execute("certutil.exe", "-addstore", "-f", "-user", "Root", certFile.getPath());
        } else {
            return ShellUtilities.execute("certutil.exe", "-addstore", "-f", "Root", certFile.getPath());
        }
    }

    public boolean remove(List<String> idList) {
        if (WindowsUtilities.isWindowsXP()) return false;
        boolean success = true;
        for (String certId : idList) {
            if (certType == Installer.PrivilegeLevel.USER) {
                success = success && ShellUtilities.execute("certutil.exe", "-delstore", "-user", "Root", certId);
            } else {
                success = success && ShellUtilities.execute("certutil.exe", "-delstore", "Root", certId);
            }
        }
        return success;
    }

    /**
     * Returns a list of serials, if found
     */
    public List<String> find() {
        ArrayList<String> serialList = new ArrayList<>();
        try {
            Process p;
            if (certType == Installer.PrivilegeLevel.USER) {
                p = Runtime.getRuntime().exec(new String[] {"certutil.exe", "-store", "-user", "Root"});
            } else {
                p = Runtime.getRuntime().exec(new String[] {"certutil.exe", "-store", "Root"});
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("================")) {
                    // First line is serial
                    String serial = parseNextLine(in);
                    if (serial != null) {
                        // Second line is issuer
                        String issuer = parseNextLine(in);
                        if (issuer.contains("OU=" + Constants.ABOUT_COMPANY)) {
                            serialList.add(serial);
                        }
                    }
                }
            }
            in.close();
        } catch(Exception e) {
            log.info("Unable to find a Trusted Root Certificate matching \"OU={}\"", Constants.ABOUT_COMPANY);
        }
        return serialList;
    }

    public boolean verify(File certFile) {
        return verifyCert(certFile);
    }

    public static boolean verifyCert(File certFile) {
        // -user also will check the root store
        String dwErrorStatus = ShellUtilities.execute( new String[] {"certutil", "-user", "-verify",  certFile.getPath() }, new String[] { "dwErrorStatus=" }, false, false);
        if(!dwErrorStatus.isEmpty()) {
            String[] parts = dwErrorStatus.split("[\r\n\\s]+");
            for(String part : parts) {
                if(part.startsWith("dwErrorStatus=")) {
                    log.info("Certificate validity says {}", part);
                    String[] status = part.split("=", 2);
                    if (status.length == 2) {
                        return status[1].trim().equals("0");
                    }
                }
            }
        }
        log.warn("Unable to determine certificate validity, you'll be prompted on startup");
        return false;
    }

    public void setInstallType(Installer.PrivilegeLevel type) {
        this.certType = type;
    }

    public Installer.PrivilegeLevel getInstallType() {
        return certType;
    }

    private static String parseNextLine(BufferedReader reader) throws IOException {
        String data = reader.readLine();
        if (data != null) {
            String[] split = data.split(":", 2);
            if (split.length == 2) {
                return split[1].trim();
            }
        }
        return null;
    }

}
