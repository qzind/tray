package qz.printer.status.job;

import org.apache.log4j.Level;
import qz.printer.status.Mappable;
import qz.printer.status.StatusContainer;
import qz.printer.status.Statusable;
import qz.printer.status.printer.WmiPrinterStatusMap;

/**
 * Created by kyle on 7/7/17.
 */
public enum JobStatus implements Statusable.Codeable {
    EMPTY(Level.INFO),
    PAUSED(Level.WARN),
    ERROR(Level.ERROR),
    DELETING(Level.WARN),
    SPOOLING(Level.INFO),
    PRINTING(Level.INFO),
    OFFLINE(Level.ERROR),
    PAPEROUT(Level.WARN),
    RETAINED(Level.INFO),
    PRINTED(Level.INFO),
    DELETED(Level.WARN),
    BLOCKED_DEVQ(Level.ERROR),
    USER_INTERVENTION(Level.WARN),
    RESTART(Level.WARN),
    COMPLETE(Level.INFO),
    RENDERING_LOCALLY(Level.INFO);

    private Level level;

    JobStatus(Level level) {
        this.level = level;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public Statusable.Codeable getDefault() {
        return EMPTY;
    }

    public static StatusContainer[] fromWmi(int bitwiseCode, String printer, Mappable mappable) {
        // Printers generally have a single status at a time however, bitwise
        // operators allow multiple statuses so we'll prepare an array to accommodate
        Statusable.Codeable[] printerStatusCodes = mappable instanceof WmiPrinterStatusMap ?
                WmiPrinterStatusMap.unwind(bitwiseCode) : WmiJobStatusMap.unwind(bitwiseCode);
        StatusContainer[] statusArray = new StatusContainer[printerStatusCodes.length];
        int i = 0;
        for(Statusable.Codeable printerStatusCode : printerStatusCodes) {
            statusArray[i++] = new StatusContainer(printerStatusCode, printer);
        }
        return statusArray;
    }



    /**public JobStatusCode code;
    public String cupsString;
    private String printer;


    public JobStatus(JobStatusCode code, String printer) {
        this(code, printer, "");
    }

    public JobStatus(JobStatusCode code, String printer, String cupsString) {
        this.code = code;
        this.printer = printer;
        this.cupsString = cupsString;
    }

    public static JobStatus[] fromWMI(int bitwiseCode, String issuingPrinterName) {
        // Printers generally have a single status at a time however, bitwise
        // operators allow multiple statuses so we'll prepare an array to accommodate
        JobStatusCode[] jobStatusCodes = JobStatusCode.unwind(bitwiseCode);
        JobStatus[] statusArray = new JobStatus[jobStatusCodes.length];
        int i = 0;
        for(JobStatusCode jobStatusCode : jobStatusCodes) {
            statusArray[i++] = new JobStatus(jobStatusCode, issuingPrinterName);
        }
        return statusArray;
    }

    public static JobStatus fromCUPS(String reason, String issuingPrinterName) {
        if (reason == null) { return null; }

        reason = reason.toLowerCase(Locale.ENGLISH).replaceAll("-(error|warning|report)", "");

        JobStatusCode jobStatus = JobStatusCode.CUPS_LOOKUP_TABLE.get(reason);
        if (jobStatus == null) { jobStatus = JobStatusCode.EMPTY; }

        return new JobStatus(jobStatus, issuingPrinterName, reason);
    }

    public String getPrinter() {
        return printer;
    }

    /**
     * Returns a macOS-compatible (as well as Linux/Windows compatible) printer name for reporting back to WebSocket
     *
    public String sanitizePrinter() {
        if(!SystemUtilities.isMac()) {
            return printer;
        }

        //On MacOS the description is used as the printer name
        NativePrinter nativePrinter = PrintServiceMatcher.matchPrinter(printer);
        if (nativePrinter == null) {
            //If the printer description is missing from the map (usually because the printer was deleted), use the cups id instead
            return printer;
        }
        return nativePrinter.getPrintService().value().getName();
    }

    public String toString() {
        String returnString = code.getName() + ": Level " + code.getSeverity() + ", StatusCode " + code.getCode() + ", From " + sanitizePrinter();
        if (!cupsString.isEmpty()) {
            returnString += ", CUPS string " + cupsString;
        }
        return returnString;
    }
     **/
}
