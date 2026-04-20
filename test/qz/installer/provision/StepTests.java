package qz.installer.provision;

import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import qz.build.provision.Step;

public class StepTests {
    private static JSONObject basePolicyStep() throws Exception {
        return new JSONObject()
                .put("description", "policy scalar roundtrip")
                .put("type", "policy")
                .put("os", "*")
                .put("phase", "install")
                .put("app", "chromium")
                .put("name", "IncognitoModeAvailability")
                .put("format", "plain");
    }

    @Test
    public void policyIntegerDataShouldRemainInteger() throws Exception {
        Step step = Step.parse(basePolicyStep().put("data", 1), StepTests.class);

        Object data = step.getDataObject();
        Assert.assertTrue(data instanceof Integer, "Expected integer data but found " + data.getClass().getName());
        Assert.assertEquals(data, 1);
    }

    @Test
    public void policyBooleanDataShouldRemainBoolean() throws Exception {
        Step step = Step.parse(basePolicyStep().put("data", true), StepTests.class);

        Object data = step.getDataObject();
        Assert.assertTrue(data instanceof Boolean, "Expected boolean data but found " + data.getClass().getName());
        Assert.assertEquals(data, true);
    }

    @Test
    public void policyFloatDataShouldRemainNumeric() throws Exception {
        Step step = Step.parse(basePolicyStep().put("data", 1.25), StepTests.class);

        Object data = step.getDataObject();
        Assert.assertTrue(data instanceof Number, "Expected numeric data but found " + data.getClass().getName());
        Assert.assertEquals(((Number)data).doubleValue(), 1.25, 0.0001);
    }
}
