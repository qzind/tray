package qz.printer.status;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.status.job.CupsJobStatusMap;
import qz.printer.status.job.WmiJobStatusMap;
import qz.printer.status.printer.CupsPrinterStatusMap;
import qz.printer.status.printer.NativePrinterStatus;
import qz.printer.status.printer.WmiPrinterStatusMap;

import java.util.Locale;

public interface NativeStatus {
    Logger log = LoggerFactory.getLogger(NativeStatus.class);

    enum NativeType {
        JOB,
        PRINTER
    }

    interface NativeMap {
        NativeStatus getParent();
        Object getRawCode();
    }
    
    NativeStatus getDefault(); //static
    String name();
    Level getLevel();

    static int[] unwind(int bitwiseCode) {
        int bitPopulation = Integer.bitCount(bitwiseCode);
        int[] matches = new int[bitPopulation];
        int mask = 1;

        while(bitPopulation > 0) {
            if ((mask & bitwiseCode) > 0) {
                matches[--bitPopulation] = mask;
            }
            mask <<= 1;
        }
        return matches;
    }

    /**
     * Assume <code>int[]</code> is Windows/Wmi
     */
    static NativeStatus[] fromRaw(int[] rawArray, NativeType nativeType) {
        NativeStatus[] parentCodes = new NativeStatus[rawArray.length];
        for(int i = 0; i < rawArray.length; i++) {
            switch(nativeType) {
                case JOB:
                    parentCodes[i] = WmiJobStatusMap.match(rawArray[i]);
                    break;
                case PRINTER:
                    parentCodes[i] = WmiPrinterStatusMap.match(rawArray[i]);
                default:

            }
        }
        return parentCodes;
    }

    /**
     * Assume <code>String[]</code> is Linux/Unix/Cups
     */
    static NativeStatus[] fromRaw(String[] rawArray, NativeType nativeType) {
        NativeStatus[] parentCodes = new NativeStatus[rawArray.length];
        for(int i = 0; i < rawArray.length; i++) {
            switch(nativeType) {
                case JOB:
                    //parentCodes[i] = CupsJobStatusMap.match(rawArray[i]);
                    break;
                case PRINTER:
                    //todo add state
                    parentCodes[i] = CupsPrinterStatusMap.match(rawArray[i], "UNKNOWN_STATUS");
                default:

            }
        }
        return parentCodes;
    }

    /**
     * Printers/Jobs generally have a single status at a time however, bitwise
     * operators allow multiple statuses so we'll prepare an array to accommodate
     */
    static Status[] fromWmi(int bitwiseCode, String printer, NativeType nativeType, int jobId, String jobName) {
        int[] rawCodes = unwind(bitwiseCode);
        NativeStatus[] parentCodes = fromRaw(rawCodes, nativeType);

        Status[] statusArray = new Status[rawCodes.length];
        for(int i = 0; i < rawCodes.length; i++) {
            statusArray[i] = new Status(parentCodes[i], printer, rawCodes[i], jobId, jobName);
        }
        return statusArray;
    }

    static Status[] fromWmi(int bitwiseCode, String printer, NativeType nativeType) {
        return fromWmi(bitwiseCode, printer, nativeType, -1, null);
    }


    static Status fromCups(String reason, String state, String printer, int jobId, NativeType nativeType) {
        return new Status(CupsJobStatusMap.match(reason, state), printer, reason, jobId, printer + jobId);
    }


    static Status fromCups(String state, String reason, String printer, NativeType nativeType) {
        if (reason == null) { return null; }
        reason = reason.toLowerCase(Locale.ENGLISH).replaceAll("-(error|warning|report)", "");

        NativeStatus printerStatus = CupsPrinterStatusMap.match(reason, state);

        return new Status(printerStatus, printer, reason);
    }
}
