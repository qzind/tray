package qz.printer.status.printer;

import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public enum CupsPrinterStatusMap {
    OTHER(PrinterStatus.UNKNOWN_STATUS), // "other"
    NONE(PrinterStatus.OK), // "none"
    MEDIA_NEEDED(PrinterStatus.PAPER_OUT), // "media-needed"
    MEDIA_JAM(PrinterStatus.PAPER_JAM), // "media-jam"
    MOVING_TO_PAUSED(PrinterStatus.OK), // "moving-to-paused"
    PAUSED(PrinterStatus.PAUSED), // "paused"
    SHUTDOWN(PrinterStatus.OFFLINE), // "shutdown"
    CONNECTING_TO_DEVICE(PrinterStatus.OK), // "connecting-to-device"
    TIMED_OUT(PrinterStatus.NOT_AVAILABLE), // "timed-out"
    STOPPING(PrinterStatus.OK), // "stopping"
    STOPPED_PARTLY(PrinterStatus.PAUSED), // "stopped-partly"
    TONER_LOW(PrinterStatus.TONER_LOW), // "toner-low"
    TONER_EMPTY(PrinterStatus.NO_TONER), // "toner-empty"
    SPOOL_AREA_FULL(PrinterStatus.OUT_OF_MEMORY), // "spool-area-full"
    COVER_OPEN(PrinterStatus.DOOR_OPEN), // "cover-open"
    INTERLOCK_OPEN(PrinterStatus.DOOR_OPEN), // "interlock-open"
    DOOR_OPEN(PrinterStatus.DOOR_OPEN), // "door-open"
    INPUT_TRAY_MISSING(PrinterStatus.PAPER_PROBLEM), // "input-tray-missing"
    MEDIA_LOW(PrinterStatus.PAPER_PROBLEM), // "media-low"
    MEDIA_EMPTY(PrinterStatus.PAPER_OUT), // "media-empty"
    OUTPUT_TRAY_MISSING(PrinterStatus.PAPER_PROBLEM), // "output-tray-missing"
    //not a great match
    OUTPUT_AREA_ALMOST_FULL(PrinterStatus.TONER_LOW), // "output-area-almost-full"
    OUTPUT_AREA_FULL(PrinterStatus.OUTPUT_BIN_FULL), // "output-area-full"
    MARKER_SUPPLY_LOW(PrinterStatus.TONER_LOW), // "marker-supply-low"
    MARKER_SUPPLY_EMPTY(PrinterStatus.NO_TONER), // "marker-supply-empty"
    // not a great match
    MARKER_WASTE_ALMOST_FULL(PrinterStatus.TONER_LOW), // "marker-waste-almost-full"
    MARKER_WASTE_FULL(PrinterStatus.NO_TONER), // "marker-waste-full"
    FUSER_OVER_TEMP(PrinterStatus.WARMING_UP), // "fuser-over-temp"
    FUSER_UNDER_TEMP(PrinterStatus.WARMING_UP), // "fuser-under-temp"
    // not a great match
    OPC_NEAR_EOL(PrinterStatus.TONER_LOW), // "opc-near-eol"
    OPC_LIFE_OVER(PrinterStatus.NO_TONER), // "opc-life-over"
    DEVELOPER_LOW(PrinterStatus.TONER_LOW), // "developer-low"
    DEVELOPER_EMPTY(PrinterStatus.NO_TONER), // "developer-empty"
    INTERPRETER_RESOURCE_UNAVAILABLE(PrinterStatus.SERVER_UNKNOWN), // "interpreter-resource-unavailable"
    // CUPS defined states
    OFFLINE(PrinterStatus.OFFLINE), // "offline"
    CUPS_INSECURE_FILTER(PrinterStatus.SERVER_UNKNOWN), // "cups-insecure-filter"
    CUPS_MISSING_FILTER(PrinterStatus.ERROR), // "cups-missing-filter"
    CUPS_WAITING_FOR_JOB_COMPLETED(PrinterStatus.PRINTING); // "cups-waiting-for-job-completed");

    public static SortedMap<String, PrinterStatus> codeLookupTable;

    private PrinterStatus parent;
    CupsPrinterStatusMap(PrinterStatus parent) {
        this.parent = parent;
    }

    public static PrinterStatus match(String code) {
        // Initialize a sorted map to speed up lookups
        if(codeLookupTable == null) {
            codeLookupTable = new TreeMap<>();
            for(CupsPrinterStatusMap value : values()) {
                // Map "TONER_LOW" to "toner-low", etc
                codeLookupTable.put(value.name().toLowerCase(Locale.ENGLISH).replace('_', '-'), value.parent);
            }
        }

        return codeLookupTable.get(code);
    }
}
