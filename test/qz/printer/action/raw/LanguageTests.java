package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Runs a raw image conversion (type: raw, format: image) for every supported language.
 * Outputs per-language files under ./out/raw-language-tests/<language>/raw-image-portrait.<language>
 * and compares them to a baseline in ./test/baseline/raw-language-tests
 * todo: update locations before merge
 */
public class LanguageTests {
    public static final boolean ESTABLISH_BASELINE = false;

    private static final Path OUT_DIR = Paths.get("./out/raw-language-tests");
    private static final Path BASE_DIR = Paths.get("./test/qz/printer/action/raw/raw-language-baseline");

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

    @DataProvider(name = "languages")
    public Object[][] languages() {
        try {
            Object[][] prefMatrix = new Object[(LanguageType.values().length - 1)][2];
            for (int i = 0; i < LanguageType.values().length; i++) {
                LanguageType languageType = LanguageType.values()[i];
                if (languageType == LanguageType.UNKNOWN) continue;
                JSONObject params = TestHelper.constructParams(languageType, TestHelper.Orientation.PORTRAIT, TestHelper.Format.IMAGE);

                prefMatrix[i] = new Object[]{languageType, params};
            }
            return prefMatrix;
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(dataProvider = "languages")
    public void testLanguagePrint(LanguageType languageType, JSONObject params) throws Exception {
        TestHelper.setupEnvironment();

        Path outFilePath = OUT_DIR.resolve(languageType.name().toLowerCase() + "-test.bin");
        Path baselineFilePath = BASE_DIR.resolve(languageType.name().toLowerCase() + "-test.bin");

        try {
            TestHelper.printRaw(outFilePath, params);
        } catch(SkipException skip) {
            if (Files.exists(baselineFilePath)) {
                // We are trying to skip a print that had a valid baseline. That needs to cause a test fail.
                throw new Exception("Print skipped for a valid baseline:" + languageType);
            } else {
                throw skip;
            }
        }

        if (ESTABLISH_BASELINE) {
            Files.copy(outFilePath, baselineFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        TestHelper.assertMatches(outFilePath, baselineFilePath);
    }
}
