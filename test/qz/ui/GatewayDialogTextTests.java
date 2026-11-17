package qz.ui;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.common.Constants;

public class GatewayDialogTextTests {
    @DataProvider(name = "certNames")
    public Object[][] certNames() {
        return new Object[][] {
                { "QZ Industries, LLC" },
                { "Example Org" },
                { "" },
                { "Permanently blocked \" blocked \" from" },
                { "Multi\nline \"quoted\"" },
        };
    }

    @Test(dataProvider = "certNames")
    public void testConfirmTextMatchesLegacyDerivation(String certName) {
        String legacy = String.format(Constants.BLOCK_SITES_TEXT.replace(" blocked ", " block ") + "?", certName);
        Assert.assertEquals(String.format(Constants.BLOCK_SITES_CONFIRM, certName), legacy);
    }

    @Test
    public void testConfirmTextIsSelfContained() {
        Assert.assertEquals(Constants.BLOCK_SITES_CONFIRM, "Permanently block \"%s\" from accessing local resources?");
        Assert.assertFalse(Constants.BLOCK_SITES_CONFIRM.contains(" blocked "));
    }
}
