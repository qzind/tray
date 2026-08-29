package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import qz.utils.SystemUtilities;

import java.awt.*;
import java.io.IOException;

import static qz.utils.linux.DeType.*;

public class DeUtilsTests {
    static Logger log = LogManager.getLogger(DeUtilsTests.class);

    private DeType deType;
    private DeTheme deTheme;
    private String foundTheme;

    @BeforeClass
    public void findDeType() {
        switch(SystemUtilities.getOs()) {
            case WINDOWS, MAC -> throw new SkipException("DeType detection is assumed on this OS");
            default -> {
                if(GraphicsEnvironment.isHeadless()) {
                    log.warn("Skipping DeType tests in headless mode");
                    throw new SkipException("Skipping DeType tests in headless mode");
                }
            }
        }

        deType = DeUtils.getDeType();
        deTheme = DeUtils.getDeTheme();
    }

    @Test
    public void testHasDe() {
        Assert.assertNotEquals(deType, UNKNOWN);
        log.info("Desktop detected '{}' via command '{}'", deType, getBinaryFound());
    }

    @BeforeMethod
    public void cacheCurrentTheme() throws IOException {
        foundTheme = deTheme.getTheme();
    }

    @Test
    public void testSetDarkTheme() throws IOException {
        deTheme.setTheme(true);
        Assert.assertTrue(deTheme.isDarkDesktop());
        log.info("Dark desktop confirmed");
    }

    @Test
    public void testSetLightTheme() throws IOException {
        deTheme.setTheme(false);
        Assert.assertFalse(deTheme.isDarkDesktop());
        log.info("Light desktop confirmed");
    }

    @Test(priority = 99)
    public void restoreCurrentTheme() throws IOException{
        deTheme.setTheme(foundTheme);
        Assert.assertEquals(foundTheme, deTheme.getTheme());
        log.info("Restored theme: '{}'", foundTheme);
    }
}
