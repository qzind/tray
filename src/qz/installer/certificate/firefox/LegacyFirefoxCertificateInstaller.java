/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer.certificate.firefox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.installer.certificate.CertificateChainBuilder;
import qz.installer.certificate.firefox.locator.AppLocator;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import java.io.*;
import java.security.cert.CertificateEncodingException;
import java.util.*;

/**
 * Legacy Firefox Certificate installer
 *
 * For old Firefox-compatible browsers still in the wild such as Firefox 52 ESR, SeaMonkey, WaterFox, etc.
 */
public class LegacyFirefoxCertificateInstaller {
    private static final Logger log = LoggerFactory.getLogger(CertificateChainBuilder.class);

    private static final String CFG_TEMPLATE = "assets/firefox-autoconfig.js.in";
    private static final String CFG_FILE = Constants.PROPS_FILE + ".cfg";
    private static final String PREFS_FILE = Constants.PROPS_FILE + ".js";
    private static final String PREFS_DIR = "defaults/pref";
    private static final String MAC_PREFIX = "Contents/Resources";

    public static void installAutoConfigScript(AppLocator app, String certData, String ... hostNames) {
        try {
            writePrefsFile(app);
            writeParsedConfig(app, certData, false, hostNames);
        } catch(Exception e) {
            log.warn("Error installing auto-config support for {}", app.getName(), e);
        }
    }

    public static void uninstallAutoConfigScript(AppLocator app) {
        try {
            writeParsedConfig(app, "", true);
        } catch(Exception e) {
            log.warn("Error uninstalling auto-config support for {}", app.getName(), e);
        }
    }

    public static File tryWrite(AppLocator app, boolean mkdirs, String ... paths) throws IOException {
        String dir = app.getPath();
        if (SystemUtilities.isMac()) {
            dir += File.separator + MAC_PREFIX;
        }
        for (String path : paths) {
            dir += File.separator + path;
        }
        File file = new File(dir);

        if(mkdirs) file.mkdirs();
        if(file.exists() && file.isDirectory() && file.canWrite()) {
            return file;
        }

        throw new IOException(String.format("Directory does not exist or is not writable: %s", file));
    }

    public static void deleteFile(File parent, String ... paths) {
        if(parent != null) {
            String toDelete = parent.getPath();
            for (String path : paths) {
                toDelete += File.separator + path;
            }
            File deleteFile = new File(toDelete);
            if (!deleteFile.exists()) {
            } else if (new File(toDelete).delete()) {
                log.info("Deleted old file: {}", toDelete);
            } else {
                log.warn("Could not delete old file: {}", toDelete);
            }
        }
    }

    public static void writePrefsFile(AppLocator app) throws Exception {
        File prefsDir = tryWrite(app, true, PREFS_DIR);
        deleteFile(prefsDir, "firefox-prefs.js"); // cleanup old version

        // first check that there aren't other prefs files
        String pref = "general.config.filename";
        for (File file : prefsDir.listFiles()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while((line = reader.readLine()) != null) {
                    if(line.contains(pref) && !line.contains(CFG_FILE)) {
                        throw new Exception(String.format("Browser already has %s defined in %s:\n   %s", pref, file, line));
                    }
                }
            } catch(IOException ignore) {}
        }

        // write out the new prefs file
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(prefsDir + File.separator + PREFS_FILE)));
        String[] data = {
                String.format("pref('%s', '%s');", pref, CFG_FILE),
                "pref('general.config.obscure_value', 0);"
        };
        for (String line : data) {
            writer.write(line + "\n");
        }
        writer.close();
    }

    private static void writeParsedConfig(AppLocator app, String certData, boolean uninstall, String ... hostNames) throws IOException, CertificateEncodingException{
        if (hostNames.length == 0) hostNames = CertificateChainBuilder.DEFAULT_HOSTNAMES;

        File cfgDir = tryWrite(app, false);
        deleteFile(cfgDir, "firefox-config.cfg"); // cleanup old version
        File dest = new File(cfgDir.getPath() + File.separator + CFG_FILE);

        HashMap<String, String> fieldMap = new HashMap<>();
        // Dynamic fields
        fieldMap.put("%CERT_DATA%", certData);
        fieldMap.put("%COMMON_NAME%", hostNames[0]);
        fieldMap.put("%TIMESTAMP%", uninstall ? "-1" : "" + new Date().getTime());
        fieldMap.put("%APP_PATH%", SystemUtilities.isMac() ? SystemUtilities.detectAppPath() != null ? SystemUtilities.detectAppPath().toString() : "" : "");
        fieldMap.put("%UNINSTALL%", "" + uninstall);

        FileUtilities.configureAssetFile(CFG_TEMPLATE, dest, fieldMap, LegacyFirefoxCertificateInstaller.class);
    }


}
