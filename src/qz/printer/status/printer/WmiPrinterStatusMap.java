package qz.printer.status.printer;

import qz.printer.status.NativeStatus;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by kyle on 5/18/17.
 */
public enum WmiPrinterStatusMap implements NativeStatus.NativeMap {
    OK(NativePrinterStatus.OK, 0x00000000, true),
    PAUSED(NativePrinterStatus.PAUSED, 0x00000001, false),
    ERROR(NativePrinterStatus.ERROR, 0x00000002, false),
    PENDING_DELETION(NativePrinterStatus.PENDING_DELETION, 0x00000004, true),
    PAPER_JAM(NativePrinterStatus.PAPER_JAM, 0x00000008, false),
    PAPER_OUT(NativePrinterStatus.PAPER_OUT, 0x00000010, false),
    MANUAL_FEED(NativePrinterStatus.MANUAL_FEED, 0x00000020, false),
    PAPER_PROBLEM(NativePrinterStatus.PAPER_PROBLEM, 0x00000040, false),
    OFFLINE(NativePrinterStatus.OFFLINE, 0x00000080, false),
    IO_ACTIVE(NativePrinterStatus.IO_ACTIVE, 0x00000100, true),
    BUSY(NativePrinterStatus.BUSY, 0x00000200, true),
    PRINTING(NativePrinterStatus.PRINTING, 0x00000400, true),
    OUTPUT_BIN_FULL(NativePrinterStatus.OUTPUT_BIN_FULL, 0x00000800, false),
    NOT_AVAILABLE(NativePrinterStatus.NOT_AVAILABLE, 0x00001000, false),
    WAITING(NativePrinterStatus.WAITING, 0x00002000, true),
    PROCESSING(NativePrinterStatus.PROCESSING, 0x00004000, true),
    INITIALIZING(NativePrinterStatus.INITIALIZING, 0x00008000, true),
    WARMING_UP(NativePrinterStatus.WARMING_UP, 0x00010000, true),
    TONER_LOW(NativePrinterStatus.TONER_LOW, 0x00020000, true),
    NO_TONER(NativePrinterStatus.NO_TONER, 0x00040000, false),
    PAGE_PUNT(NativePrinterStatus.PAGE_PUNT, 0x00080000, true),
    USER_INTERVENTION(NativePrinterStatus.USER_INTERVENTION, 0x00100000, false),
    OUT_OF_MEMORY(NativePrinterStatus.OUT_OF_MEMORY, 0x00200000, false),
    DOOR_OPEN(NativePrinterStatus.DOOR_OPEN, 0x00400000, false),
    SERVER_UNKNOWN(NativePrinterStatus.SERVER_UNKNOWN, 0x00800000, false),
    POWER_SAVE(NativePrinterStatus.POWER_SAVE, 0x01000000, true),

    /**
     * For internal use only, not WMI values (change as needed)
     */
    // Used for mapping PRINTER_ATTRIBUTE_WORK_OFFLINE from printer attributes to printer status
    ATTRIBUTE_WORK_OFFLINE(NativePrinterStatus.OFFLINE, 0x04000000, false),
    // "Unknown" placeholder for future/unmapped values
    UNKNOWN_STATUS(NativePrinterStatus.UNKNOWN, 0x02000000, false);

    public static int NOT_OK_MASK = getNotOkMask();
    private static SortedMap<Integer,NativePrinterStatus> sortedLookupTable;

    private NativePrinterStatus parent;
    private final int rawCode;

    // Printer status isn't very good about reporting recovered errors, we'll try to track them manually
    private boolean isOk;

    WmiPrinterStatusMap(NativePrinterStatus parent, int rawCode, boolean isOK) {
        this.parent = parent;
        this.rawCode = rawCode;
        this.isOk = isOK;
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

    private static int getNotOkMask() {
        int result = 0;
        for(WmiPrinterStatusMap code : values()) {
            if(!code.isOk) {
                result |= code.rawCode;
            }
        }
        return result;
    }
}
