package qz.ui.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.ui.ThemeUtilities;
import qz.ui.component.IconCache.Icon;
import qz.ui.component.iconcache.Theme;
import qz.ui.component.iconcache.Type;
import qz.utils.FileUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static qz.ui.component.IconCache.Icon.*;

public class IconCacheTests {
    private static final Logger log = LogManager.getLogger(IconCacheTests.class);

    private IconCache svgCache;
    private IconCache mixedCache;

    @BeforeClass
    public void setUp() {
        svgCache = new IconCache();
        mixedCache = new IconCache(Paths.get("resources_mixed"));
    }

    @DataProvider
    public static Object[][] iconSizeTheme() {
        return Arrays.stream(Icon.values())
                .flatMap(icon -> Arrays.stream(icon.getType().getSizes())
                        .boxed() // Converts int to Integer if getSizes() returns int[]
                        .flatMap(size -> {
                            return Arrays.stream(Theme.values())
                                    .map(theme -> new Object[]{icon, size, theme});
                        }))
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "iconSizeTheme", priority = 1)
    public void testSvgAssetSizes(Icon icon, int size, Theme theme) {
        ImageIcon imageIcon = mixedCache.getIcon(icon, theme, size);
        int expectedSize = accountForPadding(icon, size);
        log.info("Checking SVG IconCache entry for {}: Expected: '{}', Actual: '{}x{}'",
                 icon,
                 expectedSize, imageIcon.getIconWidth(), imageIcon.getIconHeight());

        Assert.assertEquals(imageIcon.getIconHeight(), expectedSize);
    }

    @Test(priority = 2)
    public void testExtractImages() throws IOException {
        Path lightSvg =  svgCache.extractSvg(DEFAULT_ICON, false);
        log.info("{} (light): {}", DEFAULT_ICON, lightSvg);
        Assert.assertTrue(lightSvg.toFile().exists());
        Assert.assertEquals(svgCache.extractedImages.size(), 1);

        // SVG will re-use light icon
        Path darkSvg = svgCache.extractSvg(DEFAULT_ICON, true);
        log.info("{} (dark): {}", DEFAULT_ICON, darkSvg);
        Assert.assertTrue(darkSvg.toFile().exists());
        Assert.assertEquals(svgCache.extractedImages.size(), 2);

        Path lightMaskSvg =  svgCache.extractSvg(DEFAULT_MASK_ICON, false);
        log.info("{} (light): {}", DEFAULT_MASK_ICON, lightMaskSvg);
        Assert.assertTrue(lightMaskSvg.toFile().exists());
        Assert.assertEquals(svgCache.extractedImages.size(), 3);

        Path darkMaskSvg = svgCache.extractSvg(DEFAULT_MASK_ICON, true);
        log.info("{} (dark): {}", DEFAULT_MASK_ICON, darkMaskSvg);
        Assert.assertTrue(darkMaskSvg.toFile().exists());
        Assert.assertEquals(svgCache.extractedImages.size(), 4);

        if(ThemeUtilities.needsInversion(DEFAULT_MASK_ICON)) {
            // look for "#ffffff" on systems that don't support template/symbolic icons
            String darkMaskIconContent = FileUtilities.readLocalFile(darkMaskSvg);
            Assert.assertTrue(darkMaskIconContent.contains("#ffffff"));
        }

        // Our loading icon
        Path loadingMask = svgCache.extractSvg(DANGER_MASK_ICON, false);
        log.info("{} (light): {}", DANGER_MASK_ICON, loadingMask);
        Assert.assertTrue(loadingMask.toFile().exists());
        Assert.assertEquals(svgCache.extractedImages.size(), 5);

        // Ensure no dupes
        svgCache.extractSvg(DEFAULT_ICON, true);
        Assert.assertEquals(svgCache.extractedImages.size(), 5);

        // Again, but for a PNG
        Path darkPng = svgCache.extractPng(DEFAULT_ICON, true, DEFAULT_ICON.getType().getSizes()[0]);
        log.info("{} (dark): {} size: {}", DEFAULT_ICON, darkPng, DEFAULT_ICON.getType().getSizes()[0]);
        Assert.assertTrue(darkPng.toFile().exists());
        Assert.assertEquals(svgCache.extractedImages.size(), 6);
    }

    @Test(dataProvider = "iconSizeTheme", priority = 3)
    public void testMixedAssetSizes(Icon icon, int size, Theme theme) {
        ImageIcon imageIcon = mixedCache.getIcon(icon, theme, size);
        int expectedSize = accountForPadding(icon, size);
        log.info("Checking mixed IconCache entry for {}: Expected: '{}', Actual: '{}x{}'",
                 icon,
                 expectedSize, imageIcon.getIconWidth(), imageIcon.getIconHeight());

        Assert.assertEquals(imageIcon.getIconHeight(), expectedSize);
    }

    @Test(priority = 4)
    public void testImagesDiffer() {
        // Ensure we actually loaded two different images
        BufferedImage svgImage = svgCache.getImage(DEFAULT_ICON);
        BufferedImage pngImage = mixedCache.getImage(DEFAULT_ICON);
        Assert.assertFalse(compareImages(svgImage, pngImage));
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
