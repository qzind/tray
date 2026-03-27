package qz.build.provision;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.build.provision.params.Arch;
import qz.build.provision.params.Os;
import qz.build.provision.params.Phase;
import qz.build.provision.params.Type;
import qz.build.provision.params.types.Remover;
import qz.build.provision.params.types.Software;
import qz.common.Sluggable;
import qz.installer.apps.locator.AppFamily;
import qz.installer.apps.policy.PolicyState;

import java.nio.file.Path;
import java.util.*;

public class Step {
    protected static final Logger log = LogManager.getLogger(Step.class);

    String description;
    Type type;
    List<String> args; // Type.SCRIPT or Type.INSTALLER or Type.CONF only
    HashSet<Os> os;
    HashSet<Arch> arch;
    Phase phase;
    String data;
    AppFamily app;
    String name;
    String format;

    Path relativePath;
    Class<?> relativeClass;

    public Step(Path relativePath, String description, Type type, HashSet<Os> os, HashSet<Arch> arch, Phase phase, AppFamily app, String name, String format, String data, List<String> args) {
        this.relativePath = relativePath;
        this.description = description;
        this.type = type;
        this.os = os;
        this.arch = arch;
        this.phase = phase;
        this.data = data;
        this.args = args;
        this.app = app;
        this.name = name;
        this.format = format;
    }

    /**
     * Only should be used by unit tests
     */
    @SuppressWarnings("rawtypes")
    Step(Class relativeClass, String description, Type type, HashSet<Os> os, HashSet<Arch> arch, Phase phase, AppFamily app, String name, String format, String data, List<String> args) {
        this((Path)null, description, type, os, arch, phase, app, name, format, data, args);
        this.relativeClass = relativeClass;
    }

