package qz.integration.services;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.io.InputStream;
import java.util.Iterator;

import java.util.Arrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImageReaderSpiTests {

	private static final Logger log = LogManager.getLogger(ImageReaderSpiTests.class);

	@DataProvider(name = "nameAndImage")
	public Object[][] nameAndImage() {
		return new Object[][] {
				{
					// JBIG2 - Provided by Apache PDFBox
					"JBIG2",
					"assets/jbig2-cameraman.jb2"
				}
		};
	}

	@DataProvider(name = "names")
	public Object[][] name() {
		return new Object[][] {
			{ "JBIG2" },
			{ "JPEG 2000" }, { "JPEG2000" }
		};
	}

	@BeforeMethod
    public void setUp() {
		ImageIO.scanForPlugins();
	}

	@Test
	public void testUnknownFormat() {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("NonExistentFormat");
		Assert.assertFalse(readers.hasNext());
	}

	@Test(dataProvider="names")
	public void testFormatPresent(String name) {
		Iterator<ImageReader> readers;

		readers = ImageIO.getImageReadersByFormatName(name);
		Assert.assertTrue(readers.hasNext());

		readers = ImageIO.getImageReadersByFormatName(name.toLowerCase());
		Assert.assertTrue(readers.hasNext());
	}

	@Test(dataProvider = "nameAndImage")
	public void testFormat(String formatName, String resource) throws Exception {
		try (InputStream is = getClass().getResourceAsStream(resource)) {
			Assert.assertNotNull(is, "InputStream is null");

			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatName);
			if (!readers.hasNext()) {
				throw new IllegalStateException(String.format("%s ImageReader plugin is not registered on the classpath!", formatName));
			}
			ImageReader reader = readers.next();

			try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
				reader.setInput(iis);
				BufferedImage bi = reader.read(0);
				for (int y = 0; y < bi.getHeight(); y++) {
					for (int x = 0; x < bi.getWidth(); x++) {
						if(bi.getRGB(x, y) != Color.WHITE.getRGB()) {
							Assert.assertTrue(true, "Found a non-white pixel, decoding is assumed working");
							return;
						}
					}
				}
			} finally {
				reader.dispose();
			}
			Assert.fail("Couldn't find a non-white pixel, decoding is assumed failed");
		}
	}

}
