package qz.installer.provision;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.build.provision.Step;
import qz.build.provision.params.Os;
import qz.build.provision.params.Phase;
import qz.build.provision.params.types.Script;
import qz.build.provision.params.types.Software;
import qz.common.Constants;
import qz.installer.provision.invoker.*;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

import static qz.common.Constants.*;
import static qz.utils.FileUtilities.*;

public class ProvisionInstaller {
    protected static final Logger log = LogManager.getLogger(ProvisionInstaller.class);
    private ArrayList<Step> steps;

    static {
        // Populate variables for scripting environment
        ShellUtilities.addEnvp("APP_TITLE", ABOUT_TITLE,
                               "APP_VERSION", VERSION,
                               "APP_ABBREV", PROPS_FILE,
                               "APP_VENDOR", ABOUT_COMPANY,
                               "APP_VENDOR_ABBREV", DATA_DIR,
                               "APP_ARCH", SystemUtilities.getArch(),
                               "APP_OS", SystemUtilities.getOs(),
                               "APP_DIR", SystemUtilities.getAppPath(),
                               "APP_USER_DIR", USER_DIR,
                               "APP_SHARED_DIR", SHARED_DIR);
    }

    public ProvisionInstaller(Path relativePath) throws IOException, JSONException {
        this(relativePath, relativePath.resolve(Constants.PROVISION_FILE).toFile());
    }

    public ProvisionInstaller(Path relativePath, File jsonFile) throws IOException, JSONException {
        if(!jsonFile.exists()) {
            log.info("Provision file not found '{}', skipping", jsonFile);
            this.steps = new ArrayList<>();
            return;
        }
        this.steps = parse(FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8), relativePath);
    }

    /**
     * Package private for internal testing only
     * Assumes files located in ./resources/ subdirectory
     */
    ProvisionInstaller(Class relativeClass, InputStream in) throws IOException, JSONException {
        this(relativeClass, IOUtils.toString(in, StandardCharsets.UTF_8));
    }

    /**
     * Package private for internal testing only
     * Assumes files located in ./resources/ subdirectory
     */
    ProvisionInstaller(Class relativeClass, String jsonData) throws JSONException {
        this.steps = parse(jsonData, relativeClass);
    }

    public void invoke(Phase phase) {
        for(Step step : this.steps) {
            if(phase == null || step.getPhase() == phase) {
                try {
                    invokeStep(step);
                }
                catch(Exception e) {
                    log.error("[PROVISION] Provisioning step failed '{}'", step, e);
                }
            }
        }
    }

    public void invoke() {
        invoke(null);
    }

    private static ArrayList<Step> parse(String jsonData, Object relativeObject) throws JSONException {
        return parse(new JSONArray(jsonData), relativeObject);
    }

    private boolean invokeStep(Step step) throws Exception {
        if(Os.matchesHost(step.getOs())) {
            log.info("[PROVISION] Invoking step '{}'", step.toString());
        } else {
            log.info("[PROVISION] Skipping step for different OS '{}'", step.toString());
            return false;
        }

        Invokable invoker;
        switch(step.getType()) {
            case CA:
                invoker = new CaInvoker(step, PropertyInvoker.getProperties(step));
                break;
            case CERT:
                invoker = new CertInvoker(step);
                break;
            case CONF:
                invoker = new ConfInvoker(step);
                break;
            case SCRIPT:
                invoker = new ScriptInvoker(step);
                break;
            case SOFTWARE:
                invoker = new SoftwareInvoker(step);
                break;
            case REMOVER:
                invoker = new RemoverInvoker(step);
                break;
            case RESOURCE:
                invoker = new ResourceInvoker(step);
                break;
            case PREFERENCE:
                invoker = new PropertyInvoker(step, PropertyInvoker.getPreferences(step));
                break;
            case PROPERTY:
                invoker = new PropertyInvoker(step, PropertyInvoker.getProperties(step));
                break;
            default:
                throw new UnsupportedOperationException("Type " + step.getType() + " is not yet supported.");
        }
        return invoker.invoke();
    }

    public ArrayList<Step> getSteps() {
        return steps;
    }

    private static ArrayList<Step> parse(JSONArray jsonArray, Object relativeObject) throws JSONException {
        ArrayList<Step> steps = new ArrayList<>();
        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonStep = jsonArray.getJSONObject(i);
            try {
                steps.add(Step.parse(jsonStep, relativeObject));
            } catch(Exception e) {
                log.warn("[PROVISION] Unable to add step '{}'", jsonStep, e);
            }
        }
        return steps;
    }

    public static boolean shouldBeExecutable(Path path) {
        return Script.parse(path) != null || Software.parse(path) != Software.UNKNOWN;
    }
}
