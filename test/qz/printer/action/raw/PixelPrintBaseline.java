package qz.printer.action.raw;

import qz.utils.ArgValue;

// Baseline generator for raw image output across all languages
public class PixelPrintBaseline {
    public static void main(String[] args) throws Exception {
        // print to file is off by default. Override for our tests
        System.setProperty(ArgValue.SECURITY_PRINT_TOFILE.getMatch(), "true");
        TestHelper.Result r = PixelPrintTests.runAll(PixelPrintTests.BASE_DIR);
        System.out.println("languages baseline: ok=" + r.ok + " skipped=" + r.skipped + " failed=" + r.failed);
        System.exit(r.failed > 0 ? 1 : 0);
    }
}
