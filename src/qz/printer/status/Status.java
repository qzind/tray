package qz.printer.status;

import qz.printer.PrintServiceMatcher;
import qz.printer.info.NativePrinter;
import qz.printer.status.job.NativeJobStatus;
import qz.printer.status.printer.NativePrinterStatus;
import qz.utils.SystemUtilities;

/**
 * Container object for both printer and job statuses
 */
public class Status {
    private NativeStatus code;
    private String printer;
    private Object rawCode;
    private EventType eventType;
    private int jobId; // job statuses only
    private String jobName; // job status only

    enum EventType {
        JOB,
        PRINTER;
    }

    public Status(NativePrinterStatus code, String printer, Object rawCode) {
        this.code = code;
        this.printer = printer;
        this.rawCode = rawCode;
        this.jobId = -1;
        this.eventType = EventType.PRINTER;
    }

    public Status(NativeJobStatus code, String printer, Object rawCode, int jobId, String jobName) {
        this.code = code;
        this.printer = printer;
        this.rawCode = rawCode;
        this.jobId = jobId;
        this.jobName = jobName;
        this.eventType = EventType.JOB;
    }

    public String sanitizePrinterName() {
        if(!SystemUtilities.isMac()) {
            return printer;
        }

        // On MacOS the description is used as the printer name
        NativePrinter nativePrinter = PrintServiceMatcher.matchPrinter(printer, true);
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

    public EventType getEventType() {
        return eventType;
    }

    public String getJobName() {
        return jobName;
    }

    public int getJobId() {
        return jobId;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Status) {
            Status status = (Status)obj;
            return status.eventType == eventType && status.printer.equals(printer) && status.jobId == jobId && rawCode.equals(status.rawCode);
        }
        return super.equals(obj);
    }

    public String toString() {
        return code.name() + ": Level: " + code.getLevel() +
                ", From: " + sanitizePrinterName() +
                ", EventType: " + eventType +
                ", Code: " + rawCode +
                (jobId > 0 ? ", JobId: " + jobId : "") +
                (jobName != null ? ", Job Name: " + jobName : "");
    }
}
