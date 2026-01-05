package qz.printer.action.raw;

// Baseline generator for raw image output across all languages
public class LanguageBaselines {
    public static void main(String[] args) throws Exception {
        TestHelper.setupEnviroment();

        TestHelper.Result r = LanguageTests.runAll(LanguageTests.BASE_DIR);
        r.logSummary();
        System.exit(r.passed() ? 0 : 1);
    }
}
