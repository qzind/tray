package qz.integration.services;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Jbig2Tests {

	@Test
	public void testJbig2() throws Exception {

		ImageIO.scanForPlugins();

		Path projectRoot = Path.of(System.getProperty("user.dir"));

		Path assetsFolder = projectRoot
			.resolve("test")
			.resolve("qz")
			.resolve("integration")
			.resolve("services")
			.resolve("assets");

		File jb2File = assetsFolder.resolve("jbig2-cameraman.jb2").toFile();
		Assert.assertTrue(jb2File.exists(), "Can't find JB2 at %s!".formatted(jb2File.getAbsolutePath()));

		try (ImageInputStream iis = ImageIO.createImageInputStream(jb2File)) {

			Assert.assertNotNull(iis, "Failed to create ImageInputStream from file!");

			org.apache.pdfbox.jbig2.JBIG2ImageReader reader = new org.apache.pdfbox.jbig2.JBIG2ImageReader(
				new org.apache.pdfbox.jbig2.JBIG2ImageReaderSpi()
			);

			try {

				reader.setInput(iis, false, false);

				BufferedImage decoded = reader.read(0);
				Assert.assertNotNull(decoded, "Decoded image is null!");

				File truthFile = assetsFolder.resolve("jbig2-cameraman.png").toFile();
				Assert.assertTrue(truthFile.exists(), "File does not exist: " + truthFile.getAbsolutePath());

				BufferedImage truth = ImageIO.read(truthFile);
				Assert.assertNotNull(truth);

				Assert.assertEquals(decoded.getWidth(), truth.getWidth());
				Assert.assertEquals(decoded.getHeight(), truth.getHeight());

				for (int y = 0; y < decoded.getHeight(); y++) {
					for (int x = 0; x < decoded.getWidth(); x++) {
						Assert.assertEquals(
							decoded.getRGB(x, y),
							truth.getRGB(x, y),
							"Lossless failure at pixel %d,%d!".formatted(x,y)
						);
					}
				}

			} finally { reader.dispose(); }

		}

	}

}
