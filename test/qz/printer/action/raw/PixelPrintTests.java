package qz.printer.action.raw;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ArgValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple raw image printing tests for HTML, IMAGE, and PDF.
 * Generates two jobs per type with different orientations and writes output to ./out
 */
public class PixelPrintTests {
    private static final Logger log = LogManager.getLogger(PixelPrintTests.class);

    private static final Path OUT_DIR = Paths.get("./out/image-print-tests");
    private static final Path RES_DIR = Paths.get("test/qz/printer/action/resources");

    public static void main(String[] args) throws Exception {
        // Allow writing raw data to files
        System.setProperty(ArgValue.SECURITY_PRINT_TOFILE.getMatch(), "true");

        boolean ok = true;
        try {
            runAll(OUT_DIR);
            TestHelper.assertMatchesBaseline(OUT_DIR, Paths.get("./test/baseline/image-print-tests"));
            log.info("Baseline matched for pixel formats");
        } catch (Throwable t) {
            ok = false;
            log.error("Baseline mismatch for pixel formats: {}", t.getMessage());
        }
        System.exit(ok ? 0 : 1);
    }

    public static TestHelper.Result runAll(Path outDir) throws Exception {
        TestHelper.Result r = new TestHelper.Result();

        Files.createDirectories(outDir);
        Files.createDirectories(RES_DIR);

        // Resolve sample assets (expected to exist under resources)
        Path htmlPath = RES_DIR.resolve("raw_sample.html");
        Path imgPath = RES_DIR.resolve("image_sample_bw.png");
        Path pdfPath = RES_DIR.resolve("pdf_sample.pdf");

        TestHelper.requireExists(htmlPath);
        TestHelper.requireExists(imgPath);
        TestHelper.requireExists(pdfPath);

        // 2 orientations x formats; count results, language fixed to ZPL for pixel tests
        TestHelper.runRawImageTest(r, "html", htmlPath, TestHelper.Orientation.PORTRAIT, outDir, LanguageType.ZPL);
        TestHelper.runRawImageTest(r, "html", htmlPath, TestHelper.Orientation.LANDSCAPE, outDir, LanguageType.ZPL);

        TestHelper.runRawImageTest(r, "image", imgPath, TestHelper.Orientation.PORTRAIT, outDir, LanguageType.ZPL);
        TestHelper.runRawImageTest(r, "image", imgPath, TestHelper.Orientation.LANDSCAPE, outDir, LanguageType.ZPL);

        TestHelper.runRawImageTest(r, "pdf", pdfPath, TestHelper.Orientation.PORTRAIT, outDir, LanguageType.ZPL);
        TestHelper.runRawImageTest(r, "pdf", pdfPath, TestHelper.Orientation.LANDSCAPE, outDir, LanguageType.ZPL);

        return r;
    }
}
