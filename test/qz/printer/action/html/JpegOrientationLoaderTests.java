package qz.printer.action.html;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageStorage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static java.awt.Color.*;

public class JpegOrientationLoaderTests {
    private static final String RESOURCE_PATH = "resources/jpeg-orientation/";



    @DataProvider
    public Object[][] images() {
        return new Object[][] {
                {"normal.jpg",          120, 80,    RED,    GREEN,  YELLOW, BLUE},
                {"flip-horizontal.jpg", 120, 80,    GREEN,  RED,    BLUE,   YELLOW},
                {"rotate-180.jpg",      120, 80,    BLUE,   YELLOW, GREEN,  RED},
                {"flip-vertical.jpg",   120, 80,    YELLOW, BLUE,   RED,    GREEN},
                {"transpose.jpg",       80, 120,    RED,    YELLOW, GREEN,  BLUE},
                {"rotate-90.jpg",       80, 120,    YELLOW, RED,    BLUE,   GREEN},
                {"transverse.jpg",      80, 120,    BLUE,   GREEN,  YELLOW, RED},
                {"rotate-270.jpg",      80, 120,    GREEN,  BLUE,   RED,    YELLOW},
                {"none.jpg",            120, 80,    RED,    GREEN,  YELLOW, BLUE},
                {"malformed.jpg",       120, 80,    RED,    GREEN,  YELLOW, BLUE}
        };
    }

    @Test(dataProvider = "images")
    public void testOrientation(String name, int width, int height, Color ... corners) throws IOException {
        byte[] jpeg = readFixture(name);

        ImageLoader loader = JpegOrientationLoaderFactory.getInstance()
                .createImageLoader(new ByteArrayInputStream(jpeg));
        try {
            ImageFrame frame = loader.load(0, 0, 0, true, true, 1, 1);
            Assert.assertNotNull(frame);
            Assert.assertEquals(frame.getWidth(), width);
            Assert.assertEquals(frame.getHeight(), height);
            Assert.assertEquals(frame.getImageType(), ImageStorage.ImageType.RGB);
            Assert.assertEquals(getCornerColors(frame), corners);
        }
        finally {
            loader.dispose();
        }
    }

    private static Color[] getCornerColors(ImageFrame frame) {
        ByteBuffer pixels = ((ByteBuffer) frame.getImageData()).duplicate();
        int inset = 12;
        return new Color[] {
                getColorAt(frame, pixels, inset, inset),
                getColorAt(frame, pixels, frame.getWidth() - 1 - inset, inset),
                getColorAt(frame, pixels, inset, frame.getHeight() - 1 - inset),
                getColorAt(frame, pixels, frame.getWidth() - 1 - inset, frame.getHeight() - 1 - inset)
        };
    }

    private static Color getColorAt(ImageFrame frame, ByteBuffer pixels, int x, int y) {
        int bands = ImageStorage.getInstance().getNumBands(frame.getImageType());
        int offset = pixels.position() + y * frame.getStride() + x * bands;
        int red = Byte.toUnsignedInt(pixels.get(offset));
        int green = Byte.toUnsignedInt(pixels.get(offset + 1));
        int blue = Byte.toUnsignedInt(pixels.get(offset + 2));

        if (red > 180 && green < 120 && blue < 120) return RED;
        if (green > 120 && red < 160 && blue < 160) return GREEN;
        if (blue > 120 && red < 160 && green < 160) return BLUE;
        if (red > 160 && green > 130 && blue < 120) return YELLOW;
        return BLACK;
    }

    private static byte[] readFixture(String name) throws IOException {
        try(InputStream input = Objects.requireNonNull(
                JpegOrientationLoaderTests.class.getResourceAsStream(RESOURCE_PATH + name), name)) {
            return input.readAllBytes();
        }
    }
}
