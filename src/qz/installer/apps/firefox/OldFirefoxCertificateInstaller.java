/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.installer.apps.firefox;

import com.github.zafarkhaja.semver.Version;
import com.sun.jna.platform.win32.WinReg;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.ConflictingPolicyException;
import qz.installer.apps.firefox.legacy.LegacyFirefoxCertificateInstaller;
import qz.installer.certificate.CertificateManager;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.locator.AppInfo;
import qz.installer.apps.locator.AppLocator;
import qz.installer.certificate.KeyPairWrapper;
import qz.utils.JsonWriter;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.WindowsUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;

/**
 * Installs the Firefox Policy file via Enterprise Policy, Distribution Policy file or AutoConfig, depending on OS & version
 */
public class OldFirefoxCertificateInstaller {
    protected static final Logger log = LogManager.getLogger(OldFirefoxCertificateInstaller.class);

    /**
     * Versions are for Mozilla's official Firefox release.
     * 3rd-party/clones may adopt Enterprise Policy support under
     * different version numbers, adapt as needed.
     */
    private static final Version WINDOWS_POLICY_VERSION = Version.parse("62.0.0");
    private static final Version MAC_POLICY_VERSION = Version.parse("63.0.0");
    private static final Version LINUX_POLICY_VERSION = Version.parse("65.0.0");
    public static final Version FIREFOX_RESTART_VERSION = Version.parse("60.0.0");

    public static final String FIREFOX_GLOBAL_POLICY_DIRECTORY = "/etc/firefox/policies";
    public static final String FIREFOX_GLOBAL_POLICY_LOCATION = String.format("%s/policies.json", FIREFOX_GLOBAL_POLICY_DIRECTORY);
    public static final String FIREFOX_SNAP_CERT_LOCATION = String.format("%s/%s%s", FIREFOX_GLOBAL_POLICY_DIRECTORY, Constants.PROPS_FILE, CertificateManager.DEFAULT_CERTIFICATE_EXTENSION); // See https://github.com/mozilla/policy-templates/issues/936
    public static final String LINUX_SNAP_CERT_LOCATION = "/etc/firefox/policies/" + Constants.PROPS_FILE + CertificateManager.DEFAULT_CERTIFICATE_EXTENSION; // See https://github.com/mozilla/policy-templates/issues/936
    public static final String LINUX_GLOBAL_CERT_LOCATION = String.format("/usr/lib/mozilla/certificates/%s%s", Constants.PROPS_FILE, CertificateManager.DEFAULT_CERTIFICATE_EXTENSION);

    private static final JSONObject DISTRIBUTION_ENTERPRISE_ROOT_POLICY;
    private static final JSONObject DISTRIBUTION_INSTALL_CERT_POLICY;
    private static final JSONObject DISTRIBUTION_REMOVE_CERT_POLICY;

