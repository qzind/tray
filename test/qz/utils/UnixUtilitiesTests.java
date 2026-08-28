package qz.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import qz.utils.gtk.GtkUtilities;
import qz.utils.kde.KdeUtilities;

import java.awt.*;
import java.io.IOException;
import java.util.Locale;

import static qz.utils.UnixUtilities.*;
import static qz.utils.UnixUtilities.getDesktopEnvironment;

public class UnixUtilitiesTests {
    static Logger log = LogManager.getLogger(UnixUtilitiesTests.class);

    private static final String GTK_DEFAULT_LIGHT = "Adwaita";
    private static final String GTK_DEFAULT_DARK = "Adwaita-dark";

    private static final String KDE_DEFAULT_LIGHT = "Breeze";
    private static final String KDE_DEFAULT_DARK = "BreezeDark";

    private DesktopEnvironment desktop;
    private String foundTheme;

    @BeforeClass
    public void findDesktopEnvironment() {
        if(GraphicsEnvironment.isHeadless()) {
            throw new SkipException("No DesktopEnvironment in headless");
        }
        desktop = getDesktopEnvironment();
    }

    @Test
    public void testHasDesktopEnvironment() {
        log.info("Desktop detected '{}' via command '{}'", desktop, DesktopEnvironment.binaryFound);
    }

    @BeforeMethod
    public void cacheCurrentTheme() throws IOException {
        foundTheme = getTheme();
    }

    @Test
    public void testSetDarkTheme() throws IOException {
        setTheme(true);
        Assert.assertTrue(isDarkDesktop());
        log.info("Dark desktop confirmed");
    }

    @Test
    public void testSetLightTheme() throws IOException {
        setTheme(false);
        Assert.assertFalse(isDarkDesktop());
        log.info("Light desktop confirmed");
    }

    @Test(priority = 99)
    public void restoreCurrentTheme() throws IOException{
        setTheme(foundTheme);
        Assert.assertEquals(foundTheme, getTheme());
        log.info("Restored theme: '{}'", foundTheme);
    }

    String getTheme() throws IOException {
        return switch(desktop) {
            case GNOME -> GtkUtilities.getTheme();
            case KDE -> KdeUtilities.getTheme();
            case UNKNOWN -> "Unknown";
        };
    }

    void setTheme(boolean isDark) throws IOException {
        switch(desktop) {
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
        switch(desktop) {
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
        boolean success = ShellUtilities.execute(DesktopEnvironment.binaryFound, "set", "org.gnome.desktop.interface", "gtk-theme", themeName);
        if(!success) {
            throw new IOException("Fail to set GTK theme to " + themeName);
        }
    }

    void setKdeTheme(boolean isDark) throws IOException {
        setKdeTheme(isDark ? KDE_DEFAULT_DARK : KDE_DEFAULT_LIGHT);
    }

    void setKdeTheme(String themeName) throws IOException {
String lookAndFeel = String.format("org.kde.%s.desktop", themeName.toLowerCase(Locale.ENGLISH));
        String qdbus = String.format("qdbus%s", DesktopEnvironment.binaryFound.charAt(DesktopEnvironment.binaryFound.length() - 1));

        boolean success =
                ShellUtilities.execute(DesktopEnvironment.binaryFound, "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme", themeName) &&
                        ShellUtilities.execute(DesktopEnvironment.binaryFound, "--file", "kdeglobals", "--group", "KDE", "--key", "LookAndFeelPackage", lookAndFeel) &&
                        ShellUtilities.execute(qdbus, "org.kde.KWin", "/KWin", "reconfigure");

        if(!success) {
            throw new IOException("Fail to set KDE theme to " + themeName);
        }
    }
}
