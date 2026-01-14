package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.printer.PrintOptions;
import qz.utils.PrintingUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Runs two raw pixel print test for each HTML, IMAGE, and PDF; one in portrait, one in landscape.
 * Outputs per-test files under ./out/raw-pixel-print-tests/raw-<format>-<portrait/landscape>.zpl
 * and compares them to a baseline in ./test/baseline/raw-pixel-print-tests
 * todo: update locations before merge
 */
public class RawPixelPrintTests {
    public static final boolean ESTABLISH_BASELINE = false;

    private static final Path OUT_DIR = Paths.get("./out/raw-pixel-tests");
    private static final Path BASE_DIR = Paths.get("./test/qz/printer/action/raw/raw-pixel-baseline");

    @BeforeClass
    public void prepareDirectory() throws IOException {
        Files.createDirectories(OUT_DIR);
        Files.createDirectories(BASE_DIR);
        RawTestHelper.cleanDirectory(OUT_DIR);
        if (ESTABLISH_BASELINE) RawTestHelper.cleanDirectory(BASE_DIR);
    }

    @DataProvider(name = "pixel")
    public Object[][] pixel() throws JSONException {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (PrintingUtilities.Format format : PrintingUtilities.Format.values()) {
            if (!format.hasBiCreator()) continue;
            for ( PrintOptions.Orientation orientation : PrintOptions.Orientation.values()) {
                retMatrix.add(new Object[] {
                        String.format("%s-%s-%s", format.slug(), orientation.slug(), LanguageType.ZPL.slug()),
                        RawTestHelper.constructParams(LanguageType.ZPL, orientation, format)
                });
            }
        }
        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "pixel")
    public void testPixelPrint(String title, JSONObject params) throws Exception {
        RawTestHelper.setupEnvironment();

        Path outFilePath = OUT_DIR.resolve(title + "-test.bin");
        Path baselineFilePath = BASE_DIR.resolve(title + "-test.bin");

        Files.createDirectories(OUT_DIR);

        RawTestHelper.printRaw(outFilePath, params);

        if (ESTABLISH_BASELINE) {
            Files.createDirectories(BASE_DIR);
            Files.copy(outFilePath, baselineFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        RawTestHelper.assertMatches(outFilePath, baselineFilePath);
    }
}
