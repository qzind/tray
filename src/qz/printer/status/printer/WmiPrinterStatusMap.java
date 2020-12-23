package qz.printer.status.printer;

import qz.printer.status.Mappable;
import qz.printer.status.Statusable;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by kyle on 5/18/17.
 */
public enum WmiPrinterStatusMap implements Mappable {
    OK(PrinterStatus.OK, 0x00000000),
    PAUSED(PrinterStatus.PAUSED, 0x00000001),
    ERROR(PrinterStatus.ERROR, 0x00000002),
    PENDING_DELETION(PrinterStatus.PENDING_DELETION, 0x00000004),
    PAPER_JAM(PrinterStatus.PAPER_JAM, 0x00000008),
    PAPER_OUT(PrinterStatus.PAPER_OUT, 0x00000010),
    MANUAL_FEED(PrinterStatus.MANUAL_FEED, 0x00000020),
    PAPER_PROBLEM(PrinterStatus.PAPER_PROBLEM, 0x00000040),
    OFFLINE(PrinterStatus.OFFLINE, 0x00000080),
    IO_ACTIVE(PrinterStatus.IO_ACTIVE, 0x00000100),
    BUSY(PrinterStatus.BUSY, 0x00000200),
    PRINTING(PrinterStatus.PRINTING, 0x00000400),
    OUTPUT_BIN_FULL(PrinterStatus.OUTPUT_BIN_FULL, 0x00000800),
    NOT_AVAILABLE(PrinterStatus.NOT_AVAILABLE, 0x00001000),
    WAITING(PrinterStatus.WAITING, 0x00002000),
    PROCESSING(PrinterStatus.PROCESSING, 0x00004000),
    INITIALIZING(PrinterStatus.INITIALIZING, 0x00008000),
    WARMING_UP(PrinterStatus.WARMING_UP, 0x00010000),
    TONER_LOW(PrinterStatus.TONER_LOW, 0x00020000),
    NO_TONER(PrinterStatus.NO_TONER, 0x00040000),
    PAGE_PUNT(PrinterStatus.PAGE_PUNT, 0x00080000),
    USER_INTERVENTION(PrinterStatus.USER_INTERVENTION, 0x00100000),
    OUT_OF_MEMORY(PrinterStatus.OUT_OF_MEMORY, 0x00200000),
    DOOR_OPEN(PrinterStatus.DOOR_OPEN, 0x00400000),
    SERVER_UNKNOWN(PrinterStatus.SERVER_UNKNOWN, 0x00800000),
    POWER_SAVE(PrinterStatus.POWER_SAVE, 0x01000000),
    UNKNOWN_STATUS(PrinterStatus.UNKNOWN_STATUS, 0x02000000);

    private static SortedMap<Integer, Statusable.Codeable> sortedLookupTable;

    private Statusable.Codeable parent;
    private final int code;

    WmiPrinterStatusMap(Statusable.Codeable parent, int code) {
        this.parent = parent;
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    static Statusable.Codeable match(int code) {
        // Initialize a sorted map to speed up lookups
        if(sortedLookupTable == null) {
            sortedLookupTable = new TreeMap<>();
            for(WmiPrinterStatusMap value : values()) {
                sortedLookupTable.put(value.code, value.parent);
            }
        }

        return sortedLookupTable.get(code);
    }

    /**
     * Unwinds an integer into an array of <code>PrinterStatus.Code</code>
     */
    public static Statusable.Codeable[] unwind(int bitwiseCode) {
        int size = Integer.bitCount(bitwiseCode);
        Statusable.Codeable[] matches = new Statusable.Codeable[size];
        int mask = 1;

        while(size > 0) {
            if ((mask & bitwiseCode) > 0) {
                matches[--size] = match(mask);
            }
            mask <<= 1;
        }
        return matches;
    }
}
