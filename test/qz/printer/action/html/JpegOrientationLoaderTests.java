package qz.printer.action.html;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageStorage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public class JpegOrientationLoaderTests {
    private static final String RESOURCE_PATH = "resources/jpeg-orientation/";

    @DataProvider
    public Object[][] images() {
        return new Object[][] {
                {"normal.jpg", 120, 80, new String[] {"red", "green", "yellow", "blue"}},
                {"flip-horizontal.jpg", 120, 80, new String[] {"green", "red", "blue", "yellow"}},
                {"rotate-180.jpg", 120, 80, new String[] {"blue", "yellow", "green", "red"}},
                {"flip-vertical.jpg", 120, 80, new String[] {"yellow", "blue", "red", "green"}},
                {"transpose.jpg", 80, 120, new String[] {"red", "yellow", "green", "blue"}},
                {"rotate-90.jpg", 80, 120, new String[] {"yellow", "red", "blue", "green"}},
                {"transverse.jpg", 80, 120, new String[] {"blue", "green", "yellow", "red"}},
                {"rotate-270.jpg", 80, 120, new String[] {"green", "blue", "red", "yellow"}},
                {"none.jpg", 120, 80, new String[] {"red", "green", "yellow", "blue"}},
                {"malformed.jpg", 120, 80, new String[] {"red", "green", "yellow", "blue"}}
        };
    }

    @Test(dataProvider = "images")
    public void testOrientation(String name, int width, int height, String[] corners) throws IOException {
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

    private static String[] getCornerColors(ImageFrame frame) {
        ByteBuffer pixels = ((ByteBuffer) frame.getImageData()).duplicate();
        int inset = 12;
        return new String[] {
                getColorAt(frame, pixels, inset, inset),
                getColorAt(frame, pixels, frame.getWidth() - 1 - inset, inset),
                getColorAt(frame, pixels, inset, frame.getHeight() - 1 - inset),
                getColorAt(frame, pixels, frame.getWidth() - 1 - inset, frame.getHeight() - 1 - inset)
        };
    }

    private static String getColorAt(ImageFrame frame, ByteBuffer pixels, int x, int y) {
        int bands = ImageStorage.getInstance().getNumBands(frame.getImageType());
        int offset = pixels.position() + y * frame.getStride() + x * bands;
        int red = Byte.toUnsignedInt(pixels.get(offset));
        int green = Byte.toUnsignedInt(pixels.get(offset + 1));
        int blue = Byte.toUnsignedInt(pixels.get(offset + 2));

        if (red > 180 && green < 120 && blue < 120) return "red";
        if (green > 120 && red < 160 && blue < 160) return "green";
        if (blue > 120 && red < 160 && green < 160) return "blue";
        if (red > 160 && green > 130 && blue < 120) return "yellow";
        return "unknown(" + red + "," + green + "," + blue + ")";
    }

    private static byte[] readFixture(String name) throws IOException {
        try(InputStream input = Objects.requireNonNull(
                JpegOrientationLoaderTests.class.getResourceAsStream(RESOURCE_PATH + name), name)) {
            return input.readAllBytes();
        }
    }
}
