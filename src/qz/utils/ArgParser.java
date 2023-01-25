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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.build.JLink;
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
import static qz.utils.ArgValue.*;
import static qz.utils.ArgValue.ArgValueOption.*;

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

    protected static final Logger log = LogManager.getLogger(ArgParser.class);

    private static final String USAGE_COMMAND = String.format("java -jar %s.jar", PROPS_FILE);
    private static final int DESCRIPTION_COLUMN = 30;
    private static final int INDENT_SIZE = 2;

    private List<String> args;
    private boolean headless;
    private ExitStatus exitStatus;

    public ArgParser(String[] args) {
        this.exitStatus = SUCCESS;
        this.args = new ArrayList<>(Arrays.asList(args));
    }
    public List<String> getArgs() {
        return args;
    }

    public int getExitCode() {
        return exitStatus.getCode();
    }

    public boolean isHeadless() { return headless; };

    /**
     * Gets the requested flag status
     */
    private boolean hasFlag(String ... matches) {
        for(String match : matches) {
            if (args.contains(match)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFlag(ArgValue argValue) {
        return hasFlag(argValue.getMatches());
    }

    public ArgValue hasFlags(boolean skipHelp, ArgValue ... argValues) {
        for(ArgValue argValue : argValues) {
            if(skipHelp && argValue == HELP) {
                continue;
            }
            if(hasFlag(argValue)) {
                return argValue;
            }
        }
        return null;
    }

    public boolean hasFlag(ArgValueOption argValueOption) {
        return hasFlag(argValueOption.getMatches());
    }

    private String valueOf(String ... matches) throws MissingArgException {
        return valueOf(false, matches);
    }

    /**
     * Convenience for valueOf(false, ...);
     */
    private String valueOpt(String ... matches) throws MissingArgException {
        return valueOf(true, matches);
    }

    /**
     * Gets the argument value immediately following a command
     * @throws MissingArgException
     */
    private String valueOf(boolean optional, String ... matches) throws MissingArgException {
        for(String match : matches) {
            if (args.contains(match)) {
                int index = args.indexOf(match) + 1;
                if (args.size() >= index + 1) {
                    String val = args.get(index);
                    if(!val.trim().isEmpty()) {
                        return val;
                    }
                }
                if(!optional) {
                    throw new MissingArgException();
                }
            }
        }
        return null;
    }

    public String valueOf(ArgValue argValue) throws MissingArgException {
        return valueOf(argValue.getMatches());
    }

    public String valueOf(ArgValueOption argValueOption) throws MissingArgException {
        return valueOf(argValueOption.getMatches());
    }

    public ExitStatus processInstallerArgs(ArgValue argValue, List<String> args) {
        try {
            switch(argValue) {
                case PREINSTALL:
                    return Installer.preinstall() ? SUCCESS : SUCCESS; // don't abort on preinstall
                case INSTALL:
                    // Handle destination
                    String dest = valueOf(DEST);
                    // Handle silent installs
                    boolean silent = hasFlag(SILENT);
                    Installer.install(dest, silent); // exception will set error
                    return SUCCESS;
                case CERTGEN:
                    TaskKiller.killAll();

                    // Handle trusted SSL certificate
                    String trustedKey = valueOf(KEY);
                    String trustedCert = valueOf(CERT);
                    String trustedPfx = valueOf(PFX);
                    String trustedPass = valueOf(PASS);
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
                        String hosts = valueOf(HOST);
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
                    throw new UnsupportedOperationException("Installation type " + argValue + " is not yet supported");
            }
        } catch(MissingArgException e) {
            log.error("Valid usage:\n   {} {}", USAGE_COMMAND, argValue.getUsage());
            return USAGE_ERROR;
        } catch(Exception e) {
            log.error("Installation step {} failed", argValue, e);
            return GENERAL_ERROR;
        }
    }

    public ExitStatus processBuildArgs(ArgValue argValue) {
        try {
            switch(argValue) {
                case JLINK:
                    new JLink(
                            valueOf("--platform", "-p"),
                            valueOf("--arch", "-a"),
                            valueOf("--vendor", "-e"),
                            valueOf("--version", "-v"),
                            valueOf("--gc", "-g"),
                            valueOf("--gcversion", "-c"),
                            valueOpt("--targetjdk", "-j")
                    );
                    return SUCCESS;
                default:
                    throw new UnsupportedOperationException("Build type " + argValue + " is not yet supported");
            }
        } catch(MissingArgException e) {
            log.error("Valid usage:\n   {} {}", USAGE_COMMAND, argValue.getUsage());
            return USAGE_ERROR;
        } catch(Exception e) {
            log.error("Build step {} failed", argValue, e);
            return GENERAL_ERROR;
        }
    }

    /**
     * Attempts to intercept utility command line args.
     * If intercepted, returns true and sets the <code>exitStatus</code> to a usable integer
     */
    public boolean intercept() {
        // First handle help request
        if(hasFlag(HELP)) {
            System.out.println(String.format("Usage: %s (command)", USAGE_COMMAND));

            ArgValue command;
            if((command = hasFlags(true, ArgValue.values())) != null) {
                // Intercept command-specific help requests
                printHelp(command);

                // Loop over command-specific documentation
                ArgValueOption[] argValueOptions = ArgValueOption.filter(command);
                if(argValueOptions.length > 0) {
                    System.out.println("OPTIONS");
                    for(ArgValueOption argValueOption : argValueOptions) {
                        printHelp(argValueOption);
                    }
                } else {
                    System.out.println(System.lineSeparator() + "No options available for this command.");
                }
            } else {
                // Show generic help
                for(ArgValue.ArgType argType : ArgValue.ArgType.values()) {
                    System.out.println(String.format("%s%s", System.lineSeparator(), argType));
                    for(ArgValue argValue : ArgValue.filter(argType)) {
                        printHelp(argValue);
                    }
                }

                System.out.println(String.format("%sFor help on a specific command:", System.lineSeparator()));
                System.out.println(String.format("%sUsage: %s --help (command)", StringUtils.rightPad("", INDENT_SIZE), USAGE_COMMAND));
                commandLoop:
                for(ArgValue argValue : ArgValue.values()) {
                    for(ArgValueOption ignore : ArgValueOption.filter(argValue)) {
                        System.out.println(String.format("%s--help %s",  StringUtils.rightPad("", INDENT_SIZE * 2), argValue.getMatches()[0]));
                        continue commandLoop;
                    }
                }
            }

            exitStatus = USAGE_ERROR;
            return true;
        }

        // Second, handle build or install commands
        ArgValue found = hasFlags(true, ArgValue.filter(ArgType.INSTALLER, ArgType.BUILD));
        if(found != null) {
            switch(found.getType()) {
                case BUILD:
                    // Handle build commands (e.g. jlink)
                    exitStatus = processBuildArgs(found);
                    return true;
                case INSTALLER:
                    // Handle install commands (e.g. install, uninstall, certgen, etc)
                    exitStatus = processInstallerArgs(found, args);
                    return true;
            }
        }

        // Last, handle all other commands including normal startup
        ArgValue argValue = null;
        try {
            // Handle graceful autostart disabling
            if (hasFlag(AUTOSTART)) {
                exitStatus = SUCCESS;
                if(!FileUtilities.isAutostart()) {
                    exitStatus = NO_AUTOSTART;
                    return true;
                }
                // Don't intercept
                exitStatus = SUCCESS;
                return false;
            }

            // Handle headless flag
            if(headless = hasFlag("-h", "--headless")) {
                // Don't intercept
                exitStatus = SUCCESS;
                return false;
            }

            // Handle version request
            if (hasFlag(ArgValue.VERSION)) {
                System.out.println(Constants.VERSION);
                exitStatus = SUCCESS;
                return true;
            }
            // Handle macOS CFBundleIdentifier request
            if (hasFlag(BUNDLEID)) {
                System.out.println(MacUtilities.getBundleId());
                exitStatus = SUCCESS;
                return true;
            }
            // Handle cert installation
            String certFile;
            if ((certFile = valueOf(argValue = ALLOW)) != null) {
                exitStatus = FileUtilities.addToCertList(ALLOW_FILE, new File(certFile));
                return true;
            }
            if ((certFile = valueOf(argValue = BLOCK)) != null) {
                exitStatus = FileUtilities.addToCertList(BLOCK_FILE, new File(certFile));
                return true;
            }

            // Handle file.allow
            String allowPath;
            if ((allowPath = valueOf(argValue = FILE_ALLOW)) != null) {
                exitStatus = FileUtilities.addFileAllowProperty(allowPath, valueOf(SANDBOX));
                return true;
            }
            if ((allowPath = valueOf(argValue = FILE_REMOVE)) != null) {
                exitStatus = FileUtilities.removeFileAllowProperty(allowPath);
                return true;
            }

            // Print library list
            if (hasFlag(LIBINFO)) {
                SecurityInfo.printLibInfo();
                exitStatus = SUCCESS;
                return true;
            }
        } catch(MissingArgException e) {
            System.out.println("Usage:");
            if(argValue != null) {
                printHelp(argValue);
            }
            log.error("Invalid usage was provided");
            exitStatus = USAGE_ERROR;
            return true;
        } catch(Exception e) {
            log.error("Internal error occurred", e);
            exitStatus = GENERAL_ERROR;
            return true;
        }
        return false;
    }

    private static void printHelp(String[] commands, String description, String usage, int indent) {
        String text = String.format("%s%s", StringUtils.leftPad("", indent), StringUtils.join(commands, ", "));
        if (description != null) {
            text = StringUtils.rightPad(text, DESCRIPTION_COLUMN) + description;

        }
        System.out.println(text);
        if (usage != null) {
            System.out.println(StringUtils.rightPad("", DESCRIPTION_COLUMN) + String.format("  %s %s", USAGE_COMMAND, usage));
        }
    }

    private static void printHelp(ArgValue argValue) {
        printHelp(argValue.getMatches(), argValue.getDescription(), argValue.getUsage(), INDENT_SIZE);
    }

    private static void printHelp(ArgValueOption argValueOption) {
        printHelp(argValueOption.getMatches(), argValueOption.getDescription(), null, INDENT_SIZE);
    }
}
