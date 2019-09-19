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
import qz.installer.certificate.firefox.locator.AppAlias;
import qz.installer.certificate.firefox.locator.AppLocator;
import qz.utils.JsonWriter;
import qz.utils.SystemUtilities;

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

    private static String ENTERPRISE_POLICY = "{ \"policies\": { \"Certificates\": { \"ImportEnterpriseRoots\": true } } }";
    public static final String POLICY_LOCATION = "distribution/policies.json";
    public static final String MAC_POLICY_LOCATION = "Contents/Resources/" + POLICY_LOCATION;

    public static void install(X509Certificate cert, String ... hostNames) {
        ArrayList<AppLocator> appList = AppLocator.locate(AppAlias.FIREFOX);
        for(AppLocator app : appList) {
            if(honorsEnterprisePolicy(app)) {
                log.info("Installing Firefox enterprise root certificate policy {}", app.getPath());
                installEnterprisePolicy(app);
            } else {
                log.info("Installing Firefox auto-config script {}", app.getPath());
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
            if(honorsEnterprisePolicy(app)) {
                log.info("Skipping uninstall of Firefox enterprise root certificate policy {}", app.getPath());
            } else {
                log.info("Uninstalling Firefox auto-config script {}", app.getPath());
                LegacyFirefoxCertificateInstaller.uninstallAutoConfigScript(app);
            }
        }
    }

    public static boolean honorsEnterprisePolicy(AppLocator app) {
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

    public static void installEnterprisePolicy(AppLocator app) {
        Path jsonPath = Paths.get(app.getPath(), SystemUtilities.isMac() ? MAC_POLICY_LOCATION : POLICY_LOCATION);
        try {
            JsonWriter.write(jsonPath.toString(), ENTERPRISE_POLICY, false, false);
        } catch(JSONException | IOException e) {
            log.warn("Could not install enterprise policy {} to {}", ENTERPRISE_POLICY, jsonPath.toString(), e);
        }
    }

    public static boolean checkRunning(AppLocator app, boolean isSilent) {
        throw new UnsupportedOperationException();
    }
}
