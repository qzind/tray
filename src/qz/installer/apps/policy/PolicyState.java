package qz.installer.apps.policy;

import com.sun.jna.platform.win32.WinReg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.installer.apps.locator.AppAlias;
import qz.utils.WindowsUtilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Container for policy lifecycle
 * <p>
 * Contents:
 * <ul>
 *  <li><code>alias</code>: Information about the app for calculating location</li>
 *  <li><code>phase</code>: <code>Phase</code> (</code><code>INSTALL</code> or <code>REMOVAL</code>) of policy</li>
 *  <li><code>type</code>: <code>Type</code> of values Single <code>VALUE</code>, <code>ARRAY</code> of values or <code>MAP</code> of String to value</li>
 *  <li><code>name</code>: <code>String</code> policy name</li>
 *  <li><code>location</code>: Location <code>Path</code> of policy to change</li>
 *   <ul><li><code>hkey</code>: Windows-only <code>HKEY</code> for storing <code>HKLM</code>, <code>HKCU</code>, etc</li></ul>
 *  <li><code>status</code>: <code>Status</code> of the change (<code>STARTED</code>, <code>SUCCEEDED</code>,<code>FAILED</code> or <code>SKIPPED</code>)</li>
 * </ul>
 * </p>
 */
public class PolicyState {
    private static final Logger log = LogManager.getLogger(PolicyState.class);

    public enum Type {
        VALUE,
        ARRAY,
        MAP
    }

    public enum Status {
        STARTED,
        SUCCEEDED,
        FAILED
    }

    final AppAlias.Alias alias;
    final PolicyInstaller.Phase phase;
    final Type type;
    final String name;
    final Path location;
    final WinReg.HKEY hkey; // win only

    Exception exception;
    Status status;
    String reason;

    public PolicyState(AppAlias.Alias alias, PolicyInstaller.Phase phase, Type type, String name, Path location, WinReg.HKEY hkey) {
        this.alias = alias;
        this.phase = phase;
        this.type = type;
        this.name = name;
        if(hkey != null && (type == Type.ARRAY || type == Type.MAP)) {
            // Special handling for Windows arrays: name is the reg key
            this.location = Paths.get(String.format("%s\\%s", location.toString(), name));
        } else {
            this.location = location;
        }
        this.hkey = hkey;

        this.status = Status.STARTED;
        this.exception = null;
    }

    public Exception getException() {
        return exception;
    }

    public Type getType() {
        return type;
    }

    public PolicyInstaller.Phase getPhase() {
        return phase;
    }

    public boolean hasFailed() {
        return status == Status.FAILED;
    }

    public String getName() {
        return name;
    }

    public Path getLocation() {
        return location;
    }

    public WinReg.HKEY getHkey() {
        return hkey;
    }

    public PolicyState setFailed() {
        return setStatus(Status.FAILED, null, null);
    }

    public PolicyState setFailed(String reason) {
        return setStatus(Status.FAILED, reason, null);
    }

    public PolicyState setFailed(Exception exception) {
        return setStatus(Status.FAILED, null, exception);
    }

    public Object failIfNull(Object value) {
        if(value == null) {
            setFailed("Returned value is null");
        } else {
            setSucceeded();
        }
        return value;
    }

    public Object[] failIfNull(Object[] values) {
        if(values == null) {
            setFailed("Returned values are null");
            return new Object[0];
        } else {
            setSucceeded(values.length == 0 ? String.format("Location '%s' is missing array entry for '%s', returning an empty array", location, name) : null);
        }
        return values;
    }

    public Map<String, Object> failIfNull(Map<String, Object> map) {
        if(map == null) {
            setFailed("Returned map is null");
            return new HashMap<>();
        } else {
            setSucceeded(map.isEmpty() ? String.format("Location '%s' is missing map entry for '%s', returning an empty map", location, name) : null);
        }
        return map;
    }

    public PolicyState setSucceeded() {
        return setStatus(Status.SUCCEEDED, null, null);
    }

    public PolicyState setSucceeded(String reason) {
        return setStatus(Status.SUCCEEDED, reason, null);
    }

    public PolicyState setSucceeded(boolean succeeded) {
        return setStatus(succeeded ? Status.SUCCEEDED : Status.FAILED, null, null);
    }

    private PolicyState setStatus(Status status, String reason, Exception exception) {
        this.status = status;
        this.reason = reason;
        this.exception = exception;
        return this;
    }

    public PolicyState reset() {
        this.status = Status.STARTED;
        this.reason = null;
        this.exception = null;
        return this;
    }

    @Override
    public String toString() {
        String lowerPhase = phase.name().toLowerCase(Locale.ENGLISH);
        String titlePhase = titleCase(lowerPhase);

        switch(status) {
            case STARTED:
                return String.format("%sing", titlePhase);
            case SUCCEEDED:
                return String.format("Successfully %sed", lowerPhase);
            case FAILED:
                return String.format("Error occurred %sing", lowerPhase);
            default:
                return titlePhase;
        }
    }

    private static String titleCase(String string) {
        String lower = string.toLowerCase(Locale.ENGLISH);
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    public PolicyState log() {
        String browserClass = titleCase(alias.getAppAlias().name());
        String reasonText = "";
        if(reason != null) {
            reasonText = String.format(" (%s)", String.join(", ", reason));
        }
        String location = (hkey == null ? this.location.toString() :
                String.format("%s\\%s", WindowsUtilities.getHkeyName(hkey), this.location));

        String message = String.format("%s %s (%s) policy '%s' %s '%s'%s%s",
                                       this,
                                       browserClass,
                                       alias.getName(false),
                                       name,
                                       phase == PolicyInstaller.Phase.INSTALL ? "to" : "from",
                                       location,
                                       reasonText,
                                       status == Status.STARTED ? "..." : "");

        if(exception != null) {
            log.error(message, exception);
        } else {
            log.info(message);
        }
        return this;
    }
}
