package qz.printer.status;

import qz.printer.PrintServiceMatcher;
import qz.printer.info.NativePrinter;
import qz.printer.status.printer.WmiPrinterStatusMap;
import qz.utils.SystemUtilities;

/**
 * Container object for both printer and job statuses
 */
public class Status {
    private NativeStatus code;
    private String printer;
    private Object rawCode;
    private int jobId; // job statuses only

    public Status(NativeStatus code, String printer, Object rawCode) {
        this.code = code;
        this.printer = printer;
        this.rawCode = rawCode;
        this.jobId = -1;
    }

    public Status(NativeStatus.NativeMap code, String printer) {
        this.code = code.getParent();
        this.printer = printer;
        this.rawCode = code.getRawCode();
        this.jobId = -1;
    }

    public Status(NativeStatus code, String printer, Object rawCode, int jobId) {
        this.code = code;
        this.printer = printer;
        this.rawCode = rawCode;
        this.jobId = jobId;
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

    public NativeStatus getCode() {
        return code;
    }

    public Object getRawCode() {
        return rawCode;
    }

    public String getPrinter() {
        return printer;
    }

    public int getJobId() {
        return jobId;
    }

    public String toString() {
        return code.name() + ": Level " + code.getLevel() + ", From " + sanitizePrinterName() + ", Code " + rawCode + (jobId > 0 ? ", JobId: " + jobId : "");
    }
}
