package qz.utils;

import org.apache.commons.lang3.StringUtils;
import qz.common.Constants;
import qz.ws.substitutions.Substitutions;

import java.util.ArrayList;
import java.util.Arrays;

import static qz.utils.ArgValue.ArgType.*;

public enum ArgValue {
    // Informational
    HELP(INFORMATION, "Display help information and exit.", null, null,
         "--help", "-h", "/?"),
    VERSION(INFORMATION, "Display version information and exit.", null, null,
            "--version", "-v"),
    BUNDLEID(INFORMATION, "Display Apple bundle identifier and exit.", null, null,
             "--bundleid", "-i"),
    LIBINFO(INFORMATION, "Display detailed library version information and exit.", null, null,
            "--libinfo", "-l"),

    // Actions
    ALLOW(ACTION,String.format("Add the specified certificate to %s.dat.", Constants.ALLOW_FILE), "--allow cert.pem", null,
          "--allow", "--whitelist", "-a"),
    BLOCK(ACTION, String.format("Add the specified certificate to %s.dat.", Constants.BLOCK_FILE), "--block cert.pem", null,
          "--block", "--blacklist", "-b"),
    FILE_ALLOW(ACTION, String.format("Add the specified file.allow entry to %s.properties for FileIO operations, sandboxed to a specified certificate if provided", Constants.PROPS_FILE), "--file-allow /my/file/path [--sandbox \"Company Name\"]",  null,
          "--file-allow"),
    FILE_REMOVE(ACTION, String.format("Removes the specified file.allow entry from %s.properties for FileIO operations", Constants.PROPS_FILE), "--file-remove /my/file/path", null,
               "--file-remove"),

    // Options
    AUTOSTART(OPTION,"Read and honor any autostart preferences before launching.", null, true,
              "--honorautostart", "-A"),
    STEAL(OPTION, "Ask other running instance to stop so that this instance can take precedence.", null, false,
          "--steal", Constants.DATA_DIR + ":steal"),
    HEADLESS(OPTION, "Force startup \"headless\" without graphical interface or interactive components.", null, false,
             "--headless"),

    // Installer stubs
    PREINSTALL(INSTALLER, "Perform critical pre-installation steps: Stop instances, all other special considerations.", null, null,
               "preinstall"),
    INSTALL(INSTALLER, "Copy to the specified destination and preforms platform-specific registration.", "install --dest /my/install/location [--silent]", null,
            "install"),
    CERTGEN(INSTALLER, "Performs certificate generation and registration for proper HTTPS support.", "certgen [--key key.pem --cert cert.pem] [--pfx cert.pfx --pass 12345] [--host \"list;of;hosts\"]", null,
            "certgen"),
    UNINSTALL(INSTALLER, "Perform all uninstall tasks: Stop instances, delete files, unregister settings.", null, null,
              "uninstall"),
    SPAWN(INSTALLER, "Spawn an instance of the specified program as the logged-in user, avoiding starting as the root user if possible.", "spawn [program params ...]", null,
          "spawn"),

    // Build stubs
    JLINK(BUILD, "Download, compress and bundle a Java Runtime", "jlink [--platform mac|windows|linux] [--arch x64|aarch64] [--vendor bellsoft|eclipse|...] [--version ...] [--gc hotspot|openj9] [--gcversion ...]", null,
          "jlink"),
    PROVISION(BUILD, "Provision/bundle addition settings or resources into this installer", "provision --json file.json [--target-os windows --target-arch x86_64]", null,
            "provision"),

