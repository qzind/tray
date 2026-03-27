package qz.installer.provision;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.build.provision.Step;
import qz.build.provision.params.Os;
import qz.build.provision.params.Type;
import qz.common.Constants;
import qz.utils.SystemUtilities;

import java.io.IOException;
import java.io.InputStream;

public class ProvisionerInstallerTests {
    private static final Logger log = LogManager.getLogger(ProvisionerInstallerTests.class);

    @DataProvider(name = "steps")
    public Object[][] steps() throws JSONException {
        try(InputStream in = ProvisionerInstallerTests.class.getResourceAsStream("resources/" + Constants.PROVISION_FILE)) {
            ProvisionInstaller provisionInstaller = new ProvisionInstaller(ProvisionerInstallerTests.class, in);
            return provisionInstaller.getSteps().stream().map(step -> new Object[] { step })
                    .toArray(Object[][]::new);
        } catch(IOException e) {
            Assert.fail("Unable to read JSON file", e);
        }
        return null;
    }

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
}
