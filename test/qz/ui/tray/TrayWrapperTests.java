package qz.ui.tray;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.build.provision.params.Os;

public class TrayWrapperTests {
    @DataProvider
    public Object[][] selections() {
        return new Object[][] {
                {false, Os.WINDOWS, false},
                {false, Os.MAC, false},
                {false, Os.LINUX, false},
                {false, Os.SOLARIS, false},
                {false, Os.ALL, false},
                {false, Os.UNKNOWN, false},
                {true, Os.WINDOWS, true},
                {true, Os.MAC, false},
                {true, Os.LINUX, false},
                {true, Os.SOLARIS, false},
                {true, Os.ALL, false},
                {true, Os.UNKNOWN, false}
        };
    }

    @Test(dataProvider = "selections")
    public void testSelection(boolean supported, Os os, boolean expected) {
        Assert.assertEquals(TrayWrapper.useSystemTray(supported, os), expected);
    }
}
