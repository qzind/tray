package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qz.utils.SystemUtilities;

import java.awt.*;
import java.io.IOException;

import static qz.utils.linux.DeType.*;

public class DeUtilsTests {
    static Logger log = LogManager.getLogger(DeUtilsTests.class);

    private DeType deType;
    private DeThemeHelper deThemeHelper;
    private String currentTheme;

    @BeforeClass
    public void findDeType() {
        switch(SystemUtilities.getOs()) {
            case WINDOWS, MAC -> throw new SkipException("Desktop environment is assumed on this OS");
            default -> {
                if(GraphicsEnvironment.isHeadless()) {
                    log.warn("Skipping desktop environment tests in headless mode");
                    throw new SkipException("Skipping desktop environment tests in headless mode");
                }
            }
        }

        deType = DeType.getDeType();
        deThemeHelper = DeThemeHelper.getDeThemeHelper();
    }

    @Test
    public void testDeToThemeCoverage() {
        for(DeType type : DeType.values()) {
            switch(type) {
                // Coverage is limited to GNOME, KDE for now
                case GNOME, KDE -> {
                    log.info("Checking desktop environment coverage for '{}'", type);
                    Assert.assertNotNull(DeThemeHelper.getDeThemeHelper(type));
                }
            }

        }
    }

    @Test
    public void testHasDe() {
        Assert.assertNotEquals(deType, UNKNOWN);
        log.info("Desktop environment detected '{}' via dbus or similar", deType);
    }

    @Test
    public void testSaveCurrentTheme() throws IOException {
        currentTheme = deThemeHelper.getTheme();
    }

    @Test
    public void testSetDarkTheme() throws IOException {
        deThemeHelper.setTheme(true);
        Assert.assertTrue(DeUtils.isDarkDesktop());
        log.info("Dark desktop confirmed");
    }

    @Test
    public void testSetLightTheme() throws IOException {
        deThemeHelper.setTheme(false);
        Assert.assertFalse(DeUtils.isDarkDesktop());
        log.info("Light desktop confirmed");
    }

    @Test(priority = 99)
    public void testRestoreCurrentTheme() throws IOException{
        deThemeHelper.setTheme(currentTheme);
        Assert.assertEquals(currentTheme, deThemeHelper.getTheme());
        log.info("Restored theme: '{}'", currentTheme);
    }
}
