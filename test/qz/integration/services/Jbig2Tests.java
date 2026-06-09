package qz.integration.services;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.InputStream;

public class Jbig2Tests {

	@Test
	public void testJbig2() throws Exception {

		ImageIO.scanForPlugins();

		try (InputStream jb2Stream = Jbig2Tests.class.getResourceAsStream("assets/jbig2-cameraman.jb2");
			 InputStream truthStream = Jbig2Tests.class.getResourceAsStream("assets/jbig2-cameraman.png")) {

			Assert.assertNotNull(jb2Stream, "jb2Stream is null - can't find or read assets/jbig2-cameraman.jb2!");
			Assert.assertNotNull(truthStream, "truthStream is null - can't find or read assets/jbig2-cameraman.png!");

			try (javax.imageio.stream.ImageInputStream iis = ImageIO.createImageInputStream(jb2Stream)) {
				org.apache.pdfbox.jbig2.JBIG2ImageReader reader = new org.apache.pdfbox.jbig2.JBIG2ImageReader(
					new org.apache.pdfbox.jbig2.JBIG2ImageReaderSpi()
				);

				try {
					reader.setInput(iis, false, false);
					BufferedImage decoded = reader.read(0);
					Assert.assertNotNull(decoded, "Decoded image is null!");

					BufferedImage truth = ImageIO.read(truthStream);
					Assert.assertNotNull(truth, "Truth image failed to load!");

					for (int y = 0; y < decoded.getHeight(); y++) {
						for (int x = 0; x < decoded.getWidth(); x++) {
							Assert.assertEquals(
								decoded.getRGB(x, y), truth.getRGB(x, y),
								String.format("Mismatch at %d,%d", x, y)
							);
						}
					}
				}
				finally { reader.dispose(); }

			}

		}

	}

}
