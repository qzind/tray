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
import qz.build.provision.params.Type;
import qz.build.provision.params.types.Script;
import qz.build.provision.params.types.Software;
import qz.common.Constants;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        if(copyResource(step)) {
            log.info("[SUCCESS] Step successfully processed '{}'", step);
            jsonSteps.put(step.toJSON());
        } else {
            log.error("[SKIPPED] Resources could not be saved '{}'", step);
        }
    }

    /**
     * Save any resources files required for INSTALL and SCRIPT steps to provision folder
     */
    public boolean copyResource(Step step) throws IOException {
        switch(step.getType()) {
            case CERT:
            case SCRIPT:
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
}
