package qz.installer.apps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AppVersionParserTests {
    private static final Logger log = LogManager.getLogger(AppVersionParserTests.class);

    @DataProvider(name = "versions")
    public Object[][] versions() {
        return new Object[][] {
                // input       // expected
                { "1.2.3.4.5", "1.2.3+4.5"},
                { "1.2.3.4", "1.2.3+4" },
                { "1.2.3-4", "1.2.3-4"},
                { "alpha", "0.0.0-alpha"},
                { "1.2a1", "1.2.0-a1"},
                { "1x", "1.0.0+x"},
                { "", "0.0.0"},

                // known chrome versions
                { "144.0.7559.112", "144.0.7559+112"},
                // known firefox versions
                { "60.9.0-ESR", "60.9.0-ESR"},
                { "115.32.0esr", "115.32.0+esr"},
                { "146.0", "146.0.0"},
                { "149.0a1", "149.0.0-a1"},
                // weird exceptions
                { "3.10.0rc1.post1", "3.10.0-rc1.post1" },
                { "1alpha", "1.0.0-alpha"},
                { "1.2alpha", "1.2.0-alpha"},
                { "1.2.0-beta", "1.2.0-beta"},
                { "1.2sigma", "1.2.0+sigma"},
                { "1.2.0pre", "1.2.0-pre"},
                { "1.2.0pretty", "1.2.0+pretty"},
                { "1.2.0rc", "1.2.0-rc"},
                { "1.2.0rc1", "1.2.0-rc1"},
                { "1.2.0+rc1", "1.2.0+rc1"},
        };
    }

    @Test(dataProvider = "versions")
    public void versionTests(String input, String expected) {
        Assert.assertEquals(AppVersionParser.parse(input).toString(), expected);
    }
}
