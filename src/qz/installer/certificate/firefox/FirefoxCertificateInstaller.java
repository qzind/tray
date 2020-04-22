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
import qz.installer.TaskControl;
import qz.installer.certificate.CertificateManager;
import qz.installer.certificate.firefox.locator.AppAlias;
import qz.installer.certificate.firefox.locator.AppInfo;
import qz.installer.certificate.firefox.locator.AppLocator;
import qz.utils.JsonWriter;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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
    private static String INSTALL_CERT_POLICY = "{ \"policies\": { \"Certificates\": { \"Install\": [ \"" + Constants.PROPS_FILE + CertificateManager.DEFAULT_CERTIFICATE_EXTENSION + "\"] } } }";
    private static String REMOVE_CERT_POLICY = "{ \"policies\": { \"Certificates\": { \"Install\": [ \"/opt/" + Constants.PROPS_FILE +  "/auth/root-ca.crt\"] } } }";

    public static final String POLICY_LOCATION = "distribution/policies.json";
    public static final String MAC_POLICY_LOCATION = "Contents/Resources/" + POLICY_LOCATION;

    public static void install(X509Certificate cert, String ... hostNames) {
        ArrayList<AppInfo> appList = AppLocator.locate(AppAlias.FIREFOX);
        for(AppInfo appInfo : appList) {
            if(honorsPolicy(appInfo)) {
                log.info("Installing Firefox ({}) enterprise root certificate policy {}", appInfo.getName(), appInfo.getPath());
                installPolicy(appInfo, cert);
            } else {
                log.info("Installing Firefox ({}) auto-config script {}", appInfo.getName(), appInfo.getPath());
                try {
                    String certData = Base64.getEncoder().encodeToString(cert.getEncoded());
                    LegacyFirefoxCertificateInstaller.installAutoConfigScript(appInfo, certData, hostNames);
                } catch(CertificateEncodingException e) {
                    log.warn("Unable to install auto-config script to {}", appInfo.getPath(), e);
                }
            }
        }
        try {
            restartFirefox(appList);
        } catch(IOException e) {
            log.warn("Unable to restart Firefox, this will have to be done manually.", e);
        }
    }

    public static void uninstall() {
        ArrayList<AppInfo> appList = AppLocator.locate(AppAlias.FIREFOX);
        for(AppInfo appInfo : appList) {
            if(honorsPolicy(appInfo)) {
                if(SystemUtilities.isWindows() || SystemUtilities.isMac()) {
                    log.info("Skipping uninstall of Firefox enterprise root certificate policy {}", appInfo.getPath());
                } else {
                    try {
                        File policy = Paths.get(appInfo.getPath(), POLICY_LOCATION).toFile();
                        if(policy.exists()) {
                            JsonWriter.write(Paths.get(appInfo.getPath(), POLICY_LOCATION).toString(), INSTALL_CERT_POLICY, false, true);
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

    public static void installPolicy(AppInfo app, X509Certificate cert) {
        Path jsonPath = Paths.get(app.getPath(), SystemUtilities.isMac() ? MAC_POLICY_LOCATION : POLICY_LOCATION);
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
        } catch(JSONException | IOException e) {
            log.warn("Could not install enterprise policy {} to {}", jsonPolicy, jsonPath.toString(), e);
        }
    }

    public static boolean checkRunning(AppLocator app, boolean isSilent) {
        throw new UnsupportedOperationException();
    }

    public static void restartFirefox(ArrayList<AppInfo> appList) throws IOException {
        ArrayList<Path> processPaths = new ArrayList<>();
        HashMap<AppInfo, Path> appsToRestart = new HashMap<>();

        String fileExtention = SystemUtilities.isWindows() ? ".exe" : "";

        AppAlias.Alias[] aliases = AppAlias.FIREFOX.getAliases();
        String[] appNames = new String[aliases.length];
        for (int i = 0; i < appNames.length; i++) {
            appNames[i] = aliases[i].posix + fileExtention;
        }

        processPaths.addAll(TaskControl.locateProcessPaths(true,appNames));

        log.warn("Found " + processPaths.toString() + " running");
        for (AppInfo appInfo : appList) {
            Path appPath = Paths.get(appInfo.getPath()).toRealPath();
            //todo change app.getPath to return a path object?
            for (Path processPath : processPaths) {
                if (processPath.startsWith(appPath)) appsToRestart.put(appInfo, processPath);
            }
        }
        String text = "The following must restart for the changes to take effect.";
        boolean shouldPrompt = false;

        for (Map.Entry<AppInfo, Path> pair: appsToRestart.entrySet()) {
            AppInfo appInfo = pair.getKey();

            if (appInfo.getVersion().lessThan(Version.forIntegers(60))) {
                shouldPrompt = true;
                text += "\n" + appInfo.getName() + " Version: " + appInfo.getVersion();
            } else {
                executeRestartRequired(pair.getValue());
            }
        }

        if (shouldPrompt) {
            //Todo Remove this debugging log
            log.warn(text);
        }
    }

    public static void executeRestartRequired(Path processPath) {
        String[] cmd = {processPath.toString(), "-private", "about:restartrequired"};
        ShellUtilities.executeRaw(cmd);
    }
}
