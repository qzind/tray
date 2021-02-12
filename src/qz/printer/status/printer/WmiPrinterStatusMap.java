package qz.printer.status.printer;

import qz.printer.status.NativeStatus;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by kyle on 5/18/17.
 */
public enum WmiPrinterStatusMap implements NativeStatus.NativeMap {
    OK(NativePrinterStatus.OK, 0x00000000),
    PAUSED(NativePrinterStatus.PAUSED, 0x00000001),
    ERROR(NativePrinterStatus.ERROR, 0x00000002),
    PENDING_DELETION(NativePrinterStatus.PENDING_DELETION, 0x00000004),
    PAPER_JAM(NativePrinterStatus.PAPER_JAM, 0x00000008),
    PAPER_OUT(NativePrinterStatus.PAPER_OUT, 0x00000010),
    MANUAL_FEED(NativePrinterStatus.MANUAL_FEED, 0x00000020),
    PAPER_PROBLEM(NativePrinterStatus.PAPER_PROBLEM, 0x00000040),
    OFFLINE(NativePrinterStatus.OFFLINE, 0x00000080),
    IO_ACTIVE(NativePrinterStatus.IO_ACTIVE, 0x00000100),
    BUSY(NativePrinterStatus.BUSY, 0x00000200),
    PRINTING(NativePrinterStatus.PRINTING, 0x00000400),
    OUTPUT_BIN_FULL(NativePrinterStatus.OUTPUT_BIN_FULL, 0x00000800),
    NOT_AVAILABLE(NativePrinterStatus.NOT_AVAILABLE, 0x00001000),
    WAITING(NativePrinterStatus.WAITING, 0x00002000),
    PROCESSING(NativePrinterStatus.PROCESSING, 0x00004000),
    INITIALIZING(NativePrinterStatus.INITIALIZING, 0x00008000),
    WARMING_UP(NativePrinterStatus.WARMING_UP, 0x00010000),
    TONER_LOW(NativePrinterStatus.TONER_LOW, 0x00020000),
    NO_TONER(NativePrinterStatus.NO_TONER, 0x00040000),
    PAGE_PUNT(NativePrinterStatus.PAGE_PUNT, 0x00080000),
    USER_INTERVENTION(NativePrinterStatus.USER_INTERVENTION, 0x00100000),
    OUT_OF_MEMORY(NativePrinterStatus.OUT_OF_MEMORY, 0x00200000),
    DOOR_OPEN(NativePrinterStatus.DOOR_OPEN, 0x00400000),
    SERVER_UNKNOWN(NativePrinterStatus.SERVER_UNKNOWN, 0x00800000),
    POWER_SAVE(NativePrinterStatus.POWER_SAVE, 0x01000000),
    UNKNOWN_STATUS(NativePrinterStatus.UNKNOWN, 0x02000000);

    private static SortedMap<Integer,NativePrinterStatus> sortedLookupTable;

    private NativePrinterStatus parent;
    private final int rawCode;

    WmiPrinterStatusMap(NativePrinterStatus parent, int rawCode) {
        this.parent = parent;
        this.rawCode = rawCode;
    }

    public static NativePrinterStatus match(int rawCode) {
        // Initialize a sorted map to speed up lookups
        if(sortedLookupTable == null) {
            sortedLookupTable = new TreeMap<>();
            for(WmiPrinterStatusMap value : values()) {
                sortedLookupTable.put(value.rawCode, value.parent);
            }
        }

        return sortedLookupTable.get(rawCode);
    }

    @Override
    public NativeStatus getParent() {
        return parent;
    }

    @Override
    public Object getRawCode() {
        return rawCode;
    }
}
