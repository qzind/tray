package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qz.utils.SystemUtilities;
import qz.utils.linux.compositor.Compositor;
import qz.utils.linux.theme.ThemeManager;

import java.awt.*;
import java.io.IOException;

public class ThemeUtilitiesTests {
    static Logger log = LogManager.getLogger(ThemeUtilitiesTests.class);

    private Compositor compositor;
    private ThemeManager themeManager;
    private String currentTheme;

    @BeforeClass
    public void beforeFindDe() {
        switch(SystemUtilities.getOs()) {
            case WINDOWS, MAC -> throw new SkipException("Desktop environment is assumed on this OS");
            default -> {
                if(GraphicsEnvironment.isHeadless()) {
                    log.warn("Skipping desktop environment tests in headless mode");
                    throw new SkipException("Skipping desktop environment tests in headless mode");
                }
            }
        }

        compositor = Compositor.detectCompositor();
        themeManager = ThemeManager.getThemeManager();
    }

    @Test(priority = -2)
    public void testHasCompositor() {
        Assert.assertNotNull(compositor);
        log.info("Will try techniques from the '{}' compositor for obtaining scale factor", compositor);
    }

    @Test
    public void testSaveCurrentTheme() throws IOException {
        currentTheme = themeManager.getTheme();
    }

    @Test
    public void testSetDarkTheme() throws IOException {
        themeManager.setTheme(true);
        Assert.assertTrue(LinuxUtilities.isDarkDesktop());
        log.info("Dark desktop confirmed");
    }

    @Test
    public void testSetLightTheme() throws IOException {
        themeManager.setTheme(false);
        Assert.assertFalse(LinuxUtilities.isDarkDesktop());
        log.info("Light desktop confirmed");
    }

    @Test(priority = 99)
    public void testRestoreCurrentTheme() throws IOException{
        themeManager.setTheme(currentTheme);
        Assert.assertEquals(currentTheme, themeManager.getTheme());
        log.info("Restored theme: '{}'", currentTheme);
    }
}
