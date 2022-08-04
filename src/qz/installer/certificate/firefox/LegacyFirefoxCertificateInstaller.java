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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.installer.certificate.CertificateChainBuilder;
import qz.installer.certificate.firefox.locator.AppInfo;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import java.io.*;
import java.nio.file.Path;
import java.security.cert.CertificateEncodingException;
import java.util.*;

/**
 * Legacy Firefox Certificate installer
 *
 * For old Firefox-compatible browsers still in the wild such as Firefox 52 ESR, SeaMonkey, WaterFox, etc.
 */
public class LegacyFirefoxCertificateInstaller {
    private static final Logger log = LogManager.getLogger(CertificateChainBuilder.class);

    private static final String CFG_TEMPLATE = "assets/firefox-autoconfig.js.in";
    private static final String CFG_FILE = Constants.PROPS_FILE + ".cfg";
    private static final String PREFS_FILE = Constants.PROPS_FILE + ".js";
    private static final String PREFS_DIR = "defaults/pref";
    private static final String MAC_PREFIX = "Contents/Resources";

    public static boolean installAutoConfigScript(AppInfo appInfo, String certData, String ... hostNames) {
        try {
            if(appInfo.getPath().toString().equals("/usr/bin")) {
                throw new Exception("Preventing install to root location");
            }
            writePrefsFile(appInfo);
            writeParsedConfig(appInfo, certData, false, hostNames);
            return true;
        } catch(Exception e) {
            log.warn("Error installing auto-config support for {}", appInfo, e);
        }
        return false;
    }

    public static boolean uninstallAutoConfigScript(AppInfo appInfo) {
        try {
            writeParsedConfig(appInfo, "", true);
            return true;
        } catch(Exception e) {
            log.warn("Error uninstalling auto-config support for {}", appInfo, e);
        }
        return false;
    }

    public static File tryWrite(AppInfo appInfo, boolean mkdirs, String ... paths) throws IOException {
        Path dir = appInfo.getPath();
        if (SystemUtilities.isMac()) {
            dir = dir.resolve(MAC_PREFIX);
        }
        for (String path : paths) {
            dir = dir.resolve(path);
        }
        File file = dir.toFile();

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

    public static void writePrefsFile(AppInfo app) throws Exception {
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
        File prefsFile = new File(prefsDir,  PREFS_FILE);
        BufferedWriter writer = new BufferedWriter(new FileWriter(prefsFile));
        String[] data = {
                String.format("pref('%s', '%s');", pref, CFG_FILE),
                "pref('general.config.obscure_value', 0);"
        };
        for (String line : data) {
            writer.write(line + "\n");
        }
        writer.close();
        prefsFile.setReadable(true, false);
    }

    private static void writeParsedConfig(AppInfo appInfo, String certData, boolean uninstall, String ... hostNames) throws IOException, CertificateEncodingException{
        if (hostNames.length == 0) hostNames = CertificateChainBuilder.DEFAULT_HOSTNAMES;

        File cfgDir = tryWrite(appInfo, false);
        deleteFile(cfgDir, "firefox-config.cfg"); // cleanup old version
        File dest = new File(cfgDir.getPath(), CFG_FILE);

        HashMap<String, String> fieldMap = new HashMap<>();
        // Dynamic fields
        fieldMap.put("%CERT_DATA%", certData);
        fieldMap.put("%COMMON_NAME%", hostNames[0]);
        fieldMap.put("%TIMESTAMP%", uninstall ? "-1" : "" + new Date().getTime());
        fieldMap.put("%APP_PATH%", SystemUtilities.isMac() ? SystemUtilities.getAppPath() != null ? SystemUtilities.getAppPath().toString() : "" : "");
        fieldMap.put("%UNINSTALL%", "" + uninstall);

        FileUtilities.configureAssetFile(CFG_TEMPLATE, dest, fieldMap, LegacyFirefoxCertificateInstaller.class);
        dest.setReadable(true, false);
    }


}
