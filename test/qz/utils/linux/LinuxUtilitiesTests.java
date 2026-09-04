package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.linux.compositor.dispatcher.*;
import qz.utils.linux.compositor.Compositor;

import java.awt.*;

public class LinuxUtilitiesTests {
    static Logger log = LogManager.getLogger(LinuxUtilitiesTests.class);

    private Compositor compositor;
    private ThemeStrings themeStrings;
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

        compositor = Compositor.detectCompositor();
        themeStrings = switch(compositor) {
            case KWIN -> new KwinStrings();
            case MUTTER -> new MutterStrings();
            case XFCE -> new XfceStrings();
            default -> new UnknownStrings();
        };
    }

    @Test(priority = -2)
    public void testHasCompositor() {
        Assert.assertNotNull(compositor);
        log.info("Will try techniques from the '{}' compositor for obtaining scale factor", compositor);
    }

    @Test
    public void testSaveCurrentTheme() {
        currentTheme = getTheme();
    }

    @Test
    public void testSetDarkTheme() {
        setTheme(true);
        Assert.assertTrue(LinuxUtilities.isDarkDesktop());
        log.info("Dark desktop confirmed");
    }

    @Test
    public void testSetLightTheme() {
        setTheme(false);
        Assert.assertFalse(LinuxUtilities.isDarkDesktop());
        log.info("Light desktop confirmed");
    }

    @Test(priority = 99)
    public void testRestoreCurrentTheme() {
        setTheme(currentTheme);
        Assert.assertEquals(currentTheme, getTheme());
        log.info("Restored theme: '{}'", currentTheme);
    }

    @Test
    public void testGetScaleFactor() {
        Double scaleFactor = LinuxUtilities.getScaleFactor();
        log.info("Detected scale factor {}x from {}", scaleFactor, Compositor.detectCompositor());
        Assert.assertNotNull(scaleFactor);
        Assert.assertNotEquals(scaleFactor, 0.0);
    }

    void setTheme(String themeName) {
        boolean success = false;
        for(String[] s : themeStrings.themeSetters(themeName)) {
            if(ShellUtilities.execute(s)) {
                success = true;
            }
        }
        if(!success) {
            log.warn("Failed to set {} theme", themeName);
        }
    }

    void setTheme(boolean isDark) {
        boolean success = false;
        for(String[] s : themeStrings.themeSetters(isDark)) {
            if(ShellUtilities.execute(s)) {
                success = true;
            }
        }
        if(!success) {
            log.warn("Failed to set {} theme", isDark? "dark":"light");
        }
    }

    String getTheme() {
        for(String[] s : themeStrings.themeGetters()) {
            String themeName = ShellUtilities.executeRaw(s);
            if(!themeName.isBlank()) {
                return themeName.replace("\"", "").replace("'", "").trim();
            }
        }
        log.warn("Failed to get theme from {}", themeStrings.getClass().getName());
        return null;
    }

}
