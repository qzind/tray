package qz.printer.action.raw;

import qz.utils.ArgValue;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PixelPrintBaseline {
    public static void main(String[] args) throws Exception {
        System.setProperty(ArgValue.SECURITY_PRINT_TOFILE.getMatch(), "true");
        Path base = Paths.get("./test/baseline");
        TestHelper.Result r = PixelPrintTests.runAll(base.resolve("image-print-tests"));
        System.out.println("languages baseline: ok=" + r.ok + " skipped=" + r.skipped + " failed=" + r.failed);
        System.exit(0);
    }
}
