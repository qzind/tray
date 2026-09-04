package qz.utils.linux.wm.dispatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qz.utils.SystemUtilities;
import qz.utils.linux.LinuxUtilities;
import qz.utils.linux.wm.WmAdjuster;
import qz.utils.linux.wm.Wm;

import java.awt.*;

public class ThemerTests {
    static Logger log = LogManager.getLogger(ThemerTests.class);

    private Wm wm;
    private Themer themer;

    private String currentTheme;

    @BeforeClass
    public void beforeFindDe() {
        Executor.setDebug(true);

        switch(SystemUtilities.getOs()) {
            case WINDOWS, MAC -> throw new SkipException("Desktop environment is assumed on this OS");
            default -> {
                if(GraphicsEnvironment.isHeadless()) {
                    log.warn("Skipping desktop environment tests in headless mode");
                    throw new SkipException("Skipping desktop environment tests in headless mode");
                }
            }
        }

        wm = Wm.detectWm();
        themer = (Themer)WmAdjuster.getDispatcher();
    }

    @Test(priority = -2)
    public void testHasWm() {
        Assert.assertNotNull(wm);
        log.info("Detected '{}' wm", wm);
        Assert.assertNotNull(themer);
        log.info("Will use '{}' dispatcher for running tests", themer);
    }

    @Test
    public void testSaveCurrentTheme() {
        currentTheme = themer.getTheme();
        Assert.assertNotNull(currentTheme);
        log.info("Current theme saved '{}', we'll restore it after tests are done", currentTheme);
    }

    @Test
    public void testSetDarkTheme() {
        themer.setTheme(true);
        Assert.assertTrue(LinuxUtilities.isDarkDesktop());
        log.info("Dark desktop confirmed");
    }

    @Test
    public void testSetLightTheme() {
        themer.setTheme(false);
        Assert.assertFalse(LinuxUtilities.isDarkDesktop());
        log.info("Light desktop confirmed");
    }

    @Test(priority = 99)
    public void testRestoreCurrentTheme() {
        themer.setTheme(currentTheme);
        Assert.assertEquals(currentTheme, themer.getTheme());
        log.info("Restored theme: '{}'", currentTheme);
    }

    @Test
    public void testGetScaleFactor() {
        Double scaleFactor = LinuxUtilities.getScaleFactor();
        log.info("Detected scale factor {}x from {}", scaleFactor, wm);
        Assert.assertNotNull(scaleFactor);
        Assert.assertNotEquals(scaleFactor, 0.0);
    }
}
