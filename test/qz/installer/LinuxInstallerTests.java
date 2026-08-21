package qz.installer;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LinuxInstallerTests {

    @Test
    public void presentTests() {
        Assert.assertEquals(
                LinuxInstaller.lprState(true, true),
                LinuxInstaller.LprState.PRESENT
        );
    }

    @Test
    public void missingDebianTests() {
        Assert.assertEquals(
                LinuxInstaller.lprState(true, false),
                LinuxInstaller.LprState.MISSING_DEBIAN
        );
    }

    @Test
    public void missingUnsupportedTests() {
        Assert.assertEquals(
                LinuxInstaller.lprState(false, false),
                LinuxInstaller.LprState.MISSING_UNSUPPORTED
        );
    }
}
