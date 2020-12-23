package qz.printer.status.printer;

import org.apache.log4j.Level;
import qz.printer.status.StatusContainer;
import qz.printer.status.Statusable;

import java.util.Locale;
import static org.apache.log4j.Level.*;

/**
 * Created by kyle on 7/7/17.
 */
public enum PrinterStatus implements Statusable.Codeable {
    OK(INFO),
    PAUSED(WARN),
    ERROR(FATAL),
    PENDING_DELETION(WARN),
    PAPER_JAM(FATAL),
    PAPER_OUT(FATAL),
    MANUAL_FEED(INFO),
    PAPER_PROBLEM(FATAL),
    OFFLINE(FATAL),
    IO_ACTIVE(INFO),
    BUSY(INFO),
    PRINTING(INFO),
    OUTPUT_BIN_FULL(FATAL),
    NOT_AVAILABLE(FATAL),
    WAITING(INFO),
    PROCESSING(INFO),
    INITIALIZING(INFO),
    WARMING_UP(INFO),
    TONER_LOW(WARN),
    NO_TONER(FATAL),
    PAGE_PUNT(FATAL),
    USER_INTERVENTION(WARN),
    OUT_OF_MEMORY(FATAL),
    DOOR_OPEN(WARN),
    SERVER_UNKNOWN(WARN),
    POWER_SAVE(INFO),
    UNKNOWN_STATUS(FATAL);

    private Level level;

    PrinterStatus(Level level) {
        this.level = level;
    }

    @Override
    public Statusable.Codeable getDefault() {
        return UNKNOWN_STATUS;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    //public Code printerStatusCode;
    //public String cupsString;
    //private String printer;

    /*public PrinterStatus(Code printerStatusCode, String printer) {
        this(printerStatusCode, printer, "");
    }

    public PrinterStatus(Code printerStatusCode, String printer, String cupsString) {
        this.printerStatusCode = printerStatusCode;
        this.printer = printer;
        this.cupsString = cupsString;
    }*/

    public static StatusContainer[] fromWmi(int bitwiseCode, String printer) {
        // Printers generally have a single status at a time however, bitwise
        // operators allow multiple statuses so we'll prepare an array to accommodate
        Statusable.Codeable[] printerStatusCodes = WmiPrinterStatusMap.unwind(bitwiseCode);
        StatusContainer[] statusArray = new StatusContainer[printerStatusCodes.length];
        int i = 0;
        for(Statusable.Codeable printerStatusCode : printerStatusCodes) {
            statusArray[i++] = new StatusContainer(printerStatusCode, printer);
        }
        return statusArray;
    }

    public static StatusContainer fromCups(String reason, String printer) {
        if (reason == null) { return null; }

        reason = reason.toLowerCase(Locale.ENGLISH).replaceAll("-(error|warning|report)", "");

        Statusable.Codeable printerStatus = CupsPrinterStatusMap.match(reason);
        if (printerStatus == null) { printerStatus = UNKNOWN_STATUS; }

        return new StatusContainer(printerStatus, printer);
    }

    /**
    @Override
    public Code getCode() {
        return printerStatusCode;
    }

    @Override
    public String getPrinter() {
        return printer;
    }

    @Override
    public String getCupsString() {
        return cupsString;
    }

    public String toString() {
        return printerStatusCode.name() + ": Level " + printerStatusCode.level + ", StatusCode " + ", From " + sanitizePrinterName();
    }
    */
}
