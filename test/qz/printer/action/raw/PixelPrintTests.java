package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Runs two raw pixel print test for each HTML, IMAGE, and PDF; one in portrait, one in landscape.
 * Outputs per-test files under ./out/raw-pixel-print-tests/raw-<format>-<portrait/landscape>.zpl
 * and compares them to a baseline in ./test/baseline/raw-pixel-print-tests
 * todo: update locations before merge
 */
public class PixelPrintTests {
    public static final boolean ESTABLISH_BASELINE = false;

    private static final Path OUT_DIR = Paths.get("./out/raw-pixel-tests");
    private static final Path BASE_DIR = Paths.get("./test/qz/printer/action/raw/raw-pixel-baseline");

    public static void main(String[] args) {
        //todo: should we add a non-testng runner?
    }

    @BeforeClass
    public void prepareDirectory() throws IOException {
        Files.createDirectories(OUT_DIR);
        Files.createDirectories(BASE_DIR);
        TestHelper.cleanDirectory(OUT_DIR);
        if (ESTABLISH_BASELINE) TestHelper.cleanDirectory(BASE_DIR);
    }

    @DataProvider(name = "pixel")
    public Object[][] pixel() throws JSONException {
        return new Object[][]{
                {"image-portrait", TestHelper.constructParams(LanguageType.ZPL, TestHelper.Orientation.PORTRAIT, TestHelper.Format.IMAGE)},
                {"image-landscape", TestHelper.constructParams(LanguageType.ZPL, TestHelper.Orientation.LANDSCAPE, TestHelper.Format.IMAGE)},
                {"pdf-portrait", TestHelper.constructParams(LanguageType.ZPL, TestHelper.Orientation.PORTRAIT, TestHelper.Format.PDF)},
                {"pdf-landscape", TestHelper.constructParams(LanguageType.ZPL, TestHelper.Orientation.LANDSCAPE, TestHelper.Format.PDF)},
                {"html-portrait", TestHelper.constructParams(LanguageType.ZPL, TestHelper.Orientation.PORTRAIT, TestHelper.Format.HTML)},
                {"html-landscape", TestHelper.constructParams(LanguageType.ZPL, TestHelper.Orientation.LANDSCAPE, TestHelper.Format.HTML)},
        };
    }

    @Test(dataProvider = "pixel")
    public void testPixelPrint(String title, JSONObject params) throws Exception {
        TestHelper.setupEnvironment();

        Path outFilePath = OUT_DIR.resolve(title + "-test.bin");
        Path baselineFilePath = BASE_DIR.resolve(title + "-test.bin");

        Files.createDirectories(OUT_DIR);

        TestHelper.printRaw(outFilePath, params);

        if (ESTABLISH_BASELINE) {
            Files.createDirectories(BASE_DIR);
            Files.copy(outFilePath, baselineFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        TestHelper.assertMatches(outFilePath, baselineFilePath);
    }
}
