package qz.integration.services;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Iterator;

public class ImageReaderSpiTests {

	@DataProvider(name = "formats")
	public Object[][] formats() {
		return new Object[][] {
			// JBIG2 - Provided by Apache PDFBox
			{
				"JBIG2",
				"assets/jbig2-cameraman.jb2",
			},
			// JPEG 2000 - Provided by jai-imageio
			{
				"JPEG 2000",
				"assets/jpeg2000-cameraman.jp2",
			},
			{
				"JPEG2000",
				"assets/jpeg2000-cameraman.jp2",
			},
			// Buggy JPEG - Provided by TwelveMonkeys
			{
				"JPEG",
				"assets/jfif-cmyk-invalid-icc-profile-srgb.jpg",
			},
		};
	}

	@BeforeMethod
	public void beforeMethod() {
		ImageIO.scanForPlugins();
	}

	@Test
	public void testUnknownFormat() {
		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("NonExistentFormat");
		Assert.assertFalse(readers.hasNext());
	}

	@Test(dataProvider="formats")
	public void testFormatPresent(String formatName, String ignore) {
		Iterator<ImageReader> readers;

		readers = ImageIO.getImageReadersByFormatName(formatName);
		Assert.assertTrue(readers.hasNext());

		readers = ImageIO.getImageReadersByFormatName(formatName.toLowerCase());
		Assert.assertTrue(readers.hasNext());
	}

	@Test(dataProvider = "formats")
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
				boolean validSize = bi.getWidth() > 0 & bi.getHeight() > 0;
				if (validSize) {
					Assert.assertTrue(true, "Image has a valid dimension");
					return;
				}
			} finally {
				reader.dispose();
			}
			Assert.fail("Image doesn't have a valid dimension, decoding is assumed failed");
		}
	}

}
