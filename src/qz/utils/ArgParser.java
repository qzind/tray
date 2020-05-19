/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.common.SecurityInfo;
import qz.exception.MissingArgException;
import qz.installer.Installer;
import qz.installer.TaskKiller;
import qz.installer.certificate.CertificateManager;

import java.io.File;
import java.util.*;
import java.util.List;

import static qz.common.Constants.*;
import static qz.utils.ArgParser.ExitStatus.*;

public class ArgParser {
    public enum ExitStatus {
        SUCCESS(0),
        GENERAL_ERROR(1),
        USAGE_ERROR(2),
        NO_AUTOSTART(0);
        private int code;
        ExitStatus(int code) {
            this.code = code;
        }
        public int getCode() {
            return code;
        }
    }

    protected static final Logger log = LoggerFactory.getLogger(ArgParser.class);
    private List<String> args;
    private ExitStatus exitStatus;

    public ArgParser(String[] args) {
        this.exitStatus = SUCCESS;
        this.args = new ArrayList<>(Arrays.asList(args));
    }
    public List<String> getArgs() {
        return args;
    }

    public ExitStatus getExitStatus() {
        return exitStatus;
    }

    public int getExitCode() {
        return exitStatus.getCode();
    }

    /**
     * Gets the requested flag status
     */
    public boolean hasFlag(String ... matches) {
        for(String match : matches) {
            if (args.contains(match)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the argument value immediately following a command
     * @throws MissingArgException
     */
    public String valueOf(String ... matches) throws MissingArgException {
        for(String match : matches) {
            if (args.contains(match)) {
                int index = args.indexOf(match) + 1;
                if (args.size() >= index + 1) {
                    String val = args.get(index);
                    if(!val.trim().isEmpty()) {
                        return val;
                    }
                }
                throw new MissingArgException();
            }
        }
        return null;
    }

    public ExitStatus processInstallerArgs(Installer.InstallType type, List<String> args) {
        try {
            switch(type) {
                case PREINSTALL:
                    return Installer.preinstall() ? SUCCESS : SUCCESS; // don't abort on preinstall
                case INSTALL:
                    // Handle destination
                    String dest = valueOf("-d", "--dest");
                    // Handle silent installs
                    boolean silent = hasFlag("-s", "--silent");
                    Installer.install(dest, silent); // exception will set error
                    return SUCCESS;
                case CERTGEN:
                    TaskKiller.killAll();

                    // Handle trusted SSL certificate
                    String trustedKey = valueOf("-k", "--key");
                    String trustedCert = valueOf("-c", "--cert");
                    String trustedPfx = valueOf("--pfx", "--pkcs12");
                    String trustedPass = valueOf("-p", "--pass");
                    if (trustedKey != null && trustedCert != null) {
                        File key = new File(trustedKey);
                        File cert = new File(trustedCert);
                        if(key.exists() && cert.exists()) {
                            new CertificateManager(key, cert); // exception will set error
                            return SUCCESS;
                        }
                        log.error("One or more trusted files was not found.");
                        throw new MissingArgException();
                    } else if((trustedKey != null || trustedCert != null || trustedPfx != null) && trustedPass != null) {
                        String pfxPath = trustedPfx == null ? (trustedKey == null ? trustedCert : trustedKey) : trustedPfx;
                        File pfx = new File(pfxPath);

                        if(pfx.exists()) {
                            new CertificateManager(pfx, trustedPass.toCharArray()); // exception will set error
                            return SUCCESS;
                        }
                        log.error("The provided pfx/pkcs12 file was not found: {}", pfxPath);
                        throw new MissingArgException();
                    } else {
                        // Handle localhost override
                        String hosts = valueOf("--host", "--hosts");
                        if (hosts != null) {
                            Installer.getInstance().certGen(true, hosts.split(";"));
                            return SUCCESS;
                        }
                        Installer.getInstance().certGen(true);
                        // Failure in this step is extremely rare, but
                        return SUCCESS; // exception will set error
                    }
                case UNINSTALL:
                    Installer.uninstall();
                    return SUCCESS;
                case SPAWN:
                    args.remove(0); // first argument is "spawn", remove it
                    Installer.getInstance().spawn(args);
                    return SUCCESS;
                default:
                    throw new UnsupportedOperationException("Installation type " + type + " is not yet supported");
            }
        } catch(MissingArgException e) {
            log.error("Valid usage:\n   java -jar {}.jar {}", PROPS_FILE, type.usage);
            return USAGE_ERROR;
        } catch(Exception e) {
            log.error("Installation step {} failed", type, e);
            return GENERAL_ERROR;
        }
    }

    /**
     * Attempts to intercept utility command line args.
     * If intercepted, returns true and sets the <code>exitStatus</code> to a usable integer
     */
    public boolean intercept() {
        // Fist, handle installation commands (e.g. install, uninstall, certgen, etc)
        for(Installer.InstallType installType : Installer.InstallType.values()) {
            if (args.contains(installType.toString())) {
                exitStatus = processInstallerArgs(installType, args);
                return true;
            }
        }
        try {
            // Handle graceful autostart disabling
            if (hasFlag("-A", "--honorautostart")) {
                exitStatus = SUCCESS;
                if(!FileUtilities.isAutostart()) {
                    exitStatus = NO_AUTOSTART;
                    return true;
                }
                // Don't intercept
                exitStatus = SUCCESS;
                return false;
            }

            // Handle version request
            if (hasFlag("-v", "--version")) {
                System.out.println(Constants.VERSION);
                exitStatus = SUCCESS;
                return true;
            }
            // Handle macOS CFBundleIdentifier request
            if (hasFlag("-i", "--bundleid")) {
                System.out.println(MacUtilities.getBundleId());
                exitStatus = SUCCESS;
                return true;
            }
            // Handle cert installation
            String certFile;
            if ((certFile = valueOf("-a", "--whitelist")) != null) {
                exitStatus = FileUtilities.addToCertList(ALLOW_FILE, new File(certFile));
                return true;
            }
            if ((certFile = valueOf("-b", "--blacklist")) != null) {
                exitStatus = FileUtilities.addToCertList(BLOCK_FILE, new File(certFile));
                return true;
            }

            // Print library list
            if (hasFlag("-l", "--libinfo")) {
                SecurityInfo.printLibInfo();
                exitStatus = SUCCESS;
                return true;
            }
        } catch(MissingArgException e) {
            log.error("Invalid usage");
            exitStatus = USAGE_ERROR;
        } catch(Exception e) {
            log.error("Internal error occured");
            exitStatus = GENERAL_ERROR;
        }
        return false;
    }
}
