/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.utils.ShellUtilities;

import java.util.Properties;

/**
 * @author Tres Finocchiaro
 */
public class LinuxCertificate {

    private static final Logger log = LoggerFactory.getLogger(LinuxCertificate.class);

    private static String nssdb = "sql:" + System.getenv("HOME") + "/.pki/nssdb";

    private static String getCertificatePath() {
        // We assume that if the keystore is "qz-tray.jks", the cert must be "root-ca.crt"
        Properties sslProperties = DeployUtilities.loadTrayProperties();
        if (sslProperties != null) {
            return sslProperties.getProperty("wss.keystore").replace(Constants.PROPS_FILE + ".jks", "root-ca.crt");
        }

        return null;
    }

    public static void installCertificate() {
        String certPath = getCertificatePath();
        String errMsg = "";
        boolean success = false;
        if (certPath != null) {
            String certutil = "certutil";
            success = ShellUtilities.execute(new String[] {
                    certutil, "-d", nssdb, "-A", "-t", "TC", "-n", Constants.ABOUT_COMPANY, "-i", certPath
            });

            if (!success) {
                errMsg += "Error executing " + certutil +
                        ".  Ensure it is installed properly with write access to " + nssdb + ".";
            }
        } else {
            errMsg += "Unable to determine path to certificate.";
        }

        if (!success) {
            log.warn("{} Secure websockets will not function on certain browsers.", errMsg);
        }
    }
}
