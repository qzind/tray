package qz.utils;

import qz.common.Constants;

import java.util.ArrayList;
import java.util.Arrays;

import static qz.utils.ArgValue.ArgType.*;

public enum ArgValue {
    // Informational
    HELP(INFORMATION, "Display help information and exit.", null,
         "--help", "-h", "/?"),
    VERSION(INFORMATION, "Display version information and exit.", null,
            "--version", "-v"),
    BUNDLEID(INFORMATION, "Display Apple bundle identifier and exit.", null,
             "--bundleid", "-i"),
    LIBINFO(INFORMATION, "Display detailed library version information and exit.", null,
            "--libinfo", "-l"),

    // Actions
    ALLOW(ACTION,String.format("Add the specified certificate to %s.dat.", Constants.ALLOW_FILE), "--allow cert.pem",
          "--allow", "--whitelist", "-a"),
    BLOCK(ACTION, String.format("Add the specified certificate to %s.dat.", Constants.BLOCK_FILE), "--block cert.pem",
          "--block", "--blacklist", "-b"),
    FILE_ALLOW(ACTION, String.format("Add the specified file.allow entry to %s.properties for FileIO operations, sandboxed to a specified certificate if provided", Constants.PROPS_FILE), "--file-allow /my/file/path [--sandbox \"Company Name\"]",
          "--file-allow"),
    FILE_REMOVE(ACTION, String.format("Removes the specified file.allow entry from %s.properties for FileIO operations", Constants.PROPS_FILE), "--file-remove /my/file/path",
               "--file-remove"),

    // Options
    AUTOSTART(OPTION,"Read and honor any autostart preferences before launching.", null,
              "--honorautostart", "-A"),
    STEAL(OPTION, "Ask other running instance to stop so that this instance can take precedence.", null,
          "--steal", Constants.DATA_DIR + ":steal"),
    HEADLESS(OPTION, "Force startup \"headless\" without graphical interface or interactive components.", null,
             "--headless"),

    // Installer stubs
    PREINSTALL(INSTALLER, "Perform critical pre-installation steps: Stop instances, all other special considerations.", null,
               "preinstall"),
    INSTALL(INSTALLER, "Copy to the specified destination and preforms platform-specific registration.", "install --dest /my/install/location [--silent]",
            "install"),
    CERTGEN(INSTALLER, "Performs certificate generation and registration for proper HTTPS support.", "certgen [--key key.pem --cert cert.pem] [--pfx cert.pfx --pass 12345] [--host \"list;of;hosts\"]",
            "certgen"),
    UNINSTALL(INSTALLER, "Perform all uninstall tasks: Stop instances, delete files, unregister settings.", null,
              "uninstall"),
    SPAWN(INSTALLER, "Spawn an instance of the specified program as the logged-in user, avoiding starting as the root user if possible.", "spawn [program params ...]",
          "spawn"),

    // Build stubs
    JLINK(BUILD, "Download, compress and bundle a Java Runtime", "jlink [--platform mac|windows|linux] [--arch x64|aarch64] [--vendor bellsoft|eclipse|...] [--version ...] [--gc hotspot|openj9] [--gcversion ...]",
          "jlink");

    private ArgType argType;
    private String description;
    private String usage;
    private String[] matches;

    ArgValue(ArgType argType, String description, String usage, String ... matches) {
        this.argType = argType;
        this.description = description;
        this.usage = usage;
        this.matches = matches;
    }
    public String[] getMatches() {
        return matches;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public ArgType getType() {
        return argType;
    }

    public enum ArgType {
        INFORMATION,
        ACTION,
        OPTION,
        INSTALLER,
        BUILD,
    }

    public static ArgValue[] filter(ArgType ... argTypes) {
        ArrayList<ArgType> match = new ArrayList<>(Arrays.asList(argTypes));
        ArrayList<ArgValue> found = new ArrayList<>();
        for(ArgValue argValue : values()) {
            if(match.contains(argValue.getType())) {
                found.add(argValue);
            }
        }
        return found.toArray(new ArgValue[found.size()]);
    }

    /**
     * Child/parent for options
     */
    public enum ArgValueOption {
        // action
        SANDBOX(ArgValue.FILE_ALLOW, "Treats the allow entry as a sandboxed location.  Only certificates with an exact Common Name can access this location",
             "--sandbox"),

        // install
        DEST(ArgValue.INSTALL, "Installs to the specified destination.  If omitted, a sane default will be used.",
             "--dest", "-d"),
        SILENT(ArgValue.INSTALL, "Suppress all prompts to the user, taking sane defaults.",
               "--silent", "-s"),

        // certgen
        HOST(ArgValue.CERTGEN, "Semicolon-delimited hostnames and/or IP addresses to generate the HTTPS certificate for.",
             "--host", "--hosts"),
        CERT(ArgValue.CERTGEN, "Path to a stand-alone HTTPS certificate",
             "--cert", "-c"),
        KEY(ArgValue.CERTGEN, "Path to a stand-alone HTTPS private key",
            "--key", "-k"),
        PFX(ArgValue.CERTGEN, "Path to a paired HTTPS private key and certificate in PKCS#12 format.",
            "--pfx", "--pkcs12"),
        PASS(ArgValue.CERTGEN, "Password for decoding private key.",
             "--pass", "-p");

        ArgValue parent;
        String description;
        String[] matches;

        ArgValueOption(ArgValue parent, String description, String ... matches) {
            this.parent = parent;
            this.description = description;
            this.matches = matches;
        }

        public static ArgValueOption[] filter(ArgValue ... parents) {
            ArrayList<ArgValue> match = new ArrayList<>(Arrays.asList(parents));
            ArrayList<ArgValueOption> found = new ArrayList<>();
            for(ArgValueOption argValueOption : values()) {
                if(match.contains(argValueOption.getParent())) {
                    found.add(argValueOption);
                }
            }
            return found.toArray(new ArgValueOption[found.size()]);
        }

        public ArgValue getParent() {
            return parent;
        }

        public String[] getMatches() {
            return matches;
        }

        public String getDescription() {
            return description;
        }
    }
}
