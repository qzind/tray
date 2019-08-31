package qz.printer.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.PrintServiceMatcher;

import java.util.Date;

public class TestClass {
    private static final Logger log = LoggerFactory.getLogger(CupsWrapper.class);

    public static void main(String ... args) {
        Date begin = new Date();
        //log.debug("Started finding printers");
        NativePrinterList printers = Wrapper.getInstance().wrapServices(PrintServiceMatcher.getPrintServices(), null);
        //log.debug("Done finding printers");
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
        log.debug(output.toString());
        log.debug("Time to find printers: " + (new Date().getTime() - begin.getTime()));
    }

}
