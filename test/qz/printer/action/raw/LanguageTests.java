package qz.printer.action.raw;

import java.nio.file.Paths;
import java.nio.file.Path;

/**
 * Runs a raw image conversion (type: raw, format: image) for every supported language.
 * Outputs per-language files under ./out/raw-language-tests/<language>/raw-image-portrait.<language>
 * and compares them to a baseline in ./test/baseline/raw-language-tests
 */
public class LanguageTests {
    private static final Path OUT_DIR = Paths.get("./out/raw-language-tests");
    private static final Path RES_DIR = Paths.get("test/qz/printer/action/resources");
    public static final Path BASE_DIR = Paths.get("./test/baseline/raw-language-tests");

    public static void main(String[] args) throws Exception {
        TestHelper.setupEnviroment();

        TestHelper.Result r = runAll(OUT_DIR);
        TestHelper.assertMatchesBaseline(r, OUT_DIR, BASE_DIR);

        r.logSummary();
        System.exit(r.passed() ? 0 : 1);
    }

    public static TestHelper.Result runAll(Path outRoot) throws Exception {
        TestHelper.Result r = new TestHelper.Result();

        TestHelper.cleanDirectory(outRoot);
        Path imgPath = RES_DIR.resolve("image_sample_bw.png");
        TestHelper.requireExists(imgPath);

        for (LanguageType lang : LanguageType.values()) {
            if (lang == LanguageType.UNKNOWN) { continue; }
            Path outDir = outRoot.resolve(lang.name().toLowerCase());
            TestHelper.runRawImageTest(r, "image", imgPath, TestHelper.Orientation.PORTRAIT, outDir, lang);
        }
        return r;
    }
}
