package qz.ui.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.ui.component.IconCache.Icon;
import qz.ui.component.iconcache.Cache;
import qz.ui.component.iconcache.Format;
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

    static int extractedSvgCache = 0;

    @BeforeClass
    public void setUp() {
        svgCache = new IconCache();
        mixedCache = new IconCache(Paths.get("resources_mixed"));
    }

    @DataProvider
    public Object[][] iconSizeTheme() {
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

    @DataProvider
    public Object[][] iconTheme() {
        return Arrays.stream(Icon.values())
                .flatMap(icon -> Arrays.stream(Theme.values())
                        .map(theme -> new Object[]{icon, theme}))
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "iconTheme", priority = 2)
    public void testExtractSvg(Icon icon, Theme theme) throws IOException {
        Cache cache = svgCache.getCache(icon, theme);
        log.info("{} ({}): {}", icon, theme, cache.getBaseLocation());

        Path svg = svgCache.extract(icon, theme);
        Assert.assertTrue(svg.toFile().exists());

        if(icon.isMaskIcon()) {
            // Look for "#000000" | "#ffffff"
            String svgContent = FileUtilities.readLocalFile(svg);
            Assert.assertTrue(svgContent.contains(theme.isDark()? "#ffffff":"#000000"), "Cannot find fill color in SVG: " + svgContent);
        }
        log.info("Extracted SVG {}", svg);
    }

    @Test(priority = 3)
    public void testExtractCache() throws IOException {
        // Ensure path doesn't change
        Path path1 = svgCache.extract(TRAY_READY_COLOR, Theme.DARK);
        Path path2 = svgCache.extract(TRAY_READY_COLOR, Theme.DARK);
        Assert.assertEquals(path1, path2);
    }

    @DataProvider
    public Object[][] nameClashProvider() {
        return new Object[][]{
                { TRAY_READY_COLOR, Theme.DARK, },
                { TRAY_LOADING_COLOR, Theme.LIGHT }
        };
    }

    /**
     * Ensure that extracting a PNG still increments the extracted count
     */
    @Test(dataProvider = "nameClashProvider", priority = 4)
    public void testNameClash(Icon icon, Theme theme) throws IOException {
        Cache cache = svgCache.getCache(icon, theme);
        Path svg = cache.extract(Format.SVG);
        log.info("{} ({}): {}", icon, theme, svg);
        Assert.assertTrue(svg.toFile().exists());

        Path png = cache.extract(Format.PNG); // PNG from the svgCache is deliberate.
        log.info("{} ({}): {}", icon, theme, png);
        Assert.assertTrue(png.toFile().exists());

        Assert.assertNotEquals(svg, png);
    }

    @Test(dataProvider = "iconSizeTheme", priority = 5)
    public void testMixedAssetSizes(Icon icon, int size, Theme theme) {
        ImageIcon imageIcon = mixedCache.getIcon(icon, theme, size);
        int expectedSize = accountForPadding(icon, size);
        log.info("Checking mixed IconCache entry for {}: Expected: '{}', Actual: '{}x{}'",
                 icon,
                 expectedSize, imageIcon.getIconWidth(), imageIcon.getIconHeight());

        Assert.assertEquals(imageIcon.getIconHeight(), expectedSize);
    }

    @Test(dataProvider = "iconTheme", priority = 6)
    public void testImagesDiffer(Icon icon, Theme theme) {
        Cache svg = svgCache.getCache(icon, theme);
        Cache png = mixedCache.getCache(icon, theme);

        // Compare svg to png
        if(svg.getFormat() == Format.SVG && png.getFormat() == Format.PNG) {
            BufferedImage svgImage = svgCache.getImage(icon);
            BufferedImage pngImage = mixedCache.getImage(icon);

            // Ensure we actually loaded two different images
            Assert.assertFalse(compareImages(svgImage, pngImage));
        } else {
            throw new SkipException("Resources are identical formats, skipping");
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
