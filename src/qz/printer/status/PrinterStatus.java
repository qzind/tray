package qz.printer.status;

import qz.printer.PrintServiceMatcher;
import qz.printer.info.NativePrinter;
import qz.utils.SystemUtilities;

import java.util.Locale;

import static qz.printer.status.PrinterStatusType.*;

/**
 * Created by kyle on 7/7/17.
 */
public class PrinterStatus {

    public PrinterStatusType type;
    public String cupsString;

    private String issuingPrinterName;


    public PrinterStatus(PrinterStatusType type, String issuingPrinterName) {
        this(type, issuingPrinterName, "");
    }

    public PrinterStatus(PrinterStatusType type, String issuingPrinterName, String cupsString) {
        this.type = type;
        this.issuingPrinterName = issuingPrinterName;
        this.cupsString = cupsString;
    }

    public static PrinterStatus[] getFromWMICode(int code, String issuingPrinterName) {
        if (code == 0) {
            return new PrinterStatus[] {new PrinterStatus(OK, issuingPrinterName)};
        }

        int bitPopulation = Integer.bitCount(code);
        PrinterStatus[] statusArray = new PrinterStatus[bitPopulation];
        int mask = 1;

        while(bitPopulation > 0) {
            if ((mask & code) > 0) {
                statusArray[--bitPopulation] = new PrinterStatus(codeLookupTable.get(mask), issuingPrinterName);
            }
            mask <<= 1;
        }
        return statusArray;
    }

    public static PrinterStatus getFromCupsString(String reason, String issuingPrinterName) {
        if (reason == null) { return null; }

        reason = reason.toLowerCase(Locale.ENGLISH).replaceAll("-(error|warning|report)", "");

        PrinterStatusType statusType = cupsLookupTable.get(reason);
        if (statusType == null) { statusType = UNKNOWN_STATUS; }

        return new PrinterStatus(statusType, issuingPrinterName, reason);
    }

    public String getIssuingPrinterName() {
        return issuingPrinterName;
    }

    /**
     * Returns a macOS-compatible (as well as Linux/Windows compatible) printer name for reporting back to WebSocket
     */
    public String getIssuingPrinterName(boolean isMacOS) {
        if(!isMacOS) {
            return issuingPrinterName;
        }

        //On MacOS the description is used as the printer name
        NativePrinter nativePrinter = PrintServiceMatcher.matchPrinter(issuingPrinterName);
        if (nativePrinter == null) {
            //If the printer description is missing from the map (usually because the printer was deleted), use the cups id instead
            return issuingPrinterName;
        }
        return nativePrinter.getPrintService().value().getName();
    }

    public String toString() {
        String returnString = type.getName() + ": Level " + type.getSeverity() + ", StatusCode " + type.getCode() + ", From " + getIssuingPrinterName(SystemUtilities.isMac());
        if (!cupsString.isEmpty()) {
            returnString += ", CUPS string " + cupsString;
        }
        return returnString;
    }
}
