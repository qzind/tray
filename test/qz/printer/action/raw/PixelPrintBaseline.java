package qz.printer.action.raw;

// Baseline generator for raw image output across multiple formats and orientations
public class PixelPrintBaseline {
    public static void main(String[] args) throws Exception {
        TestHelper.setupEnviroment();

        TestHelper.Result r = PixelPrintTests.runAll(PixelPrintTests.BASE_DIR);
        r.logSummary();
        System.exit(r.passed() ? 0 : 1);
    }
}
