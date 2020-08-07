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
import com.sun.jna.platform.win32.WinReg;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.certificate.CertificateManager;
import qz.installer.certificate.firefox.locator.AppAlias;
import qz.installer.certificate.firefox.locator.AppInfo;
import qz.installer.certificate.firefox.locator.AppLocator;
import qz.utils.JsonWriter;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
    public static final Version FIREFOX_RESTART_VERSION = Version.valueOf("60.0.0");

    private static String ENTERPRISE_ROOT_POLICY = "{ \"policies\": { \"Certificates\": { \"ImportEnterpriseRoots\": true } } }";
    private static String INSTALL_CERT_POLICY = "{ \"policies\": { \"Certificates\": { \"Install\": [ \"" + Constants.PROPS_FILE + CertificateManager.DEFAULT_CERTIFICATE_EXTENSION + "\"] } } }";
    private static String REMOVE_CERT_POLICY = "{ \"policies\": { \"Certificates\": { \"Install\": [ \"/opt/" + Constants.PROPS_FILE +  "/auth/root-ca.crt\"] } } }";

    public static final String POLICY_LOCATION = "distribution/policies.json";
    public static final String MAC_POLICY_LOCATION = "Contents/Resources/" + POLICY_LOCATION;

    public static final String WINDOWS_ALT_POLICY = "Software\\Policies\\%s\\%s\\Certificates";

    public static void install(X509Certificate cert, String ... hostNames) {
        ArrayList<AppInfo> appList = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        ArrayList<Path> processPaths = null;
        for(AppInfo appInfo : appList) {
            boolean success = false;
            if (honorsPolicy(appInfo)) {
                log.info("Installing Firefox ({}) enterprise root certificate policy {}", appInfo.getName(), appInfo.getPath());
                success = installPolicy(appInfo, cert);
            } else {
                log.info("Installing Firefox ({}) auto-config script {}", appInfo.getName(), appInfo.getPath());
                try {
                    String certData = Base64.getEncoder().encodeToString(cert.getEncoded());
                    success = LegacyFirefoxCertificateInstaller.installAutoConfigScript(appInfo, certData, hostNames);
                }
                catch(CertificateEncodingException e) {
                    log.warn("Unable to install auto-config script to {}", appInfo.getPath(), e);
                }
            }
            if(success) {
                issueRestartWarning(processPaths = AppLocator.getRunningPaths(appList, processPaths), appInfo);
            }
        }
    }

    public static void uninstall() {
        ArrayList<AppInfo> appList = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        for(AppInfo appInfo : appList) {
            if(honorsPolicy(appInfo)) {
                if(SystemUtilities.isWindows() || SystemUtilities.isMac()) {
                    log.info("Skipping uninstall of Firefox enterprise root certificate policy {}", appInfo.getPath());
                } else {
                    try {
                        File policy = appInfo.getPath().resolve(POLICY_LOCATION).toFile();
                        if(policy.exists()) {
                            JsonWriter.write(appInfo.getPath().resolve(POLICY_LOCATION).toString(), INSTALL_CERT_POLICY, false, true);
                        }
                    } catch(IOException | JSONException e) {
                        log.warn("Unable to remove Firefox ({}) policy {}", appInfo.getName(), e);
                    }
                }
            } else {
                log.info("Uninstalling Firefox auto-config script {}", appInfo.getPath());
                LegacyFirefoxCertificateInstaller.uninstallAutoConfigScript(appInfo);
            }
        }
    }

    public static boolean honorsPolicy(AppInfo appInfo) {
        if (appInfo.getVersion() == null) {
            log.warn("Firefox-compatible browser was found {}, but no version information is available", appInfo.getPath());
            return false;
        }
        if(SystemUtilities.isWindows()) {
            return appInfo.getVersion().greaterThanOrEqualTo(WINDOWS_POLICY_VERSION);
        } else if (SystemUtilities.isMac()) {
            return appInfo.getVersion().greaterThanOrEqualTo(MAC_POLICY_VERSION);
        } else {
            return appInfo.getVersion().greaterThanOrEqualTo(LINUX_POLICY_VERSION);
        }
    }

    public static boolean hasSystemPolicy(AppInfo appInfo) {
        String jsonPolicy = SystemUtilities.isWindows() || SystemUtilities.isMac() ? ENTERPRISE_ROOT_POLICY : INSTALL_CERT_POLICY;
        return JsonWriter.contains(appInfo.getPath().resolve(SystemUtilities.isMac() ? MAC_POLICY_LOCATION : POLICY_LOCATION).toFile(), jsonPolicy);
    }

    /**
     * Returns true if an alternative Firefox policy (e.g. registry, plist user or system) is installed
     */
    private static boolean hasAlternatePolicy(AppInfo appInfo, boolean userOnly) {
        if(SystemUtilities.isWindows()) {
            String key = String.format(WINDOWS_ALT_POLICY, appInfo.getVendor(), appInfo.getName(true));
            Integer foundPolicy = WindowsUtilities.getRegInt(userOnly ? WinReg.HKEY_CURRENT_USER : WinReg.HKEY_LOCAL_MACHINE, key, "ImportEnterpriseRoots");
            if(foundPolicy != null) {
                return foundPolicy == 1;
            }
        } else if(SystemUtilities.isMac()) {
            if(!userOnly) {
                return false; // Skip system check
            }
            String policesEnabled = ShellUtilities.executeRaw(new String[] { "defaults", "read", appInfo.getBundleId(), "EnterprisePoliciesEnabled"}, true);
            if(policesEnabled != null && policesEnabled.trim().equals("1")) {
                String foundPolicy = ShellUtilities.execute(new String[] {"defaults", "read", appInfo.getBundleId(), "Certificates"}, new String[] { "ImportEnterpriseRoots = 1;" }, true, true);
                return !foundPolicy.isEmpty();
            }
            return false;
        } else {
            // Linux alternate policy not yet supported
        }
        return false;
    }

    public static boolean installPolicy(AppInfo app, X509Certificate cert) {
        Path jsonPath = app.getPath().resolve(SystemUtilities.isMac() ? MAC_POLICY_LOCATION : POLICY_LOCATION);
        String jsonPolicy = SystemUtilities.isWindows() || SystemUtilities.isMac() ? ENTERPRISE_ROOT_POLICY : INSTALL_CERT_POLICY;
        try {
            if(jsonPolicy.equals(INSTALL_CERT_POLICY)) {
                // Linux lacks the concept of "enterprise roots", we'll write it to a known location instead
                File certFile = new File("/usr/lib/mozilla/certificates", Constants.PROPS_FILE + CertificateManager.DEFAULT_CERTIFICATE_EXTENSION);

                // Make sure we can traverse and read
                File certs = new File("/usr/lib/mozilla/certificates");
                certs.mkdirs();
                certs.setReadable(true, false);
                certs.setExecutable(true, false);
                File mozilla = certs.getParentFile();
                mozilla.setReadable(true, false);
                mozilla.setExecutable(true, false);

                // Make sure we can read
                CertificateManager.writeCert(cert, certFile);
                certFile.setReadable(true, false);
            }

            File jsonFile = jsonPath.toFile();

            // Make sure we can traverse and read
            File distribution = jsonFile.getParentFile();
            distribution.mkdirs();
            distribution.setReadable(true, false);
            distribution.setExecutable(true, false);

            if(jsonPolicy.equals(INSTALL_CERT_POLICY)) {
                // Delete previous policy
                JsonWriter.write(jsonPath.toString(), REMOVE_CERT_POLICY, false, true);
            }

            JsonWriter.write(jsonPath.toString(), jsonPolicy, false, false);

            // Make sure ew can read
            jsonFile.setReadable(true, false);
            return true;
        } catch(JSONException | IOException e) {
            log.warn("Could not install enterprise policy {} to {}", jsonPolicy, jsonPath.toString(), e);
        }
        return false;
    }

    /**
     * Retroactively checks the system for a missing Firefox cert
     * - Removed after Firefox upgrade
     * - Installed after QZ Tray was installed
     */
    public static void fixMissingPolicies() {
        if(!SystemUtilities.isWindows() && !SystemUtilities.isMac()) {
            return; // Retroactive cert checking for Linux isn't yet supported
        }
        ArrayList<AppInfo> appList = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        ArrayList<AppInfo> needsCert = new ArrayList();

        // Iterate Firefox instances without a system-wide or user-wide policy installed
        for(AppInfo appInfo : appList) {
            // Warning: JSON policy must match exactly
            if (honorsPolicy(appInfo) && !hasSystemPolicy(appInfo) && !hasAlternatePolicy(appInfo, true) && !hasAlternatePolicy(appInfo, false)) {
                log.warn("SSL support for {} ({}) is missing. We'll attempt to install it...", appInfo.getName(), appInfo.getPath());
                needsCert.add(appInfo);
            }
        }

        ArrayList<Path> runningList = null;
        for(AppInfo appInfo : needsCert) {
            if(installAlternatePolicy(appInfo)) {
                issueRestartWarning(runningList = AppLocator.getRunningPaths(needsCert, runningList), appInfo);
            }
        }
    }

    public static boolean installAlternatePolicy(AppInfo appInfo) {
        if(SystemUtilities.isWindows()) {
            String key = String.format(WINDOWS_ALT_POLICY, appInfo.getVendor(), appInfo.getName(true));;
            return WindowsUtilities.addRegValue(WinReg.HKEY_CURRENT_USER, key, "ImportEnterpriseRoots", 1);
        } else if(SystemUtilities.isMac()) {
            return ShellUtilities.execute(new String[] {"defaults", "write", appInfo.getBundleId(), "EnterprisePoliciesEnabled", "-bool", "TRUE"}, true) &&
                    ShellUtilities.execute(new String[] {"defaults", "write", appInfo.getBundleId(), "Certificates", "-dict", "ImportEnterpriseRoots", "-bool", "TRUE"}, true);
        }
        return false;
    }

    public static boolean issueRestartWarning(ArrayList<Path> runningPaths, AppInfo appInfo) {
        if (appInfo.getVersion().greaterThanOrEqualTo(FirefoxCertificateInstaller.FIREFOX_RESTART_VERSION)) {
            if (runningPaths.contains(appInfo.getExePath())) {
                try {
                    Installer.getInstance().spawn(appInfo.getExePath().toString(), "-private", "about:restartrequired");
                    return true;
                }
                catch(Exception ignore) {}
            }
        } else {
            log.warn("{} must be restarted for changes to take effect", appInfo.getName());
        }
        return false;
    }
}
