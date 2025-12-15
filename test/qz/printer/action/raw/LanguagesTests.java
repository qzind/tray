package qz.printer.action.raw;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ArgValue;
import java.nio.file.Paths;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs a raw image conversion (type: raw, format: image) for every supported language.
 * Outputs per-language files under ./out/raw-all-langs/<language>/raw-image-portrait.zpl
 */
public class LanguagesTests {

    private static final Logger log = LogManager.getLogger(LanguagesTests.class);

    private static final Path OUT_BASE = Paths.get("./out/all-langs");
    private static final Path RES_DIR = Paths.get("test/qz/printer/action/resources");

    public static void main(String[] args) throws Exception {
        System.setProperty(ArgValue.SECURITY_PRINT_TOFILE.getMatch(), "true");

        boolean ok = true;
        try {
            TestHelper.Result r = runAll(OUT_BASE);
            TestHelper.assertMatchesBaseline(OUT_BASE, Paths.get("./test/baseline/all-langs"));
            log.info("All-languages run complete. ok={}, skipped={}, failed={}", r.ok, r.skipped, r.failed);
            log.info("Baseline matched for all languages");
        } catch (Throwable t) {
            ok = false;
            log.error("Baseline mismatch for all languages: {}", t.getMessage());
        }
        System.exit(ok ? 0 : 1);
    }

    public static TestHelper.Result runAll(Path outBase) throws Exception {
        Files.createDirectories(outBase);
        Path imgPath = RES_DIR.resolve("image_sample_bw.png");
        TestHelper.requireExists(imgPath);

        TestHelper.Result result = new TestHelper.Result();
        for (LanguageType lang : LanguageType.values()) {
            if (lang == LanguageType.UNKNOWN) { continue; }
            Path outDir = outBase.resolve(lang.name().toLowerCase());
            TestHelper.runRawImageTest(result, "image", imgPath, TestHelper.Orientation.PORTRAIT, outDir, lang);
        }
        return result;
    }
}