    // Parameter stubs
    TRAY_NOTIFICATIONS(PREFERENCES, "Show verbose connect/disconnect notifications in the tray area", null, false,
        "tray.notifications"),
    TRAY_HEADLESS(PREFERENCES, "Start QZ Tray in headless (no user interface) mode", null, false,
        "tray.headless"),
    TRAY_MONOCLE(PREFERENCES, "Enable/disable the use of the Monocle for JavaFX/HTML rendering", null, true,
        "tray.monocle"),
    TRAY_PREVIEW(PREFERENCES, "Enable/disable previews for JavaFX/HTML printing", null, false,
        "tray.preview"),
    TRAY_STRICTMODE(PREFERENCES, "Enable/disable solely trusting certificates matching authcert.override", null, false,
        "tray.strictmode"),
    TRAY_IDLE_PRINTERS(PREFERENCES, "Enable/disable idle crawling of printers and their media information for faster initial results", null, true,
        "tray.idle.printers"),
    TRAY_IDLE_JAVAFX(PREFERENCES, "Enable/disable idle starting of JavaFX for better initial performance", null, true,
        "tray.idle.javafx"),
    SECURITY_FILE_ENABLED(PREFERENCES, "Enable/disable all File Communications features", null, true,
        "security.file.enabled"),
    SECURITY_FILE_STRICT(PREFERENCES, "Enable/disable signing requirements for File Communications features", null, true,
        "security.file.strict"),

    SECURITY_SUBSTITUTIONS_ENABLE(PREFERENCES, "Enable/disable client-side JSON data substitutions via \"" + Substitutions.FILE_NAME + "\" file", null, true,
        "security.substitutions.enable"),
    SECURITY_SUBSTITUTIONS_STRICT(PREFERENCES, "Enable/disable restrictions for materially changing JSON substitutions such as \"copies\":, \"data\": { \"data\": ... } blobs", null, true,
        "security.substitutions.strict"),

    SECURITY_DATA_PROTOCOLS(PREFERENCES, "URL protocols allowed for print, serial, hid, etc", null, "http,https",
        "security.data.protocols"),
    SECURITY_PRINT_TOFILE(PREFERENCES, "Enable/disable printing directly to file paths", null, false,
        "security.print.tofile"),
    SECURITY_WSS_SNISTRICT(PREFERENCES, "Enables strict http/websocket SNI checks", null, false,
                           "security.wss.snistrict"),
    SECURITY_WSS_HTTPSONLY(PREFERENCES, "Disables insecure http/websocket ports (e.g. '8182')", null, false,
                           "security.wss.httpsonly"),
    SECURITY_WSS_HOST(PREFERENCES, "Influences which physical adapter to bind to by setting the host parameter for http/websocket listening", null, "0.0.0.0",
                           "security.wss.host"),
    SECURITY_WSS_ALLOWORIGIN(PREFERENCES, "Override 'Access-Control-Allow-Origin: *' HTTP response header for fine-grained control of incoming HTTP connections", null, "*",
                             "security.wss.alloworigin"),
    WEBSOCKET_SECURE_PORTS(PREFERENCES, "Comma separated list of secure websocket (wss://) ports to use", null, StringUtils.join(Constants.DEFAULT_WSS_PORTS, ","),
                           "websocket.secure.ports"),
    WEBSOCKET_INSECURE_PORTS(PREFERENCES, "Comma separated list of insecure websocket (ws://) ports to use", null, StringUtils.join(Constants.DEFAULT_WS_PORTS, ","),
                           "websocket.insecure.ports"),
    LOG_DISABLE(PREFERENCES, "Disable/enable logging features", null, false,
        "log.disable"),
    LOG_ROTATE(PREFERENCES, "Number of log files to retain when the size fills up", null, 5,
        "log.rotate"),
    LOG_SIZE(PREFERENCES, "Maximum file size (in bytes) of a single log file", null, 524288,
        "log.size"),
    AUTHCERT_OVERRIDE(PREFERENCES, "Override the trusted root certificate in the software.", null, null,
        "authcert.override", "trustedRootCert"),
    PRINTER_STATUS_JOB_DATA(PREFERENCES, "Return all raw (binary) job data with job statuses (use with caution)", null, false,
        "printer.status.jobdata");

    private ArgType argType;
    private String description;
    private String usage;
    private Object defaultVal;
    private String[] matches;

    ArgValue(ArgType argType, String description, String usage, Object defaultVal, String ... matches) {
        this.argType = argType;
        this.description = description;
        this.usage = usage;
        this.defaultVal = defaultVal;
        this.matches = matches;
    }

    public String getMatch() { return matches[0]; }

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

    public Object getDefaultVal() {
        return defaultVal;
    }

    public enum ArgType {
        INFORMATION,
        ACTION,
        OPTION,
        INSTALLER,
        BUILD,
        PREFERENCES,
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
