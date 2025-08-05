package qz.build.provision;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.build.provision.params.Arch;
import qz.build.provision.params.Os;
import qz.build.provision.params.Phase;
import qz.build.provision.params.Type;
import qz.common.Constants;
import qz.installer.provision.invoker.PropertyInvoker;
import qz.utils.ArgValue;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class ProvisionBuilder {
    protected static final Logger log = LogManager.getLogger(ProvisionBuilder.class);

    public static final Path BUILD_PROVISION_FOLDER = SystemUtilities.getJarParentPath().resolve(Constants.PROVISION_DIR);
    public static final File BUILD_PROVISION_FILE = BUILD_PROVISION_FOLDER.resolve(Constants.PROVISION_FILE).toFile();

    private File ingestFile;
    private JSONArray jsonSteps;
    private Arch targetArch;
    private Os targetOs;

    /**
     * Parses command line input to create a "provision" folder in the dist directory for customizing the installation or startup
     */
    public ProvisionBuilder(String type, String phase, String os, String arch, String data, String args, String description, String ... varArgs) throws IOException, JSONException {
        createProvisionDirectory(false);

        targetOs = Os.ALL;
        targetArch = Arch.ALL;
        jsonSteps = new JSONArray();

        // Wrap into JSON so that we can save it
        JSONObject jsonStep = new JSONObject();
        putPattern(jsonStep, "description", description);
        putPattern(jsonStep, "type", type);
        putPattern(jsonStep, "phase", phase);
        putPattern(jsonStep, "os", os);
        putPattern(jsonStep, "arch", arch);
        putPattern(jsonStep, "data", data);
        putPattern(jsonStep, "args", args);
        putPattern(jsonStep, "arg%d", varArgs);

        // Command line invocation, use the working directory
        Path relativePath = Paths.get(System.getProperty("user.dir"));
        ingestStep(jsonStep, relativePath);
    }

    /**
     * To be called by ant's <code>provision</code> target
     */
    public ProvisionBuilder(File antJsonFile, String antTargetOs, String antTargetArch) throws IOException, JSONException {
        createProvisionDirectory(true);

        // Calculate the target os, architecture
        this.targetArch = Arch.parseStrict(antTargetArch);
        this.targetOs = Os.parseStrict(antTargetOs);

        this.jsonSteps = new JSONArray();
        this.ingestFile = antJsonFile;

        String jsonData = FileUtils.readFileToString(antJsonFile, StandardCharsets.UTF_8);
        JSONArray pendingSteps = new JSONArray(jsonData);

        // Cycle through so that each Step can be individually processed
        Path relativePath = antJsonFile.toPath().getParent();
        for(int i = 0; i < pendingSteps.length(); i++) {
            JSONObject jsonStep = pendingSteps.getJSONObject(i);
            System.out.println();
            try {
                ingestStep(jsonStep, relativePath);
            } catch(Exception e) {
                log.warn("[SKIPPED] Step '{}'", jsonStep, e);
            }
        }

    }

    public JSONArray getJson() {
        return jsonSteps;
    }

    /**
     * Construct as a Step to perform basic parsing/sanity checks
     * Copy resources (if needed) to provisioning directory
     */
    private void ingestStep(JSONObject jsonStep, Path relativePath) throws JSONException, IOException {
        Step step = Step.parse(jsonStep, relativePath);
        if(!targetOs.matches(step.os)) {
            log.info("[SKIPPED] Os '{}' does not match target Os '{}' '{}'", Os.serialize(step.os), targetOs, step);
            return;
        }

        if(!targetArch.matches(step.arch)) {
            log.info("[SKIPPED] Arch '{}' does not match target Os '{}' '{}'", Arch.serialize(step.arch), targetArch, step);
            return;
        }

        // Inject any special inferences (such as inferring resources from args)
        inferAdditionalSteps(step);

        if(copyResource(step)) {
            log.info("[SUCCESS] Step successfully processed '{}'", step);
            jsonSteps.put(step.toJSON());
            // Special case for custom websocket ports
            if(step.getType() == Type.PROPERTY && step.getPhase() == Phase.CERTGEN) {
                HashMap<String, String> pairs = PropertyInvoker.parsePropertyPairs(step);
                if(pairs.get(ArgValue.WEBSOCKET_SECURE_PORTS.getMatch()) != null ||
                        pairs.get(ArgValue.WEBSOCKET_INSECURE_PORTS.getMatch()) != null) {
                    // Clone to install step
                    jsonSteps.put(step.cloneTo(Phase.INSTALL).toJSON());
                }
            }
        } else {
            log.error("[SKIPPED] Resources could not be saved '{}'", step);
        }
    }

    /**
     * Save any resources files required for INSTALL and SCRIPT steps to provision folder
     */
    public boolean copyResource(Step step) throws IOException {
        switch(step.getType()) {
            case CA:
            case CERT:
            case SCRIPT:
            case RESOURCE:
            case SOFTWARE:
                boolean isRelative = !Paths.get(step.getData()).isAbsolute();
                File src;
                if(isRelative) {
                    if(ingestFile != null) {
                        Path parentDir = ingestFile.getParentFile().toPath();
                        src = parentDir.resolve(step.getData()).toFile();
                    } else {
                        throw formatted("Unable to resolve path: '%s' '%s'", step.getData(), step);
                    }
                } else {
                    src = new File(step.getData());
                }
                String fileName = src.getName();
                if(fileName.equals(BUILD_PROVISION_FILE.getName())) {
                    throw formatted("Resource name conflicts with provision file '%s' '%s'", fileName, step);
                }
                File dest = BUILD_PROVISION_FOLDER.resolve(fileName).toFile();
                int i = 0;
                // Avoid conflicting file names
                String name = dest.getName();

                // Avoid resource clobbering when being invoked by command line or providing certificates.
                // Otherwise, assume the intent is to re-use the same resource (e.g. "my_script.sh", etc)
                if(ingestFile == null || step.getType() == Type.CERT) {
                    while(dest.exists()) {
                        // Append "filename-1.txt" until there's no longer a conflict
                        if (name.contains(".")) {
                            dest = BUILD_PROVISION_FOLDER.resolve(String.format("%s-%s.%s", FilenameUtils.removeExtension(name), ++i,
                                                                                FilenameUtils.getExtension(name))).toFile();
                        } else {
                            dest = BUILD_PROVISION_FOLDER.resolve(String.format("%-%", name, ++i)).toFile();
                        }
                    }
                }

                FileUtils.copyFile(src, dest);
                if(dest.exists()) {
                    step.setData(BUILD_PROVISION_FOLDER.relativize(dest.toPath()).toString());
                } else {
                    return false;
                }
                break;
            default:
        }
        return true;
    }

    /**
     * Appends the JSONObject to the end of the provisionFile
     */
    public boolean saveJson(boolean overwrite) throws IOException, JSONException {
        // Read existing JSON file if exists
        JSONArray mergeSteps;
        if(!overwrite && BUILD_PROVISION_FILE.exists()) {
            String jsonData = FileUtils.readFileToString(BUILD_PROVISION_FILE, StandardCharsets.UTF_8);
            mergeSteps = new JSONArray(jsonData);
        } else {
            mergeSteps = new JSONArray();
        }

        // Merge in new steps
        for(int i = 0; i < jsonSteps.length(); i++) {
            mergeSteps.put(jsonSteps.getJSONObject(i));
        }

        FileUtils.writeStringToFile(BUILD_PROVISION_FILE, mergeSteps.toString(3), StandardCharsets.UTF_8);
        return true;
    }

    /**
     * Convenience method for adding a name/value pair into the JSONObject
     */
    private static void putPattern(JSONObject jsonStep, String name, String val) throws JSONException {
        if(val != null && !val.isEmpty()) {
            jsonStep.put(name, val);
        }
    }

    /**
     * Convenience method for adding consecutive patterned value pairs into the JSONObject
     * e.g. --arg1 "foo" --arg2 "bar"
     */
    private static void putPattern(JSONObject jsonStep, String pattern, String ... varArgs) throws JSONException {
        int argCounter = 0;
        for(String arg : varArgs) {
            jsonStep.put(String.format(pattern, ++argCounter), arg);
        }
    }

    private static void createProvisionDirectory(boolean cleanDirectory) throws IOException {
        if(cleanDirectory) {
            FileUtils.deleteDirectory(BUILD_PROVISION_FOLDER.toFile());
        }
        if(BUILD_PROVISION_FOLDER.toFile().isDirectory()) {
            return;
        }
        if(BUILD_PROVISION_FOLDER.toFile().mkdirs()) {
            return;
        }
        throw formatted("Could not create provision destination: '%'", BUILD_PROVISION_FOLDER);
    }

    private static IOException formatted(String message, Object ... args) {
        String formatted = String.format(message, args);
        return new IOException(formatted);
    }

    /**
     * Returns the first index of the specified arg prefix pattern(s)
     *
     * e.g. if pattern is "/f1", it will return 1 from args { "/s", "/f1C:\foo" }
     */
    private int argPrefixIndex(Step step, String ... prefixes) {
        for(int i = 0; i < step.args.size() ; i++){
            for(String prefix : prefixes) {
                if (step.args.get(i).toLowerCase().startsWith(prefix.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the "value" of the specified arg prefix pattern(s)
     *
     * e.g. if pattern is "/f1", it will return "C:\foo" from args { "/s", "/f1C:\foo" }
     *
     */
    private String argPrefixValue(Step step, int index, String ... prefixes) {
        String arg = step.args.get(index);
        String value = null;
        for(String prefix : prefixes) {
            if (arg.toLowerCase().startsWith(prefix.toLowerCase())) {
                value = arg.substring(prefix.length());
                if((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                    // Remove surrounding quotes
                    value = value.substring(1, value.length() - 1);
                }
            }
        }
        return value;
    }

    /**
     * Clones the provided step into a new step that performs a prerequisite task.
     *
     * This is "magic" in the sense that it's highly specific to <code>Type</code>
     * <code>Os</code> and <code>Step.args</code>.
     *
     * For example:
     *
     *   Older InstallShield installers supported the <code>/f1</code> parameter which
     *   implies an answer file of which we need to bundle for a successful deployment.
     */
    private void inferAdditionalSteps(Step orig) throws JSONException, IOException {
        // Infer resource step for InstallShield .iss answer files
        if(orig.getType() == Type.SOFTWARE && Os.WINDOWS.matches(orig.getOs())) {
            String[] patterns = { "/f1", "-f1" };
            int index = argPrefixIndex(orig, patterns);
            if(index > 0) {
                String resource = argPrefixValue(orig, index, patterns);
                if(resource != null) {
                    // Clone to copy the Phase, Os and Description
                    Step step = orig.clone();

                    // Swap Type, clear args and update the data
                    step.setType(Type.RESOURCE);
                    step.setArgs(new ArrayList<>());
                    step.setData(resource);

                    if(copyResource(step)) {
                        File resourceFile = new File(resource);
                        jsonSteps.put(step.toJSON());
                        orig.getArgs().set(index, String.format("/f1\"%s\"", resourceFile.getName()));
                        log.info("[SUCCESS] Step successfully inferred and appended '{}'", step);
                    }  else {
                        log.error("[SKIPPED] Resources could not be saved '{}'", step);
                    }
                }
            }
        }
    }
}
