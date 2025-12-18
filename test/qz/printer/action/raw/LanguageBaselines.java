package qz.printer.action.raw;

import qz.utils.ArgValue;

// Baseline generator for raw image output across all languages
public class LanguageBaselines {
    public static void main(String[] args) throws Exception {
        // print to file is off by default. Override for our tests
        System.setProperty(ArgValue.SECURITY_PRINT_TOFILE.getMatch(), "true");
        System.setProperty(ArgValue.SECURITY_DATA_PROTOCOLS.getMatch(), "http,https,file");

        TestHelper.Result r = LanguageTests.runAll(LanguageTests.BASE_DIR);
        r.logSummary();
        System.exit(r.passed() ? 0 : 1);
    }
}
