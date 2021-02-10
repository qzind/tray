package qz.printer.status;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.status.job.CupsJobStatusMap;
import qz.printer.status.job.WmiJobStatusMap;
import qz.printer.status.printer.CupsPrinterStatusMap;
import qz.printer.status.printer.WmiPrinterStatusMap;
import qz.utils.ByteUtilities;

import java.util.Locale;

public interface NativeStatus {
    Logger log = LoggerFactory.getLogger(NativeStatus.class);

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
        return new Status(CupsJobStatusMap.match(reason, state), printer, reason, jobId, jobName);
    }


    static Status fromCupsPrinterStatus(String state, String reason, String printer) {
        if (reason == null) { return null; }
        reason = reason.toLowerCase(Locale.ENGLISH).replaceAll("-(error|warning|report)", "");

        NativeStatus printerStatus = CupsPrinterStatusMap.match(reason, state);

        return new Status(printerStatus, printer, reason);
    }
}
