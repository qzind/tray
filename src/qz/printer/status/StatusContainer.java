package qz.printer.status;

import qz.printer.PrintServiceMatcher;
import qz.printer.info.NativePrinter;
import qz.utils.SystemUtilities;

public class StatusContainer {
    private Statusable.Codeable code;
    private String printer;
    public StatusContainer(Statusable.Codeable code, String printer) {
        this.code = code;
        this.printer = printer;
    }
    public String sanitizePrinterName() {
        if(!SystemUtilities.isMac()) {
            return printer;
        }

        // On MacOS the description is used as the printer name
        NativePrinter nativePrinter = PrintServiceMatcher.matchPrinter(printer);
        if (nativePrinter == null) {
            // If the printer description is missing from the map (usually because the printer was deleted), use the cups id instead
            return printer;
        }
        return nativePrinter.getPrintService().value().getName();
    }

    public Statusable.Codeable getCode() {
        return code;
    }

    public String toString() {
        return code.name() + ": Level " + code.getLevel() + ", From " + sanitizePrinterName();
    }
}
