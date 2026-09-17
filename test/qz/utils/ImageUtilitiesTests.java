package qz.utils;

import org.testng.Assert;
import org.testng.annotations.Test;
import qz.ui.component.IconCache;

import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.Base64;

import static qz.ui.component.IconCache.Icon.LOGO_ICON;

public class ImageUtilitiesTests {
    @Test
    public void testImageFromSvgResourceScalesToRequestedHeight() {
        BufferedImage image = ImageUtilities.imageFromSvgResource(
                Paths.get("../resources/logo.svg"),
                260,
                new IconCache()
        );

        Assert.assertNotNull(image, "SVG image should load");
        Assert.assertEquals(image.getWidth(), 260, "SVG image width");
        Assert.assertEquals(image.getHeight(), 260, "SVG image height");
    }

    @Test
    public void testImageToBase64PngOutput() {
        String base64 = ImageUtilities.imageToBase64(LOGO_ICON, "png");
        byte[] png = Base64.getDecoder().decode(base64);

        Assert.assertFalse(base64.isEmpty(), "Base64 output should not be empty");
        Assert.assertEquals(png[0], (byte)0x89, "PNG signature byte 0");
        Assert.assertEquals(png[1], (byte)0x50, "PNG signature byte 1");
        Assert.assertEquals(png[2], (byte)0x4e, "PNG signature byte 2");
        Assert.assertEquals(png[3], (byte)0x47, "PNG signature byte 3");
    }
}
