package qz.printer.status;

import org.apache.log4j.Level;
import qz.printer.info.NativePrinter;
import qz.printer.status.job.CupsJobStatusMap;
import qz.printer.status.job.NativeJobStatus;
import qz.printer.status.job.WmiJobStatusMap;
import qz.printer.status.printer.CupsPrinterStatusMap;
import qz.printer.status.printer.NativePrinterStatus;
import qz.printer.status.printer.WmiPrinterStatusMap;
import qz.utils.ByteUtilities;

import java.util.Locale;

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
        NativeStatus[] parentCodes = new NativeStatus[rawCodes.length];
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
        NativeStatus[] parentCodes = new NativeStatus[rawCodes.length];
        for(int i = 0; i < rawCodes.length; i++) {
            parentCodes[i] = WmiPrinterStatusMap.match(rawCodes[i]);
        }

        Status[] statusArray = new Status[rawCodes.length];
        for(int i = 0; i < rawCodes.length; i++) {
            statusArray[i] = new Status(parentCodes[i], printer, rawCodes[i], -1, null);
        }
        return statusArray;
    }


    static Status fromCupsJobStatus(String reason, String state, String printer, int jobId, String jobName) {
        NativeJobStatus cupsJobStatus = CupsJobStatusMap.matchReason(reason);
        if(cupsJobStatus == null) {
            // Don't return the raw reason if we couldn't find it mapped, return state instead
            return new Status(CupsJobStatusMap.matchState(state), printer, state, jobId, jobName);
        } else if(cupsJobStatus == NativeJobStatus.UNMAPPED) {
            // Still return the state, but let the user know what the unmapped state reason was
            return new Status(CupsJobStatusMap.matchState(state), printer, reason, jobId, jobName);
        }
        return new Status(cupsJobStatus, printer, reason, jobId, jobName);
    }


    static Status fromCupsPrinterStatus(String reason, String state, String printer) {
        if (reason == null) { return null; }
        reason = reason.toLowerCase(Locale.ENGLISH).replaceAll("-(error|warning|report)", "");

        NativePrinterStatus cupsPrinterStatus = CupsPrinterStatusMap.matchReason(reason);
        if(cupsPrinterStatus == null) {
            // Don't return the raw reason if we couldn't find it mapped, return state instead
            return new Status(CupsPrinterStatusMap.matchState(state), printer, state);
        } else if(cupsPrinterStatus == NativePrinterStatus.UNMAPPED) {
            // Still return the state, but let the user know what the unmapped state reason was
            return new Status(CupsPrinterStatusMap.matchState(state), printer, reason);
        }
        return new Status(cupsPrinterStatus, printer, reason);
    }
}
