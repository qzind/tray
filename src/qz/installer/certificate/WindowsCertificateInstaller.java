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

/**
 * TODO: Can we get the info through JNA?
 * TODO: https://stackoverflow.com/a/42088543/3196753
 * TODO: https://stackoverflow.com/q/36673163/3196753
 */
public class WindowsCertificateInstaller extends NativeCertificateInstaller {
    private static final Logger log = LoggerFactory.getLogger(WindowsCertificateInstaller.class);
    private Installer.PrivilegeLevel type;

    public WindowsCertificateInstaller(Installer.PrivilegeLevel type) {
        setInstallType(type);
    }

    public boolean add(File certFile) {
        if (SystemUtilities.isWindowsXP()) return false;
        if (type == Installer.PrivilegeLevel.USER) {
            // This will prompt the user
            return ShellUtilities.execute("certutil.exe", "-addstore", "-f", "-user", "Root", certFile.getPath());
        } else {
            return ShellUtilities.execute("certutil.exe", "-addstore", "-f", "Root", certFile.getPath());
        }
    }

    public boolean remove(List<String> idList) {
        if (SystemUtilities.isWindowsXP()) return false;
        boolean success = true;
        for (String certId : idList) {
            if (type == Installer.PrivilegeLevel.USER) {
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
            if (type == Installer.PrivilegeLevel.USER) {
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
        this.type = type;
    }

    public Installer.PrivilegeLevel getInstallType() {
        return type;
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

    // FIXME, standardize this
    public static boolean addPrompt(File certFile) {
        SystemUtilities.setSystemLookAndFeel();

        String html = "<html>" + Constants.ABOUT_TITLE + " was unable to install a critical component For HTTPS to function properly:<br><br>" +
                "To do this manually:<ol>" +
                "<li>Navigate to <strong>" + certFile.getPath() + "</strong></li>" +
                "<li>Click <strong>Install Certificate...</strong></li>" +
                "<li>Click <strong>Place all certificates in the following store</strong></li>" +
                "<li>Browse to <strong>Trusted Root Certificate Authority</li>" +
                "<li>Click <strong>Finish</strong></li>" +
                "<li>Click <strong>Yes</strong> on thumbprint Security Warning</li></ol></html>";

        JOptionPane.showMessageDialog(null, html);
        ShellUtilities.executeRaw("rundll32.exe", "cryptext.dll,CryptExtAddCER", certFile.getPath());
        return true;
    }
}
