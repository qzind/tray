package qz.printer.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import qz.printer.PrintOptions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manual smoke check for issue #435.
 */
public class ReversePortraitTest {

    private static final Logger log = LogManager.getLogger(ReversePortraitTest.class);

    private static final Path RES_DIR = Paths.get("./test/qz/printer/action/resources");
    private static final Path OUT_DIR = Paths.get("./out/issue-435-smoke");
    private static final String FORMAT = "png";

    public static void main(String[] args) {
        try {
            Files.createDirectories(OUT_DIR);

            checkOptions();
            test("image", readImage("image-sample.png"));
            test("pdf", readPdf("pdf-sample.pdf"));

            log.info("Reverse portrait smoke check passed");
            log.info("Review output files in {}", OUT_DIR.toAbsolutePath());
        }
        catch(Throwable t) {
            log.error("Reverse portrait smoke check failed", t);
            System.exit(1);
        }
    }

    private static void checkOptions() {
        double image = PrintImage.getImageRotation(0, PrintOptions.Orientation.REVERSE_PORTRAIT, false);
        double pdf = PrintPDF.getPageRotation(0, PrintOptions.Orientation.REVERSE_PORTRAIT);

        // Issue #435 fallback
        // reverse portrait should become a 180-degree
        // content turn
        if (image != 180) {
            throw new IllegalStateException("Expected reverse-portrait image rotation to be 180, got " + image);
        }
        if (pdf != 180) {
            throw new IllegalStateException("Expected reverse-portrait PDF rotation to be 180, got " + pdf);
        }

        // PDF reverse portrait should keep
        // portrait paper geometry, not landscape sizing
        if (PrintPDF.isLandscapeOrientation(PrintOptions.Orientation.REVERSE_PORTRAIT)) {
            throw new IllegalStateException("Reverse portrait should keep portrait page geometry");
        }
    }

    private static void test(String id, BufferedImage source) throws IOException {
        // images are saved
        // so that smoke output can be inspected manually
        BufferedImage reverse = rotate(source);
        BufferedImage roundtrip = rotate(reverse);

        check(id, source, reverse, roundtrip);

        save(id + "-source", source);
        save(id + "-reverse-portrait", reverse);
        save(id + "-roundtrip", roundtrip);
    }

    private static BufferedImage rotate(BufferedImage image) {
        return PrintImage.rotate(
                image,
                PrintImage.getImageRotation(0, PrintOptions.Orientation.REVERSE_PORTRAIT, false),
                RenderingHints.VALUE_DITHER_DEFAULT,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    }

    private static void check(String id, BufferedImage source, BufferedImage reverse, BufferedImage roundtrip) {
        sameSize(id + " reverse", source, reverse);
        sameSize(id + " roundtrip", source, roundtrip);

        int reverseDiff = 0;
        int roundtripDiff = 0;
        for(int y = 0; y < source.getHeight(); y++) {
            for(int x = 0; x < source.getWidth(); x++) {
                if (source.getRGB(x, y) != reverse.getRGB(x, y)) {
                    reverseDiff++;
                }
                if (source.getRGB(x, y) != roundtrip.getRGB(x, y)) {
                    roundtripDiff++;
                }
            }
        }

        // 1st turn proves visible reversal
        // 2nd turn proves the 180 degree transform is stable
        if (reverseDiff == 0) {
            throw new IllegalStateException(id + " reverse output matched source unexpectedly");
        }
        if (roundtripDiff != 0) {
            throw new IllegalStateException(id + " roundtrip differed by " + roundtripDiff + " pixels");
        }
    }

    private static void sameSize(String id, BufferedImage expected, BufferedImage actual) {
        if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight()) {
            throw new IllegalStateException(String.format(
                    "%s dimensions changed from %sx%s to %sx%s",
                    id, expected.getWidth(), expected.getHeight(), actual.getWidth(), actual.getHeight()));
        }
    }

    private static BufferedImage readImage(String name) throws IOException {
        return ImageIO.read(RES_DIR.resolve(name).toFile());
    }

    private static BufferedImage readPdf(String name) throws IOException {
        try(PDDocument doc = PDDocument.load(RES_DIR.resolve(name).toFile())) {
            return new PDFRenderer(doc).renderImage(0);
        }
    }

    private static void save(String id, BufferedImage image) throws IOException {
        Path output = OUT_DIR.resolve(String.format("issue-435-%s.%s", id, FORMAT));
        ImageIO.write(image, FORMAT, output.toFile());
        log.info("Wrote {}: {}", id, output.toAbsolutePath());
    }
}