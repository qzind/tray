package qz.printer.info.tests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.PrintServiceMatcher;
import qz.printer.info.NativePrinter;
import qz.printer.info.NativePrinterList;

import java.util.Date;

public class NativePrinterTests {
    private static final Logger log = LoggerFactory.getLogger(NativePrinterTests.class);

    public static void main(String ... args) {
        runTest();
        runTest();
        runTest();
        runTest();
        runTest();
        runTest();
        runTest();
        runTest();
    }

    private static void runTest() {
        Date begin = new Date();
        NativePrinterList printers = PrintServiceMatcher.getNativePrinterList();
        StringBuilder output = new StringBuilder("Found printers:\n");
        for (NativePrinter printer : printers.values()) {
            output.append(String.format("      printerId: '%s', description: '%s', driverFile: '%s', " +
                                                "resolution: '%s', driver: '%s'\n",
                                        printer.getPrinterId(),
                                        printer.getDescription(),
                                        printer.getDriverFile(),
                                        printer.getResolution(),
                                        printer.getDriver()
            ));
        }
        Date end = new Date();
        log.debug(output.toString());
        log.debug("Time to find printers: " + (end.getTime() - begin.getTime()));
    }

}
