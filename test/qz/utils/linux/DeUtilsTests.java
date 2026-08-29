package qz.utils.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;
import qz.utils.linux.gtk.GtkTheme;
import qz.utils.linux.kde.KdeTheme;

import java.awt.*;
import java.io.IOException;

import static qz.utils.linux.DeType.*;

public class DeUtilsTests {
    static Logger log = LogManager.getLogger(DeUtilsTests.class);

    private static final String GTK_DEFAULT_LIGHT = "Adwaita";
    private static final String GTK_DEFAULT_DARK = "Adwaita-dark";

    private static final String KDE_DEFAULT_LIGHT = "BreezeLight";
    private static final String KDE_DEFAULT_DARK = "BreezeDark";

    private DeType deType;
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
    }

    @Test
    public void testHasDe() {
        Assert.assertNotEquals(deType, UNKNOWN);
        log.info("Desktop detected '{}' via command '{}'", deType, getBinaryFound());
    }

    @BeforeMethod
    public void cacheCurrentTheme() throws IOException {
        foundTheme = getTheme();
    }

    @Test
    public void testSetDarkTheme() throws IOException {
        setTheme(true);
        Assert.assertTrue(DeUtils.isDarkDesktop());
        log.info("Dark desktop confirmed");
    }

    @Test
    public void testSetLightTheme() throws IOException {
        setTheme(false);
        Assert.assertFalse(DeUtils.isDarkDesktop());
        log.info("Light desktop confirmed");
    }

    @Test(priority = 99)
    public void restoreCurrentTheme() throws IOException{
        setTheme(foundTheme);
        Assert.assertEquals(foundTheme, getTheme());
        log.info("Restored theme: '{}'", foundTheme);
    }

    String getTheme() throws IOException {
        return switch(deType) {
            case GNOME -> GtkTheme.getTheme();
            case KDE -> KdeTheme.getTheme();
            case UNKNOWN -> "Unknown";
        };
    }

    void setTheme(boolean isDark) throws IOException {
        switch(deType) {
            case GNOME:
                setGtkTheme(isDark);
                return;
            case KDE:
                setKdeTheme(isDark);
                return;
            default:
                throw new IOException("Unknown desktop environment");
        }
    }

    void setTheme(String themeName) throws IOException {
        switch(deType) {
            case GNOME:
                setGtkTheme(themeName);
                return;
            case KDE:
                setKdeTheme(themeName);
                return;
            default:
                throw new IOException("Unknown desktop environment");
        }
    }

    void setGtkTheme(boolean isDark) throws IOException{
        setGtkTheme(isDark ? GTK_DEFAULT_DARK : GTK_DEFAULT_LIGHT);
    }

    void setGtkTheme(String themeName) throws IOException {
        boolean success = ShellUtilities.execute(getBinaryFound(), "set", "org.gnome.desktop.interface", "gtk-theme", themeName);
        if(!success) {
            throw new IOException("Fail to set GTK theme to " + themeName);
        }
    }

    void setKdeTheme(boolean isDark) throws IOException {
        setKdeTheme(isDark ? KDE_DEFAULT_DARK : KDE_DEFAULT_LIGHT);
    }

    void setKdeTheme(String themeName) throws IOException {
        boolean success = ShellUtilities.execute("plasma-apply-colorscheme", themeName);
        if(!success) {
            throw new IOException("Fail to set KDE theme to " + themeName);
        }
    }
}
