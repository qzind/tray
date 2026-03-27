package qz.installer.provision;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.build.provision.Step;
import qz.build.provision.params.Os;
import qz.build.provision.params.Phase;
import qz.build.provision.params.Type;
import qz.common.Constants;
import qz.common.PropertyHelper;
import qz.installer.apps.policy.PolicyInstaller;
import qz.installer.certificate.CertificateManager;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProvisionerInstallerTests {
    private static final Logger log = LogManager.getLogger(ProvisionerInstallerTests.class);

    @DataProvider(name = "steps")
    public Iterator<Object[]> steps() {
        try(InputStream in = ProvisionerInstallerTests.class.getResourceAsStream("resources/" + Constants.PROVISION_FILE)) {
            ProvisionInstaller provisionInstaller = new ProvisionInstaller(ProvisionerInstallerTests.class, in);
            List<Step> steps = provisionInstaller.getSteps();
            List<Step> clones = new ArrayList<>();

            for(Step step : steps) {
                if(step.getType() == Type.POLICY && step.getPhase() != Phase.UNINSTALL) {
                    // Cleanup policies by explicitly uninstalling them (via clone)
                    String newDescription = step.getDescription().replace(
                            String.format("at '%s'", step.getPhase()),
                            String.format("at '%s'", PolicyInstaller.Phase.UNINSTALL.slug())
                    );
                    clones.add(step.cloneTo(Phase.UNINSTALL, newDescription));
                }
            }
            steps.addAll(clones);
            return steps.stream().map(step -> new Object[]{ step }).iterator();
        } catch(Throwable t) {
            log.error("An unexpected exception occurred", t);
        }
        return null;
    }

    @BeforeClass
    public void createPropertiesFiles() {
        createPropertiesFile(FileUtilities.USER_DIR.resolve(Constants.PREFS_FILE + ".properties"));

        if(SystemUtilities.getJarParentPath() != null) {
            createPropertiesFile(SystemUtilities.getJarParentPath().resolve(Constants.PROPS_FILE + ".properties"));
        } else {
            log.error("Can't create '{}' file", Constants.PROPS_FILE + ".properties");
        }
    }

    /**
     * Tests only whether the provision step succeeds based on expectation.
     * For validating written values, write dedicated tests elsewhere.
     */
    @Test(dataProvider = "steps")
    public void provisionInstallerTests(Step step) throws JSONException {
        boolean expected = !step.getDescription().contains("ERROR EXPECTED") && // description says so
                Os.matchesHost(step.getOs()) &&  // wrong os
                step.getType() != Type.CONF; // depends on mutable jvm runtime

        boolean actual;
        try {
            actual = ProvisionInstaller.invokeStep(step);
        } catch(Exception ignore) {
            actual = false;
        }
        Assert.assertEquals(actual, expected);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public static void createPropertiesFile(Path path) {
        PropertyHelper helper = new PropertyHelper(path.toFile());
        helper.setProperty("ProvisionDummyEntry", "test value");
        if(!helper.save()) {
            log.error("Couldn't create properties file '{}', those tests will fail", path);
        }
    }
}
