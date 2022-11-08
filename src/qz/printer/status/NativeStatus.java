package qz.printer.status;

import org.apache.logging.log4j.Level;
import qz.printer.status.job.CupsJobStatusMap;
import qz.printer.status.job.NativeJobStatus;
import qz.printer.status.job.WmiJobStatusMap;
import qz.printer.status.printer.CupsPrinterStatusMap;
import qz.printer.status.printer.NativePrinterStatus;
import qz.printer.status.printer.WmiPrinterStatusMap;
import qz.utils.ByteUtilities;

public interface NativeStatus {
    interface NativeMap {
        NativeStatus getParent();
        Object getRawCode();
    }

    NativeStatus getDefault(); //static
    String name();
    Level getLevel();

    /**
     * Printers/Jobs generally have a single status at a time however, bitwise
     * operators allow multiple statuses so we'll prepare an array to accommodate
     */
    static Status[] fromWmiJobStatus(int bitwiseCode, String printer, int jobId, String jobName) {
        int[] rawCodes = ByteUtilities.unwind(bitwiseCode);
        NativeJobStatus[] parentCodes = new NativeJobStatus[rawCodes.length];
        for(int i = 0; i < rawCodes.length; i++) {
            parentCodes[i] = WmiJobStatusMap.match(rawCodes[i]);
        }

        Status[] statusArray = new Status[rawCodes.length];
        for(int i = 0; i < rawCodes.length; i++) {
            statusArray[i] = new Status(parentCodes[i], printer, rawCodes[i], jobId, jobName);
        }
        return statusArray;
    }

    static Status[] fromWmiPrinterStatus(int bitwiseCode, String printer) {
        int[] rawCodes = ByteUtilities.unwind(bitwiseCode);
        // WmiPrinterStatusMap has an explicit 0x00000000 = OK, so we'll need to shim that
        if(rawCodes.length == 0) {
            rawCodes = new int[] { (Integer)WmiPrinterStatusMap.OK.getRawCode() };
        }
        NativePrinterStatus[] parentCodes = new NativePrinterStatus[rawCodes.length];
        for(int i = 0; i < rawCodes.length; i++) {
            parentCodes[i] = WmiPrinterStatusMap.match(rawCodes[i]);
        }

        Status[] statusArray = new Status[rawCodes.length];
        for(int i = 0; i < rawCodes.length; i++) {
            statusArray[i] = new Status(parentCodes[i], printer, rawCodes[i]);
        }
        return statusArray;
    }


    static Status fromCupsJobStatus(String reason, String state, String printer, int jobId, String jobName) {
        // First check known job-state-reason pairs
        NativeJobStatus cupsJobStatus = CupsJobStatusMap.matchReason(reason);
        if(cupsJobStatus == null) {
            // Don't return the raw job-state-reason if we couldn't find it mapped, return job-state instead
            return new Status(CupsJobStatusMap.matchState(state), printer, state, jobId, jobName);
        } else if(cupsJobStatus == NativeJobStatus.UNMAPPED) {
            // Still lookup the job-state, but let the user know what the unmapped job-state-reason was
            return new Status(CupsJobStatusMap.matchState(state), printer, reason, jobId, jobName);
        }
        return new Status(cupsJobStatus, printer, reason, jobId, jobName);
    }


    static Status fromCupsPrinterStatus(String reason, String state, String printer) {
        return CupsPrinterStatusMap.createStatus(reason, state, printer);
    }
}