    @Override
    public String toString() {
        return String.format("Step { description='%s', type='%s', os='%s', arch='%s', phase='%s', app='%s', name='%s', format='%s', data='%s', args='%s' }",
                             description,
                             type,
                             Os.serialize(os),
                             Arch.serialize(arch),
                             phase,
                             Sluggable.slugOf(app),
                             name,
                             format,
                             data,
                             StringUtils.join(args, ","));
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("description", description)
                .put("type", type)
                .put("os", Os.serialize(os))
                .put("arch", Arch.serialize(arch))
                .put("phase", phase)
                .put("data", data);

        if(app != null) {
            json.put("app", Sluggable.slugOf(app))
                    .put("name", name)
                    .put("format", PolicyState.Type.parse(format, PolicyState.Type.VALUE));
        }

        for(int i = 0; i < args.size(); i++) {
            json.put(String.format("arg%s", i + 1), args.get(i));
        }
        return json;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public HashSet<Os> getOs() {
        return os;
    }

    public void setOs(HashSet<Os> os) {
        this.os = os;
    }

    public HashSet<Arch> getArch() {
        return arch;
    }

    public void setArch(HashSet<Arch> arch) {
        this.arch = arch;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public AppFamily getApp() {
        return app;
    }

    public void setApp(AppFamily app) {
        this.app = app;
    }

    public void setName(String name) { this.name = name; }

    public String getName() { return name; };

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Class<?> getRelativeClass() {
        return relativeClass;
    }

    public Path getRelativePath() {
        return relativePath;
    }

    public boolean usingClass() {
        return relativeClass != null;
    }

    public boolean usingPath() {
        return relativePath != null;
    }

    public static Step parse(JSONObject jsonStep, Object relativeObject) {
        String description = jsonStep.optString("description", "");
        Type type = Type.parse(jsonStep.optString("type", null));
        String data = jsonStep.optString("data", null);

        // Handle installer args
        List<String> args = new LinkedList<>();
        if(type == Type.SOFTWARE || type == Type.CONF) {
            // Handle space-delimited args
            args = Software.parseArgs(jsonStep.optString("args", ""));
            // Handle standalone single args (won't break on whitespace)
            // e.g. "arg1": "C:\Program Files\Foo"
            int argCounter = 0;
            while(true) {
                String singleArg = jsonStep.optString(String.format("arg%d", ++argCounter), "");
                if(!singleArg.trim().isEmpty()) {
                    args.add(singleArg.trim());
                } else {
                    // stop searching if the next incremental arg (e.g. "arg2") isn't found
                    break;
                }
            }
        }

        // Mandate "args" as the CONF path
        if(type == Type.CONF) {
            // Honor "path" first, if provided
            String path = jsonStep.optString("path", "");
            if(!path.isEmpty()) {
                args.add(0, path);
            }

            // Keep only the first value
            if(!args.isEmpty()) {
                args = args.subList(0, 1);
            } else {
                throw formatted("Conf path value cannot be blank.");
            }
        }

        HashSet<Os> os = new HashSet<>();
        if(jsonStep.has("os")) {
            // Do not tolerate bad os values
            String osString = jsonStep.optString("os");
            os = Os.parse(osString);
            if(os.isEmpty()) {
                throw formatted("Os provided '%s' could not be parsed", osString);
            }
        }

        HashSet<Arch> arch = new HashSet<>();
        if(jsonStep.has("arch")) {
            // Do not tolerate bad arch values
            String archString = jsonStep.optString("arch");
            arch = Arch.parse(archString);
            if(arch.isEmpty()) {
                throw formatted("Arch provided \"%s\" could not be parsed", archString);
            }
        }

        Phase phase = null;
        if(jsonStep.has("phase")) {
            String phaseString = jsonStep.optString("phase", null);
            phase = Phase.parse(phaseString);
            if(phase == null) {
                log.warn("Phase provided \"{}\" could not be parsed", phaseString);
            }
        }

        AppFamily app = null;
        if(jsonStep.has("app")) {
            String appString = jsonStep.optString("app", null);
            app = AppFamily.parse(appString, null);
            if(app == null) {
                log.warn("App provided \"{}\" could not be parsed", appString);
            }
        }

        String name = jsonStep.optString("name", null);

        String format = jsonStep.optString("format", "plain");

        Step step;
        if(relativeObject instanceof Path) {
            step = new Step((Path)relativeObject, description, type, os, arch, phase, app, name, format, data, args);
        } else if(relativeObject instanceof Class) {
            step = new Step((Class<?>)relativeObject, description, type, os, arch, phase, app, name, format, data, args);
        } else {
            throw formatted("Parameter relativeObject must be of type 'Path' or 'Class' but '%s' was provided", relativeObject.getClass());
        }
        return step.sanitize();
    }

    private Step sanitize() {
        return throwIfNull("Type", type)
                .throwIfNull("Data", data)
                .validateOs()
                .validateArch()
                .validatePolicy()
                .enforcePhase(Type.PREFERENCE, Phase.STARTUP)
                .enforcePhase(Type.CA, Phase.CERTGEN)
                .enforcePhase(Type.CERT, Phase.STARTUP)
                .enforcePhase(Type.CONF, Phase.CERTGEN)
                .enforcePhase(Type.SOFTWARE, Phase.INSTALL)
                .enforcePhase(Type.REMOVER, Phase.INSTALL)
                .enforcePhase(Type.POLICY, Phase.CERTGEN, Phase.INSTALL, Phase.UNINSTALL)
                .enforcePhase(Type.PROPERTY, Phase.CERTGEN, Phase.INSTALL)
                .validateRemover();
    }

    private Step validateRemover() {
        if(type != Type.REMOVER) {
            return this;
        }
        Remover remover = Remover.parse(data);
        switch(remover) {
            case CUSTOM:
                break;
            case QZ:
            default:
                if(remover.matchesCurrentSystem()) {
                    throw formatted("Remover '%s' would conflict with this installer, skipping. ", remover);
                }
                return this;
        }

        // Custom removers must have three elements
        if(data == null || data.split(",").length != 3) {
            throw formatted("Remover data '%s' is invalid.  Data must match a known type [%s] or contain exactly 3 elements.", data, Remover.valuesDelimited(","));
        }
        return this;
    }

    private Step throwIfNull(String name, Object value) {
        if(value == null) {
            throw formatted("%s cannot be null", name);
        }
        return this;
    }

    private Step validateOs() {
        if(os == null) {
            os = new HashSet<>();
        }

        if(os.isEmpty()) {
            switch(type) {
                case SOFTWARE:
                    os.addAll(Software.parse(data).defaultOs());
                    break;
                case CONF:
                default:
                    os.add(Os.ALL);
            }
            log.debug("Os list is empty, assuming '{}'", os);
        }
        return this;
    }

    private Step validateArch() {
        if(arch == null) {
            arch = new HashSet<>();
        }
        if(arch.isEmpty()) {
            arch.add(Arch.ALL);
            log.debug("Arch list is empty, assuming '{}'", Arch.ALL);
        }
        return this;
    }

    private Step validatePolicy() {
        if(type == Type.POLICY && app == null) {
            String[] slugs = Arrays.stream(AppFamily.values()).map(AppFamily::slug).toArray(String[]::new);
            throw formatted("Policy requires a corresponding app value.  Possible values are: [%s]", String.join(",", slugs));
        }
        return this;
    }

    private Step enforcePhase(Type matchType, Phase ... requiredPhases) {
        if(requiredPhases.length == 0) {
            throw new UnsupportedOperationException("At least one Phase must be specified");
        }
        if(type == matchType) {
            for(Phase requiredPhase : requiredPhases) {
                if (phase == null) {
                    phase = requiredPhase;
                    log.debug("Phase is null, defaulting to '{}' based on Type '{}'", phase, type);
                    return this;
                } else if (phase == requiredPhase) {
                    return this;
                }
            }
            log.debug("Phase '{}' is unsupported for Type '{}', defaulting to '{}'", phase, type, phase = requiredPhases[0]);
        }
        return this;
    }

    private static UnsupportedOperationException formatted(String message, Object ... args) {
        String formatted = String.format(message, args);
        return new UnsupportedOperationException(formatted);
    }

    public Step cloneTo(Phase phase, String description) {
       return relativePath != null ?
                    new Step(relativePath, description, type, os, arch, phase, app, name, format, data, args) :
                    new Step(relativeClass, description, type, os, arch, phase, app, name, format, data, args);
    }

    Step cloneTo(Phase phase) {
        return cloneTo(phase, description);
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Step clone() {
        return cloneTo(this.phase);
    }
}
