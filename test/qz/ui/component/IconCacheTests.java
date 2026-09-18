package qz.ui.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;
import qz.ui.component.IconCache.Icon;
import qz.utils.SystemUtilities;


import javax.swing.*;

import java.awt.image.BufferedImage;
import java.nio.file.Paths;

import static qz.ui.component.IconCache.Icon.*;

public class IconCacheTests {
    private static final Logger log = LogManager.getLogger(IconCacheTests.class);

    private IconCache svgCache;
    private IconCache mixedCache;

    @Test(priority = -1)
    public void testSvgAssets() {
        checkIcons(svgCache = new IconCache());
    }

    @Test
    public void testMixedAssets() {
        checkIcons(mixedCache = new IconCache(Paths.get("./resources_mixed")));
    }

    @Test(priority = 99)
    public void testImagesDiffer() {
        // Ensure we actually loaded two different images
        BufferedImage svgImage = svgCache.getImage(DEFAULT_ICON);
        BufferedImage pngImage = mixedCache.getImage(DEFAULT_ICON);
        Assert.assertFalse(compareImages(svgImage, pngImage));
    }

    private static void checkIcons(IconCache iconCache) {
        for(Icon icon : Icon.values()) {
            for(int size : icon.getSizes()) {
                int expectedSize = accountForPadding(icon, size);

                for(Theme theme : Theme.values()) {
                    ImageIcon imageIcon = iconCache.getIcon(icon, theme, size);
                    log.info("Checking IconCache entry for {} '{}': Expected: '{}', Actual: '{}x{}'",
                             icon, icon.getId(theme, size), expectedSize,
                             imageIcon.getIconWidth(), imageIcon.getIconHeight());

                    Assert.assertEquals(imageIcon.getIconHeight(), expectedSize);
                }
            }
        }
    }

    /**
     * macOS tray icons require 25% padding
     */
    private static int accountForPadding(Icon icon, int size) {
        if(icon.getType() == Type.SYSTEM_TRAY && SystemUtilities.isMac()) {
            return size + (size / 4); // handle 25% padding
        }
        return size;
    }

    public static boolean compareImages(BufferedImage image1, BufferedImage image2) {
        if(image1 ==  null || image2 ==  null) {
            return image1 == image2;
        }

        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            return false;
        }

        // compare every pixel
        for (int x = 0; x < image1.getWidth(); x++) {
            for (int y = 0; y < image1.getHeight(); y++) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }
}
