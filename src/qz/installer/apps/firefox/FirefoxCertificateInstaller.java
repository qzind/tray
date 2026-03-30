package qz.installer.apps.firefox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.apps.policy.PolicyState;
import qz.installer.apps.policy.locator.LinuxPolicyLocator;
import qz.installer.certificate.CertificateManager;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static qz.common.Constants.PROPS_FILE;
import static qz.installer.Installer.*;
import static qz.installer.apps.locator.AppFamily.FIREFOX;
import static qz.installer.apps.policy.PolicyInstaller.PolicyLocator.*;
import static qz.installer.apps.policy.PolicyState.Type.MAP;

/**
 * Utility function for handling Firefox's complex certificate handling
 * <ul>
 *  <li>Installs <code>"{ ImportEnterpriseRoots: true }"</code> policy on macOS & Windows</li>
 *  <li>Installs <code>"{ Install: [ "path/to/qz-tray.crt" ] }</code> policy on Linux</li>
 *  <li>Handles nuances with sandboxed paths on Linux (snaps, flatpaks, etc.)</li>
 * </ul>
 */
public class FirefoxCertificateInstaller {
    protected static final Logger log = LogManager.getLogger(FirefoxCertificateInstaller.class);

    private static final String CERTIFICATE_NAME = Constants.PROPS_FILE + CertificateManager.DEFAULT_CERTIFICATE_EXTENSION;
    private static final LinuxPolicyLocator locator = new LinuxPolicyLocator();

    private final Installer.PrivilegeLevel scope;
    private final X509Certificate caCart;

    public FirefoxCertificateInstaller(Installer.PrivilegeLevel scope, X509Certificate caCert) {
        this.scope = scope;
        this.caCart = caCert;
    }

    /**
     * Installs the Firefox "certificate", policy which varies depending on platform
     */
    public void install() {
        PolicyInstaller policyInstaller;
        for(Map.Entry<AppFamily.AppVariant,Set<AppType>> variantSet : getVariantAppTypes(FIREFOX).entrySet()) {
            AppFamily.AppVariant appVariant = variantSet.getKey();
            for(AppType appType : variantSet.getValue()) {
                policyInstaller = new PolicyInstaller(scope, appVariant, appType);
                switch(SystemUtilities.getOs()) {
                    case WINDOWS:
                    case MAC:
                        // Windows and macOS set policy flag
                        Map<String,Object> returnMap = policyInstaller.readMap(MAP, "Certificates");
                        if (returnMap.getOrDefault("ImportEnterpriseRoots", true).equals(false)) {
                            log.error("Cannot install '{}' policy 'ImportEnterpriseRoots' to 'true', it's already set to 'false'", appVariant);
                            continue; // appVariant
                        }
                        policyInstaller.install(PolicyState.Type.MAP, "Certificates", "ImportEnterpriseRoots", true);
                        break;
                    case LINUX:
                    default:
                        // remove old value
                        policyInstaller.uninstall(PolicyState.Type.MAP, "Certificates", "Install", new Object[] {PROPS_FILE + ".crt"});

                        // Linux needs cert added explicitly
                        File certFile = copyCertificate(appVariant, appType);
                        if (certFile != null) {
                            policyInstaller.install(PolicyState.Type.MAP, "Certificates", "Install", new Object[] {certFile.toString()});
                        }
                }
            }
        }
    }

    public void uninstall() {
        // Remove Firefox Certificate
        for(Map.Entry<AppFamily.AppVariant, Set<AppType>> variantSet : getVariantAppTypes(FIREFOX).entrySet()) {
            AppFamily.AppVariant appVariant = variantSet.getKey();
            for(AppType appType : variantSet.getValue()) {
                switch(SystemUtilities.getOs()) {
                    case WINDOWS:
                    case MAC:
                        // Leave behind "ImportEnterpriseRoots"
                        break;
                    case LINUX:
                    default:
                        // Delete certs we've installed
                        File certFile = deleteCertificate(appVariant, appType);
                        if (certFile != null) {
                            new PolicyInstaller(scope, appVariant).uninstall(PolicyState.Type.MAP, "Certificates", "Install", new Object[] {certFile.toString()});
                        }
                }
            }
        }
    }

    /**
     * Copy the certificate to a location Firefox can read/import via custom policy
     */
    private File copyCertificate(AppFamily.AppVariant appVariant, AppType appType) {
        File certFile = calculatePath(appVariant, appType);

        // Remove cert if there's one there
        if(!deleteCertificate(certFile)) {
            return null;
        }

        // Create parent and grandparent, ensure readable and traversable
        // Note: execute permissions are required to list files in unix
        if(FileUtilities.mkdirsRecursive(certFile, true, false, true, isOwnerOnly(scope))) {
            try {
                CertificateManager.writeCert(caCart, certFile);
                // Assume the file should be readable too
                if (!certFile.setReadable(true, isOwnerOnly(scope))) {
                    log.warn("Unable to set readable '{}'", certFile);
                }
                return certFile;
            } catch(IOException e) {
                log.error("Unable to install '{}' certificate", appVariant.getName(true), e);
            }
        }

        return null;
    }

    private File calculatePath(AppFamily.AppVariant appVariant, AppType appType) {
        Path policyLocation = locator.getLocation(scope, appVariant, appType);

        // Cleanup old cert location (e.g. /etc/policies/firefox/qz-tray.crt)
        deleteCertificate(policyLocation
                       .resolve("..")
                       .resolve(CERTIFICATE_NAME).toFile());

        // Calculate new cert path
        return policyLocation
                .getParent()
                .resolve("certificates")
                .resolve(CERTIFICATE_NAME)
                .toFile();
    }

    private File deleteCertificate(AppFamily.AppVariant appVariant, AppType appType) {
        File certFile = calculatePath(appVariant, appType);
        return deleteCertificate(certFile) ? certFile : null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean deleteCertificate(File file) {
        if(file.exists()) {
            if(!file.delete()) {
                log.warn("Could not delete file '{}'", file);
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a matrix of AppVariant : AppType for this platform
     * <p>
     * This is currently kept generic so it can eventually be moved into a separate utilities function
     * for when policy support for other <code>AppFamily</code> (e.g. <code>CHROMIUM</code>) is added.
     * </p>
     */
    @SuppressWarnings("SameParameterValue")
    private static HashMap<AppFamily.AppVariant, Set<AppType>> getVariantAppTypes(AppFamily appFamily) {
        HashMap<AppFamily.AppVariant, Set<AppType>> variantSets = new HashMap<>();

        // All apps and platforms have default
        Set<AppType> appTypes = new HashSet<>(Set.of(AppType.DEFAULT));
        switch(appFamily) {
            case FIREFOX:
                if (SystemUtilities.isLinux()) {
                    // Firefox on Linux has flatpak policy support too, but certs appear broken
                    // See https://github.com/mozilla/policy-templates/discussions/1301
                    // appTypes.add(AppType.FLATPAK);
                }
            case CHROMIUM:
            default:
        }

        for(AppFamily.AppVariant variant : appFamily.getVariants()) {
            variantSets.put(variant, appTypes);
        }
        return variantSets;
    }
}
