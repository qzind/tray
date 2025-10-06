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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class Step {
    protected static final Logger log = LogManager.getLogger(Step.class);

    String description;
    Type type;
    List<String> args; // Type.SCRIPT or Type.INSTALLER or Type.CONF only
    HashSet<Os> os;
    HashSet<Arch> arch;
    Phase phase;
    String data;

    Path relativePath;
    Class relativeClass;

    public Step(Path relativePath, String description, Type type, HashSet<Os> os, HashSet<Arch> arch, Phase phase, String data, List<String> args) {
        this.relativePath = relativePath;
        this.description = description;
        this.type = type;
        this.os = os;
        this.arch = arch;
        this.phase = phase;
        this.data = data;
        this.args = args;
    }

    /**
     * Only should be used by unit tests
     */
    Step(Class relativeClass, String description, Type type, HashSet<Os> os, HashSet<Arch> arch, Phase phase, String data, List<String> args) {
        this.relativeClass = relativeClass;
        this.description = description;
        this.type = type;
        this.os = os;
        this.arch = arch;
        this.phase = phase;
        this.data = data;
        this.args = args;
    }

    @Override
    public String toString() {
        return "Step { " +
                "description=\"" + description + "\", " +
                "type=\"" + type + "\", " +
                "os=\"" + Os.serialize(os) + "\", " +
                "arch=\"" + Arch.serialize(arch) + "\", " +
                "phase=\"" + phase + "\", " +
                "data=\"" + data + "\", " +
                "args=\"" + StringUtils.join(args, ",") + "\" " +
                "}";
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("description", description)
                .put("type", type)
                .put("os", Os.serialize(os))
                .put("arch", Arch.serialize(arch))
                .put("phase", phase)
                .put("data", data);

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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Class getRelativeClass() {
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
            if(args.size() > 0) {
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
            if(os.size() == 0) {
                throw formatted("Os provided '%s' could not be parsed", osString);
            }
        }

        HashSet<Arch> arch = new HashSet<>();
        if(jsonStep.has("arch")) {
            // Do not tolerate bad arch values
            String archString = jsonStep.optString("arch");
            arch = Arch.parse(archString);
            if(arch.size() == 0) {
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
        Step step;
        if(relativeObject instanceof Path) {
            step = new Step((Path)relativeObject, description, type, os, arch, phase, data, args);
        } else if(relativeObject instanceof Class) {
            step = new Step((Class)relativeObject, description, type, os, arch, phase, data, args);
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
                .enforcePhase(Type.PREFERENCE, Phase.STARTUP)
                .enforcePhase(Type.CA, Phase.CERTGEN)
                .enforcePhase(Type.CERT, Phase.STARTUP)
                .enforcePhase(Type.CONF, Phase.CERTGEN)
                .enforcePhase(Type.SOFTWARE, Phase.INSTALL)
                .enforcePhase(Type.REMOVER, Phase.INSTALL)
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
            if(type == Type.SOFTWARE) {
                // Software must default to a sane operating system
                os = Software.parse(data).defaultOs();
            } else {
                os = new HashSet<>();
            }
        }
        if(os.size() == 0) {
            os.add(Os.ALL);
            log.debug("Os list is null, assuming '{}'", Os.ALL);
        }
        return this;
    }

    private Step validateArch() {
        if(arch == null) {
            arch = new HashSet<>();
        }
        if(arch.size() == 0) {
            arch.add(Arch.ALL);
            log.debug("Arch list is null, assuming '{}'", Arch.ALL);
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
    Step cloneTo(Phase phase) {
       return relativePath != null ?
                    new Step(relativePath, description, type, os, arch, phase, data, args) :
                    new Step(relativeClass, description, type, os, arch, phase, data, args);
    }

    public Step clone() {
        return cloneTo(this.phase);
    }
}
