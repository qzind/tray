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

    @Test
    public void missingDebianPromptTests() {
        Assert.assertTrue(LinuxInstaller.shouldPromptLpr(LinuxInstaller.LprState.MISSING_DEBIAN, false, false));
    }

    @Test
    public void presentPromptTests() {
        Assert.assertFalse(LinuxInstaller.shouldPromptLpr(LinuxInstaller.LprState.PRESENT, false, false));
    }

    @Test
    public void missingUnsupportedPromptTests() {
        Assert.assertFalse(LinuxInstaller.shouldPromptLpr(LinuxInstaller.LprState.MISSING_UNSUPPORTED, false, false));
    }

    @Test
    public void silentPromptTests() {
        Assert.assertFalse(LinuxInstaller.shouldPromptLpr(LinuxInstaller.LprState.MISSING_DEBIAN, true, false));
    }

    @Test
    public void headlessPromptTests() {
        Assert.assertFalse(LinuxInstaller.shouldPromptLpr(LinuxInstaller.LprState.MISSING_DEBIAN, false, true));
    }
}
