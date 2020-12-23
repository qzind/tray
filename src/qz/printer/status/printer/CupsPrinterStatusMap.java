package qz.printer.status.printer;

import qz.printer.status.NativeStatus;

import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

public enum CupsPrinterStatusMap implements NativeStatus.NativeMap {
    OTHER(NativePrinterStatus.UNKNOWN_STATUS), // "other"
    NONE(NativePrinterStatus.OK), // "none"
    MEDIA_NEEDED(NativePrinterStatus.PAPER_OUT), // "media-needed"
    MEDIA_JAM(NativePrinterStatus.PAPER_JAM), // "media-jam"
    MOVING_TO_PAUSED(NativePrinterStatus.OK), // "moving-to-paused"
    PAUSED(NativePrinterStatus.PAUSED), // "paused"
    SHUTDOWN(NativePrinterStatus.OFFLINE), // "shutdown"
    CONNECTING_TO_DEVICE(NativePrinterStatus.OK), // "connecting-to-device"
    TIMED_OUT(NativePrinterStatus.NOT_AVAILABLE), // "timed-out"
    STOPPING(NativePrinterStatus.OK), // "stopping"
    STOPPED_PARTLY(NativePrinterStatus.PAUSED), // "stopped-partly"
    TONER_LOW(NativePrinterStatus.TONER_LOW), // "toner-low"
    TONER_EMPTY(NativePrinterStatus.NO_TONER), // "toner-empty"
    SPOOL_AREA_FULL(NativePrinterStatus.OUT_OF_MEMORY), // "spool-area-full"
    COVER_OPEN(NativePrinterStatus.DOOR_OPEN), // "cover-open"
    INTERLOCK_OPEN(NativePrinterStatus.DOOR_OPEN), // "interlock-open"
    DOOR_OPEN(NativePrinterStatus.DOOR_OPEN), // "door-open"
    INPUT_TRAY_MISSING(NativePrinterStatus.PAPER_PROBLEM), // "input-tray-missing"
    MEDIA_LOW(NativePrinterStatus.PAPER_PROBLEM), // "media-low"
    MEDIA_EMPTY(NativePrinterStatus.PAPER_OUT), // "media-empty"
    OUTPUT_TRAY_MISSING(NativePrinterStatus.PAPER_PROBLEM), // "output-tray-missing"
    //not a great match
    OUTPUT_AREA_ALMOST_FULL(NativePrinterStatus.TONER_LOW), // "output-area-almost-full"
    OUTPUT_AREA_FULL(NativePrinterStatus.OUTPUT_BIN_FULL), // "output-area-full"
    MARKER_SUPPLY_LOW(NativePrinterStatus.TONER_LOW), // "marker-supply-low"
    MARKER_SUPPLY_EMPTY(NativePrinterStatus.NO_TONER), // "marker-supply-empty"
    // not a great match
    MARKER_WASTE_ALMOST_FULL(NativePrinterStatus.TONER_LOW), // "marker-waste-almost-full"
    MARKER_WASTE_FULL(NativePrinterStatus.NO_TONER), // "marker-waste-full"
    FUSER_OVER_TEMP(NativePrinterStatus.WARMING_UP), // "fuser-over-temp"
    FUSER_UNDER_TEMP(NativePrinterStatus.WARMING_UP), // "fuser-under-temp"
    // not a great match
    OPC_NEAR_EOL(NativePrinterStatus.TONER_LOW), // "opc-near-eol"
    OPC_LIFE_OVER(NativePrinterStatus.NO_TONER), // "opc-life-over"
    DEVELOPER_LOW(NativePrinterStatus.TONER_LOW), // "developer-low"
    DEVELOPER_EMPTY(NativePrinterStatus.NO_TONER), // "developer-empty"
    INTERPRETER_RESOURCE_UNAVAILABLE(NativePrinterStatus.SERVER_UNKNOWN), // "interpreter-resource-unavailable"
    // CUPS defined states
    OFFLINE(NativePrinterStatus.OFFLINE), // "offline"
    CUPS_INSECURE_FILTER(NativePrinterStatus.SERVER_UNKNOWN), // "cups-insecure-filter"
    CUPS_MISSING_FILTER(NativePrinterStatus.ERROR), // "cups-missing-filter"
    CUPS_WAITING_FOR_JOB_COMPLETED(NativePrinterStatus.PRINTING); // "cups-waiting-for-job-completed");

    public static SortedMap<String,NativePrinterStatus> codeLookupTable;

    private NativePrinterStatus parent;
    CupsPrinterStatusMap(NativePrinterStatus parent) {
        this.parent = parent;
    }

    public static NativePrinterStatus match(String code) {
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

    @Override
    public NativeStatus getParent() {
        return parent;
    }

    @Override
    public Object getRawCode() {
        return name().toLowerCase(Locale.ENGLISH).replace("_", "-");
    }
}
