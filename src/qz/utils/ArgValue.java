package qz.utils;

import qz.common.Constants;

import java.util.ArrayList;
import java.util.Arrays;

import static qz.utils.ArgValue.ArgType.*;

public enum ArgValue {
    // Informational
    HELP(INFORMATION, "--help", "-h", "Display help information and exit.", null),
    VERSION(INFORMATION, "--version", "-v", "Display version information and exit.", null),
    BUNDLEID(INFORMATION, "--bundleid", "-i", "Display Apple bundle identifier and exit.", null),
    LIBINFO(INFORMATION, "--libinfo", "-l", "Display detailed library version information and exit.", null),

    // Actions
    ALLOW(ACTION,"--allow", "--whitelist", "-a", String.format("Add the specified certificate to %s.dat.", Constants.ALLOW_FILE), "--allow cert.pem"),
    BLOCK(ACTION,"--block", "--blacklist", "-b", String.format("Add the specified certificate to %s.dat.", Constants.BLOCK_FILE), "--block cert.pem"),

    // Options
    AUTOSTART(OPTION,"--honorautostart", "-A", "Read and honor any autostart preferences before launching.", null),
    HEADLESS(OPTION, "--headless", "Force startup \"headless\" without graphical interface or interactive components.", null),

    // Installer stubs
    PREINSTALL(INSTALLER, "preinstall", "Perform critical pre-installation steps: Stop instances, all other special considerations.", null),
    INSTALL(INSTALLER, "install", "Copy to the specified destination and preforms platform-specific registration.", "install --dest /my/install/location [--silent]"),
    CERTGEN(INSTALLER, "certgen", "Performs certificate generation and registration for proper HTTPS support.", "certgen [--key key.pem --cert cert.pem] [--pfx cert.pfx --pass 12345] [--host \"list;of;hosts\"]"),
    UNINSTALL(INSTALLER, "uninstall", "Perform all uninstall tasks: Stop instances, delete files, unregister settings.", null),
    SPAWN(INSTALLER, "spawn", "Spawn an instance of the specified program as the logged-in user, avoiding starting as the root user if possible.", "spawn [program params ...]");

    private ArgType argType;
    private String[] matches;
    private String description;
    private String usage;

    ArgValue(ArgType argType, String[] matches, String description, String usage) {
        this.argType = argType;
        this.matches = matches;
        this.description = description;
        this.usage = usage;
    }
    ArgValue(ArgType argType, String match1, String match2, String match3, String description, String usage) {
        this(argType, new String[] { match1, match2, match3 }, description, usage);
    }
    ArgValue(ArgType argType, String match1, String match2, String description, String usage) {
        this(argType, new String[] { match1, match2 }, description, usage);
    }
    ArgValue(ArgType argType, String match, String description, String usage) {
        this(argType, new String[] { match }, description, usage);
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
        INSTALLER
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
}
