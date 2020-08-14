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
 * Installs the Firefox Policy file via Enterprise Policy, Distribution Policy file or AutoConfig, depending on OS & version
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
    public static final String POLICY_AUDIT_MESSAGE = "Enterprise policy installed by " + Constants.ABOUT_TITLE + " on " + SystemUtilities.timeStamp();

    public static final String WINDOWS_ALT_POLICY = "Software\\Policies\\%s\\%s\\Certificates";

    public static void install(X509Certificate cert, String ... hostNames) {
        // Blindly install Firefox enterprise policies to the system (macOS, Windows)
        ArrayList<AppAlias.Alias> enterpriseFailed = new ArrayList<>();
        for(AppAlias.Alias alias : AppAlias.FIREFOX.getAliases()) {
            try {
                if(alias.isEnterpriseReady() && !hasEnterprisePolicy(alias, false)) {
                    if (!installEnterprisePolicy(alias, false)) {
                        log.warn("Unable to install {} enterprise cert support. We'll fallback on the distribution policy instead", alias.getName());
                        enterpriseFailed.add(alias);
                    }
                }
            } catch(ConflictingPolicyException e) {
                log.warn("Conflict found installing {} enterprise cert support.  We'll fallback on the distribution policy instead", alias.getName(), e);
                enterpriseFailed.add(alias);
            }
        }

        // Search for installed instances
        ArrayList<AppInfo> foundApps = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        ArrayList<Path> processPaths = null;

        for(AppInfo appInfo : foundApps) {
            boolean success = false;
            if (honorsPolicy(appInfo)) {
                if((SystemUtilities.isWindows()|| SystemUtilities.isMac()) && !enterpriseFailed.contains(appInfo.getAlias())) {
                    // Enterprise policy was already installed
                    success = true;
                } else {
                    log.info("Installing Firefox distribution policy for {}", appInfo);
                    success = installDistributionPolicy(appInfo, cert);
                }
            } else {
                log.info("Installing Firefox auto-config script for", appInfo);
                try {
                    String certData = Base64.getEncoder().encodeToString(cert.getEncoded());
                    success = LegacyFirefoxCertificateInstaller.installAutoConfigScript(appInfo, certData, hostNames);
                }
                catch(CertificateEncodingException e) {
                    log.warn("Unable to install auto-config script for {}", appInfo, e);
                }
            }
            if(success) {
                issueRestartWarning(processPaths = AppLocator.getRunningPaths(foundApps, processPaths), appInfo);
            }
        }
    }

    public static void uninstall() {
        ArrayList<AppInfo> appList = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        for(AppInfo appInfo : appList) {
            if(honorsPolicy(appInfo)) {
                if(SystemUtilities.isWindows() || SystemUtilities.isMac()) {
                    log.info("Skipping uninstall of Firefox enterprise root certificate policy for {}", appInfo);
                } else {
                    try {
                        File policy = appInfo.getPath().resolve(POLICY_LOCATION).toFile();
                        if(policy.exists()) {
                            JsonWriter.write(appInfo.getPath().resolve(POLICY_LOCATION).toString(), INSTALL_CERT_POLICY, false, true);
                        }
                    } catch(IOException | JSONException e) {
                        log.warn("Unable to remove Firefox policy for {}", appInfo, e);
                    }
                }
            } else {
                log.info("Uninstalling Firefox auto-config script for {}", appInfo);
                LegacyFirefoxCertificateInstaller.uninstallAutoConfigScript(appInfo);
            }
        }
    }

    public static boolean honorsPolicy(AppInfo appInfo) {
        if (appInfo.getVersion() == null) {
            log.warn("Firefox-compatible browser found {}, but no version information is available", appInfo);
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

    /**
     * Returns true if an alternative Firefox policy (e.g. registry, plist user or system) is installed
     */
    private static boolean hasEnterprisePolicy(AppAlias.Alias alias, boolean userOnly) throws ConflictingPolicyException {
        if(SystemUtilities.isWindows()) {
            if(userOnly) {
                log.warn("Can't write " + alias.getName() + " policy as user, aborting");
                return false;
            }
            String key = String.format(WINDOWS_ALT_POLICY, alias.getVendor(), alias.getName(true));
            Integer foundPolicy = WindowsUtilities.getRegInt(userOnly ? WinReg.HKEY_CURRENT_USER : WinReg.HKEY_LOCAL_MACHINE, key, "ImportEnterpriseRoots");
            if(foundPolicy != null) {
                return foundPolicy == 1;
            }
        } else if(SystemUtilities.isMac()) {
            String policyLocation = "/Library/Preferences/";
            if(userOnly) {
                policyLocation = System.getProperty("user.dir") + policyLocation;
            }
            String policesEnabled = ShellUtilities.executeRaw(new String[] { "defaults", "read", policyLocation + alias.getBundleId(), "EnterprisePoliciesEnabled"}, true);
            String foundPolicy = ShellUtilities.executeRaw(new String[] {"defaults", "read", policyLocation + alias.getBundleId(), "Certificates"}, true);
            if(policesEnabled != null && foundPolicy != null) {
                // Policies exist, decide how to proceed
                if(policesEnabled.equals("1") && foundPolicy.contains("ImportEnterpriseRoots = 1;")) {
                    return true;
                }
                throw new ConflictingPolicyException(String.format("%s enterprise policy conflict at %s: %s", alias.getName(), policyLocation + alias.getBundleId(), foundPolicy));
            }
        } else {
            // Linux alternate policy not yet supported
        }
        return false;
    }

    /**
     * Install policy to distribution/policies.json
     */
    public static boolean installDistributionPolicy(AppInfo app, X509Certificate cert) {
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
            log.warn("Could not install distribution policy {} to {}", jsonPolicy, jsonPath.toString(), e);
        }
        return false;
    }

    public static boolean installEnterprisePolicy(AppAlias.Alias alias, boolean userOnly) {
        if(SystemUtilities.isWindows()) {
            String key = String.format(WINDOWS_ALT_POLICY, alias.getVendor(), alias.getName(true));;
            WindowsUtilities.addRegValue(WinReg.HKEY_CURRENT_USER, key, "Comment", POLICY_AUDIT_MESSAGE);
            return WindowsUtilities.addRegValue(WinReg.HKEY_CURRENT_USER, key, "ImportEnterpriseRoots", 1);
        } else if(SystemUtilities.isMac()) {
            String policyLocation = "/Library/Preferences/";
            if(userOnly) {
                policyLocation = System.getProperty("user.dir") + policyLocation;
            }
            return ShellUtilities.execute(new String[] {"defaults", "write", policyLocation + alias.getBundleId(), "EnterprisePoliciesEnabled", "-bool", "TRUE"}, true) &&
                    ShellUtilities.execute(new String[] {"defaults", "write", policyLocation + alias.getBundleId(), "Certificates", "-dict", "ImportEnterpriseRoots", "-bool", "TRUE",
                                                         "Comment", "-string", POLICY_AUDIT_MESSAGE}, true);
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
            log.warn("{} must be restarted manually for changes to take effect", appInfo);
        }
        return false;
    }
}
