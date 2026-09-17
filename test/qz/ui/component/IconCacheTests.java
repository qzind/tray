package qz.ui.component;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static qz.ui.component.IconCache.Icon.*;

public class IconCacheTests {
    private IconCache iconCache;

    @BeforeMethod
    public void setUp() {
        iconCache = new IconCache();
    }

    @Test
    public void testCacheBuilds() {
        Assert.assertNotNull(iconCache);
    }

    @Test
    public void testRepresentativeResourcesLoad() {
        Assert.assertNotNull(iconCache.getImage(LOGO_ICON), "Logo icon should load");
        Assert.assertNotNull(iconCache.getImage(DEFAULT_ICON), "Tray icon should load");
        Assert.assertNotNull(iconCache.getImage(DEFAULT_MASK_ICON), "Tray mask icon should load");
        Assert.assertNotNull(iconCache.getImage(ABOUT_ICON), "Menu icon should load");
        Assert.assertNotNull(iconCache.getImage(TRUST_VERIFIED_ICON), "Dialog icon should load");
    }

    @Test
    public void testRepresentativeImageSizes() {
        assertSize(iconCache.getImage(LOGO_ICON), 260, 260, "Logo icon");
        assertSize(iconCache.getImage(ABOUT_ICON), 16, 16, "Menu icon");
        assertSize(iconCache.getImage(TRUST_VERIFIED_ICON), 45, 45, "Dialog icon");
        assertSize(iconCache.getImage(DEFAULT_ICON, new Dimension(20, 20), false), 20, 20, "Tray icon");

        List<BufferedImage> taskbarImages = iconCache.getImages(TASK_BAR_ICON);
        Assert.assertEquals(taskbarImages.size(), 5, "Taskbar icon size count");
        assertSize(taskbarImages.get(0), 20, 20, "Taskbar icon 20");
        assertSize(taskbarImages.get(1), 24, 24, "Taskbar icon 24");
        assertSize(taskbarImages.get(2), 32, 32, "Taskbar icon 32");
        assertSize(taskbarImages.get(3), 40, 40, "Taskbar icon 40");
        assertSize(taskbarImages.get(4), 48, 48, "Taskbar icon 48");
    }

    private static void assertSize(BufferedImage image, int width, int height, String message) {
        Assert.assertNotNull(image, message + " should load");
        Assert.assertEquals(image.getWidth(), width, message + " width");
        Assert.assertEquals(image.getHeight(), height, message + " height");
    }
}
