package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import qz.printer.PrintOptions;
import qz.utils.PrintingUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Runs a raw image conversion (type: raw, format: image) for every supported language.
 * Outputs per-language files under ./out/raw-language-tests/<language>/raw-image-portrait.<language>
 * and compares them to a baseline in ./test/baseline/raw-language-tests
 * todo: update locations before merge
 */
public class RawLanguageTests {
    public static final boolean ESTABLISH_BASELINE = false;

    private static final Path OUT_DIR = Paths.get("./out/raw-language-tests");
    private static final Path BASE_DIR = Paths.get("./test/qz/printer/action/raw/raw-language-baseline");

    @BeforeClass
    public void prepareDirectory() throws IOException {
        Files.createDirectories(OUT_DIR);
        Files.createDirectories(BASE_DIR);
        RawTestHelper.cleanDirectory(OUT_DIR);
        if (ESTABLISH_BASELINE) RawTestHelper.cleanDirectory(BASE_DIR);
    }

    // construct a test matrix of {file
    @DataProvider(name = "languages")
    public Object[][] languages() throws JSONException {
        ArrayList<Object[]> retMatrix = new ArrayList<>();
        for (LanguageType languageType : LanguageType.values()) {
            JSONObject params = RawTestHelper.constructParams(languageType, PrintOptions.Orientation.PORTRAIT, PrintingUtilities.Format.IMAGE);
            retMatrix.add(new Object[]{languageType.slug(), params});
        }
        return retMatrix.toArray(new Object[0][]);
    }

    @Test(dataProvider = "languages")
    public void testLanguagePrint(String title, JSONObject params) throws Exception {
        RawTestHelper.setupEnvironment();

        Path outFilePath = OUT_DIR.resolve(title + "-test.bin");
        Path baselineFilePath = BASE_DIR.resolve(title + "-test.bin");

        try {
            RawTestHelper.printRaw(outFilePath, params);
        } catch(SkipException skip) {
            if (Files.exists(baselineFilePath)) {
                // We are trying to skip a print that had a valid baseline. That needs to cause a test fail.
                throw new Exception(
                        "Print test shouldn't be skipped for a valid baseline: " + baselineFilePath
                );
            } else {
                throw skip;
            }
        }

        if (ESTABLISH_BASELINE) {
            Files.copy(outFilePath, baselineFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        RawTestHelper.assertMatches(outFilePath, baselineFilePath);
    }
}
