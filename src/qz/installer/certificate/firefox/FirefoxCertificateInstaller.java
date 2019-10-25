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

import com.github.zafarkhaja.semver.Version;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.installer.certificate.PropertiesLoader;
import qz.installer.certificate.firefox.locator.AppAlias;
import qz.installer.certificate.firefox.locator.AppLocator;
import qz.utils.FileUtilities;
import qz.utils.JsonWriter;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;

/**
 * Installs the Firefox Policy file via Policy file or AutoConfig, depending on the version
 */
public class FirefoxCertificateInstaller {
    protected static final Logger log = LoggerFactory.getLogger(FirefoxCertificateInstaller.class);

    /**
     * Versions are for Mozilla's official Firefox release.
     * 3rd-party/clones may adopt Enterprise Policy support under
     * different version numbers, adapt as needed.
     */
    private static final Version WINDOWS_POLICY_VERSION = Version.valueOf("62.0.0");
    private static final Version MAC_POLICY_VERSION = Version.valueOf("63.0.0");
    private static final Version LINUX_POLICY_VERSION = Version.valueOf("65.0.0");

    private static String ENTERPRISE_ROOT_POLICY = "{ \"policies\": { \"Certificates\": { \"ImportEnterpriseRoots\": true } } }";
    private static String INSTALL_CERT_POLICY = "{ \"policies\": { \"Certificates\": { \"Install\": [ \"" + Constants.PROPS_FILE + PropertiesLoader.DEFAULT_CERTIFICATE_EXTENSION + "\"] } } }";
    public static final String POLICY_LOCATION = "distribution/policies.json";
    public static final String MAC_POLICY_LOCATION = "Contents/Resources/" + POLICY_LOCATION;

    public static void install(X509Certificate cert, String ... hostNames) {
        ArrayList<AppLocator> appList = AppLocator.locate(AppAlias.FIREFOX);
        for(AppLocator app : appList) {
            if(honorsPolicy(app)) {
                log.info("Installing Firefox ({}) enterprise root certificate policy {}", app.getName(), app.getPath());
                installPolicy(app, cert);
            } else {
                log.info("Installing Firefox ({}) auto-config script {}", app.getName(), app.getPath());
                try {
                    String certData = Base64.getEncoder().encodeToString(cert.getEncoded());
                    LegacyFirefoxCertificateInstaller.installAutoConfigScript(app, certData, hostNames);
                } catch(CertificateEncodingException e) {
                    log.warn("Unable to install auto-config script to {}", app.getPath(), e);
                }
            }
        }
    }

    public static void uninstall() {
        ArrayList<AppLocator> appList = AppLocator.locate(AppAlias.FIREFOX);
        for(AppLocator app : appList) {
            if(honorsPolicy(app)) {
                log.info("Skipping uninstall of Firefox enterprise root certificate policy {}", app.getPath());
            } else {
                log.info("Uninstalling Firefox auto-config script {}", app.getPath());
                LegacyFirefoxCertificateInstaller.uninstallAutoConfigScript(app);
            }
        }
    }

    public static boolean honorsPolicy(AppLocator app) {
        if (app.getVersion() == null) {
            log.warn("Firefox-compatible browser was found {}, but no version information is available", app.getPath());
            return false;
        }
        if(SystemUtilities.isWindows()) {
            return app.getVersion().greaterThanOrEqualTo(MAC_POLICY_VERSION);
        } else if (SystemUtilities.isMac()) {
            return app.getVersion().greaterThanOrEqualTo(WINDOWS_POLICY_VERSION);
        } else {
            return app.getVersion().greaterThanOrEqualTo(LINUX_POLICY_VERSION);
        }
    }

    public static void installPolicy(AppLocator app, X509Certificate cert) {
        Path jsonPath = Paths.get(app.getPath(), SystemUtilities.isMac() ? MAC_POLICY_LOCATION : POLICY_LOCATION);
        String jsonPolicy = SystemUtilities.isWindows() || SystemUtilities.isMac() ? ENTERPRISE_ROOT_POLICY : INSTALL_CERT_POLICY;
        FileUtilities.inheritPermissions(jsonPath); // also creates parent directories
        try {
            if(jsonPolicy.equals(INSTALL_CERT_POLICY)) {
                // Linux lacks the concept of "enterprise roots", we'll write it to a known location instead
                File certFile = new File("/usr/lib/mozilla/certificates", Constants.PROPS_FILE + PropertiesLoader.DEFAULT_CERTIFICATE_EXTENSION);

                // Make sure we can traverse and read
                File certs = new File("/usr/lib/mozilla/certificates");
                certs.mkdirs();
                certs.setReadable(true, false);
                certs.setExecutable(true, false);
                File mozilla = certs.getParentFile();
                mozilla.setReadable(true, false);
                mozilla.setExecutable(true, false);

                // Make sure we can read
                PropertiesLoader.writeCert(cert, certFile);
                certFile.setReadable(true, false);
            }

            File jsonFile = jsonPath.toFile();

            // Make sure we can traverse and read
            File distribution = jsonFile.getParentFile();
            distribution.mkdirs();
            distribution.setReadable(true, false);
            distribution.setExecutable(true, false);
            JsonWriter.write(jsonPath.toString(), jsonPolicy, false, false);

            // Make sure ew can read
            jsonFile.setReadable(true, false);
        } catch(JSONException | IOException e) {
            log.warn("Could not install enterprise policy {} to {}", jsonPolicy, jsonPath.toString(), e);
        }
    }

    public static boolean checkRunning(AppLocator app, boolean isSilent) {
        throw new UnsupportedOperationException();
    }
}
