package qz.installer.apps.firefox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.installer.Installer;
import qz.installer.apps.locator.AppAlias;
import qz.installer.apps.policy.locator.LinuxPolicyLocator;
import qz.installer.certificate.CertificateManager;
import qz.utils.FileUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.X509Certificate;

import static qz.installer.Installer.*;

/**
 * Adds or removes a certificate from the policies directory
 */
public class CertificateSideLoader {
    protected static final Logger log = LogManager.getLogger(CertificateSideLoader.class);

    private static final String CERTIFICATE_NAME = Constants.PROPS_FILE + CertificateManager.DEFAULT_CERTIFICATE_EXTENSION;
    private static final LinuxPolicyLocator locator = new LinuxPolicyLocator();

    private final Installer.PrivilegeLevel scope;
    private final AppAlias.Alias alias;

    public CertificateSideLoader(Installer.PrivilegeLevel scope, AppAlias.Alias alias) {
        this.scope = scope;
        this.alias = alias;
    }

    /**
     * Copy the certificate to a location Firefox can read/import via custom policy
     */
    public File add(X509Certificate cert) {
        File certFile = calculatePath();

        // Remove cert if there's one there
        if(!remove(certFile)) {
            return null;
        }

        // Create parent and grandparent, ensure readable and traversable
        // Note: execute permissions are required to list files in unix
        if(FileUtilities.mkdirsRecursive(certFile, true, false, true, isOwnerOnly(scope))) {
            try {
                CertificateManager.writeCert(cert, certFile);
                return certFile;
            } catch(IOException e) {
                log.error("Unable to install '{}' certificate", alias.getName(true), e);
            }
        }

        return null;
    }

    private File calculatePath() {
        Path policyLocation = locator.getLocation(scope, alias);

        // Cleanup old cert location (e.g. /etc/policies/firefox/qz-tray.crt)
        remove(policyLocation
                       .resolve("..")
                       .resolve(CERTIFICATE_NAME).toFile());

        // Calculate new cert path
        return policyLocation
                .getParent()
                .resolve("certificates")
                .resolve(CERTIFICATE_NAME)
                .toFile();
    }

    public File remove() {
        File certFile = calculatePath();
        return remove(certFile) ? certFile : null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean remove(File file) {
        if(file.exists()) {
            if(!file.delete()) {
                log.warn("Could not delete file '{}'", file);
                return false;
            }
        }
        return true;
    }
}