    static {
        try {
DISTRIBUTION_ENTERPRISE_ROOT_POLICY = new JSONObject()
        .put("policies", new JSONObject()
                .put("Certificates", new JSONObject()
                        .put("ImportEnterpriseRoots", true)));

            // Note: "qz-tray.crt" is a copy of "root-ca.crt", renamed to avoid collision
            String certName = String.format("%s%s", KeyPairWrapper.getAlias(KeyPairWrapper.Type.SSL), CertificateManager.DEFAULT_CERTIFICATE_EXTENSION);

            //String caCertName = String.format("%s%s", KeyPairWrapper.getAlias(KeyPairWrapper.Type.CA), CertificateManager.DEFAULT_CERTIFICATE_EXTENSION);
            //Path qzRootCertPath =  Paths.get(SHARED_DIR.toString(), "ssl", caCertName);

            String linuxSnapCertLocation = String.format("%s/%s", FIREFOX_GLOBAL_POLICY_DIRECTORY, certName); // See https://github.com/mozilla/policy-templates/issues/936

            DISTRIBUTION_INSTALL_CERT_POLICY = new JSONObject()
                    .put("policies", new JSONObject()
                            .put("Certificates", new JSONObject()
                                    .put("Install", new JSONArray()
                                            .put(certName)
                                            .put(linuxSnapCertLocation))));


            // Policies to clean up from previous versions
            Path oldCertPath = Paths.get( "/opt/",Constants.PROPS_FILE, "auth", "root-ca.crt");
            DISTRIBUTION_REMOVE_CERT_POLICY = new JSONObject()
                    .put("policies", new JSONObject()
                            .put("Certificates", new JSONObject()
                                    .put("Install", new JSONArray()
                                            .put(oldCertPath))));
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        String DISTRIBUTION_ENTERPRISE_ROOT_POLICY_OLD = "{ \"policies\": { \"Certificates\": { \"ImportEnterpriseRoots\": true } } }";
        String DISTRIBUTION_INSTALL_CERT_POLICY_OLD = "{ \"policies\": { \"Certificates\": { \"Install\": [ \"" + Constants.PROPS_FILE + CertificateManager.DEFAULT_CERTIFICATE_EXTENSION + "\", \"" + LINUX_SNAP_CERT_LOCATION + "\" ] } } }";
        String DISTRIBUTION_REMOVE_CERT_POLICY_OLD = "{ \"policies\": { \"Certificates\": { \"Install\": [ \"/opt/" + Constants.PROPS_FILE +  "/auth/root-ca.crt\"] } } }";

        System.out.println("NOTE: The following is JSON is debug info and can safety be ignored... ");
        System.out.println("   " + DISTRIBUTION_ENTERPRISE_ROOT_POLICY_OLD);
        System.out.println("   " + DISTRIBUTION_ENTERPRISE_ROOT_POLICY);

        System.out.println("   " + DISTRIBUTION_INSTALL_CERT_POLICY_OLD);
        System.out.println("   " + DISTRIBUTION_INSTALL_CERT_POLICY);

        System.out.println("   " + DISTRIBUTION_REMOVE_CERT_POLICY_OLD);
        System.out.println("   " + DISTRIBUTION_REMOVE_CERT_POLICY);
    }

    public static final String DISTRIBUTION_POLICY_LOCATION = "distribution/policies.json";
    public static final String DISTRIBUTION_MAC_POLICY_LOCATION = "Contents/Resources/" + DISTRIBUTION_POLICY_LOCATION;

    public static final String POLICY_AUDIT_MESSAGE = "Enterprise policy installed by " + Constants.ABOUT_TITLE + " on " + SystemUtilities.timeStamp();

    public static void main(String ... args) {
        // just looking at static fields for now...
    }

    public static void install(X509Certificate cert, String ... hostNames) {
        // Blindly install Firefox enterprise policies to the system (macOS, Windows)
        ArrayList<AppAlias.Alias> enterpriseFailed = new ArrayList<>();
        for(AppAlias.Alias alias : AppAlias.FIREFOX.getAliases()) {
            boolean success = false;
            try {
                if(alias.isEnterpriseReady() && !hasEnterprisePolicy(alias, false)) {
                    log.info("Installing Firefox enterprise certificate policy for {}", alias);
                    success = installEnterprisePolicy(alias, false);
                }
            } catch(ConflictingPolicyException e) {
                log.warn("Conflict found installing {} enterprise cert support.  We'll fallback on the distribution policy instead", alias.getName(), e);
            }
            if(!success) {
                enterpriseFailed.add(alias);
            }
        }

        // Search for installed instances
        HashSet<AppInfo> foundApps = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        HashSet<Path> processPaths = null;

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
                log.info("Installing Firefox auto-config script for {}", appInfo);
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
        HashSet<AppInfo> appList = AppLocator.getInstance().locate(AppAlias.FIREFOX);
        for(AppInfo appInfo : appList) {
            if(honorsPolicy(appInfo)) {
                if(SystemUtilities.isWindows() || SystemUtilities.isMac()) {
                    log.info("Skipping uninstall of Firefox enterprise root certificate policy for {}", appInfo);
                } else {
                    try {
                        File policy = appInfo.getPath().resolve(DISTRIBUTION_POLICY_LOCATION).toFile();
                        if(policy.exists()) {
                            JsonWriter.write(appInfo.getPath().resolve(DISTRIBUTION_POLICY_LOCATION).toString(), DISTRIBUTION_INSTALL_CERT_POLICY.toString(), false, true);
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
            return appInfo.getVersion().isHigherThanOrEquivalentTo(WINDOWS_POLICY_VERSION);
        } else if (SystemUtilities.isMac()) {
            return appInfo.getVersion().isHigherThanOrEquivalentTo(MAC_POLICY_VERSION);
        } else {
            return appInfo.getVersion().isHigherThanOrEquivalentTo(LINUX_POLICY_VERSION);
        }
    }

    /**
     * Returns true if an alternative Firefox policy (e.g. registry, plist user or system) is installed
     */
    private static boolean hasEnterprisePolicy(AppAlias.Alias alias, boolean userOnly) throws ConflictingPolicyException {
        if(SystemUtilities.isWindows()) {
            String key = String.format("SOFTWARE\\Policies\\%s\\%s\\Certificates", alias.getVendor(), alias.getName(true));
            Integer foundPolicy = WindowsUtilities.getRegInt(userOnly ? WinReg.HKEY_CURRENT_USER : WinReg.HKEY_LOCAL_MACHINE, key, "ImportEnterpriseRoots");
            if(foundPolicy != null) {
                return foundPolicy == 1;
            }
        } else if(SystemUtilities.isMac()) {
            String policyLocation = "/Library/Preferences/";
            if(userOnly) {
                policyLocation = System.getProperty("user.home") + policyLocation;
            }
            String policesEnabled = ShellUtilities.executeRaw(new String[] { "defaults", "read", policyLocation + alias.getBundleId(), "EnterprisePoliciesEnabled"}, true);
            String foundPolicy = ShellUtilities.executeRaw(new String[] {"defaults", "read", policyLocation + alias.getBundleId(), "Certificates"}, true);
            if(!policesEnabled.isEmpty() && !foundPolicy.isEmpty()) {
                // Policies exist, decide how to proceed
                if(policesEnabled.trim().equals("1") && foundPolicy.contains("ImportEnterpriseRoots = 1;")) {
                    return true;
                }
                throw new ConflictingPolicyException(String.format("%s enterprise policy conflict at %s: %s", alias.getName(), policyLocation + alias.getBundleId(), foundPolicy));
            }
        } /*
        else {
            // Linux alternate policy not yet supported
        }
        */
        return false;
    }

    /**
     * Install policy to distribution/policies.json
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean installDistributionPolicy(AppInfo app, X509Certificate cert) {
        Path jsonPath = app.getPath().resolve(SystemUtilities.isMac() ? DISTRIBUTION_MAC_POLICY_LOCATION:DISTRIBUTION_POLICY_LOCATION);
        JSONObject jsonPolicy = SystemUtilities.isWindows() || SystemUtilities.isMac() ? DISTRIBUTION_ENTERPRISE_ROOT_POLICY:DISTRIBUTION_INSTALL_CERT_POLICY;

        // Special handling for snaps
        if(app.getPath().toString().startsWith("/snap")) {
            log.info("Snap detected, installing policy file to global location instead: {}", FIREFOX_GLOBAL_POLICY_LOCATION);
            jsonPath = Paths.get(FIREFOX_GLOBAL_POLICY_LOCATION);
        }

        try {
            if(jsonPolicy.equals(DISTRIBUTION_INSTALL_CERT_POLICY)) {
                // Linux lacks the concept of "enterprise roots", we'll write it to a known location instead
                writeCertFile(cert, FIREFOX_SNAP_CERT_LOCATION); // so that the snap can read from it
                writeCertFile(cert, LINUX_GLOBAL_CERT_LOCATION); // default location for non-snaps
            }

            File jsonFile = jsonPath.toFile();

            // Make sure we can traverse and read
            File distribution = jsonFile.getParentFile();
            distribution.mkdirs();
            distribution.setReadable(true, false);
            distribution.setExecutable(true, false);

            if(jsonPolicy.equals(DISTRIBUTION_INSTALL_CERT_POLICY)) {
                // Delete previous policy
                JsonWriter.write(jsonPath.toString(), DISTRIBUTION_REMOVE_CERT_POLICY.toString(), false, true);
            }

            JsonWriter.write(jsonPath.toString(), jsonPolicy.toString(), false, false);

            // Make sure ew can read
            return jsonFile.setReadable(true, false);
        } catch(JSONException | IOException e) {
            log.warn("Could not install distribution policy {} to {}", jsonPolicy, jsonPath.toString(), e);
        }
        return false;
    }

    public static boolean installEnterprisePolicy(AppAlias.Alias alias, boolean userOnly) {
        if(SystemUtilities.isWindows()) {
            String key = String.format("Software\\Policies\\%s\\%s\\Certificates", alias.getVendor(), alias.getName(true));;
            WindowsUtilities.addRegValue(userOnly ? WinReg.HKEY_CURRENT_USER : WinReg.HKEY_LOCAL_MACHINE, key, "Comment", POLICY_AUDIT_MESSAGE);
            return WindowsUtilities.addRegValue(userOnly ? WinReg.HKEY_CURRENT_USER : WinReg.HKEY_LOCAL_MACHINE, key, "ImportEnterpriseRoots", 1);
        } else if(SystemUtilities.isMac()) {
            String policyLocation = "/Library/Preferences/";
            if(userOnly) {
                policyLocation = System.getProperty("user.home") + policyLocation;
            }
            return ShellUtilities.execute(new String[] {"defaults", "write", policyLocation + alias.getBundleId(), "EnterprisePoliciesEnabled", "-bool", "TRUE"}, true) &&
                    ShellUtilities.execute(new String[] {"defaults", "write", policyLocation + alias.getBundleId(), "Certificates", "-dict", "ImportEnterpriseRoots", "-bool", "TRUE",
                                                         "Comment", "-string", POLICY_AUDIT_MESSAGE}, true);
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean issueRestartWarning(HashSet<Path> runningPaths, AppInfo appInfo) {
        boolean firefoxIsRunning = runningPaths.contains(appInfo.getExePath());

        // Edge case for detecting if snap is running, since we can't compare the exact path easily
        for(Path runningPath : runningPaths) {
            if (runningPath.startsWith("/snap/")) {
                firefoxIsRunning = true;
                break;
            }
        }

        if (firefoxIsRunning) {
            if (appInfo.getVersion().isHigherThanOrEquivalentTo(OldFirefoxCertificateInstaller.FIREFOX_RESTART_VERSION)) {
                try {
                    Installer.getInstance().spawn(appInfo.getExePath().toString(), "-private", "about:restartrequired");
                    return true;
                }
                catch(Exception ignore) {}
            } else {
                log.warn("{} must be restarted manually for changes to take effect", appInfo);
            }
        }
        return false;
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void writeCertFile(X509Certificate cert, String location) throws IOException {
        File certFile = new File(location);

        // Make sure we can traverse and read
        File certs = new File(location).getParentFile();
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
}
