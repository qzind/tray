package qz.printer.status;

import org.apache.log4j.Level;
import qz.printer.PrintServiceMatcher;
import qz.printer.info.NativePrinter;
import qz.printer.status.printer.CupsPrinterStatusMap;
import qz.printer.status.printer.PrinterStatus;
import qz.printer.status.printer.WmiPrinterStatusMap;
import qz.utils.SystemUtilities;

import java.util.Locale;

/**
 * Common interface for enum-managing bitwise classes such as <code>PrinterStaus</code> and <code>JobStatus</code>
 */
public interface Statusable {
    interface Codeable {
        String name();
        Codeable getDefault(); //static
        Level getLevel();
        static StatusContainer[] fromWmi(int bitwiseCode, String printer) {
            // Printers generally have a single status at a time however, bitwise
            // operators allow multiple statuses so we'll prepare an array to accommodate
            Statusable.Codeable[] statusCodes = WmiPrinterStatusMap.unwind(bitwiseCode);
            StatusContainer[] statusArray = new StatusContainer[statusCodes.length];
            int i = 0;
            for(Statusable.Codeable statusCode : statusCodes) {
                statusArray[i++] = new StatusContainer(statusCode, printer);
            }
            return statusArray;
        }
     }



    /**
     * Returns a macOS-compatible (as well as Linux/Windows compatible) printer name for reporting back to WebSocket
     */

}
