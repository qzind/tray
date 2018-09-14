package qz.printer.status;

import org.apache.log4j.Level;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by kyle on 5/18/17.
 */
public enum PrinterStatusType {

    OK("OK", Level.INFO, 0x00000000),
    PAUSED("PAUSED", Level.WARN, 0x00000001),
    ERROR("ERROR", Level.FATAL, 0x00000002),
    PENDING_DELETION("PENDING_DELETION", Level.WARN, 0x00000004),
    PAPER_JAM("PAPER_JAM", Level.FATAL, 0x00000008),
    PAPER_OUT("PAPER_OUT", Level.FATAL, 0x00000010),
    MANUAL_FEED("MANUAL_FEED", Level.INFO, 0x00000020),
    PAPER_PROBLEM("PAPER_PROBLEM", Level.FATAL, 0x00000040),
    OFFLINE("OFFLINE", Level.FATAL, 0x00000080),
    IO_ACTIVE("IO_ACTIVE", Level.INFO, 0x00000100),
    BUSY("BUSY", Level.INFO, 0x00000200),
    PRINTING("PRINTING", Level.INFO, 0x00000400),
    OUTPUT_BIN_FULL("OUTPUT_BIN_FULL", Level.FATAL, 0x00000800),
    NOT_AVAILABLE("NOT_AVAILABLE", Level.FATAL, 0x00001000),
    WAITING("WAITING", Level.INFO, 0x00002000),
    PROCESSING("PROCESSING", Level.INFO, 0x00004000),
    INITIALIZING("INITIALIZING", Level.INFO, 0x00008000),
    WARMING_UP("WARMING_UP", Level.INFO, 0x00010000),
    TONER_LOW("TONER_LOW", Level.WARN, 0x00020000),
    NO_TONER("NO_TONER", Level.FATAL, 0x00040000),
    PAGE_PUNT("PAGE_PUNT", Level.FATAL, 0x00080000),
    USER_INTERVENTION("USER_INTERVENTION", Level.WARN, 0x00100000),
    OUT_OF_MEMORY("OUT_OF_MEMORY", Level.FATAL, 0x00200000),
    DOOR_OPEN("DOOR_OPEN", Level.WARN, 0x00400000),
    SERVER_UNKNOWN("SERVER_UNKNOWN", Level.WARN, 0x00800000),
    POWER_SAVE("POWER_SAVE", Level.INFO, 0x01000000),
    UNKNOWN_STATUS("UNKNOWN_STATUS", Level.FATAL, 0x02000000);

    public static final SortedMap<Integer,PrinterStatusType> codeLookupTable = Collections.unmodifiableSortedMap(initCodeMap());
    public static final SortedMap<String,PrinterStatusType> cupsLookupTable = Collections.unmodifiableSortedMap(initCupsMap());

    private final String statusName;
    private final Level severity;
    private final int code;

    PrinterStatusType(String statusName, Level severity, int code) {
        this.statusName = statusName;
        this.severity = severity;
        this.code = code;
    }

    public String getName() {
        return statusName;
    }

    public Level getSeverity() {
        return severity;
    }

    public int getCode() {
        return code;
    }

    private static SortedMap<Integer,PrinterStatusType> initCodeMap() {
        SortedMap<Integer,PrinterStatusType> tempMap = new TreeMap<>();
        for(PrinterStatusType s : PrinterStatusType.values()) {
            tempMap.put(s.code, s);
        }
        return tempMap;
    }

    private static SortedMap<String,PrinterStatusType> initCupsMap() {
        SortedMap<String,PrinterStatusType> tempMap = new TreeMap<>();
        tempMap.put("other", UNKNOWN_STATUS);
        tempMap.put("none", OK);
        tempMap.put("media-needed", PAPER_OUT);
        tempMap.put("media-jam", PAPER_JAM);
        tempMap.put("moving-to-paused", OK);
        tempMap.put("paused", PAUSED);
        tempMap.put("shutdown", OFFLINE);
        tempMap.put("connecting-to-device", OK);
        tempMap.put("timed-out", NOT_AVAILABLE);
        tempMap.put("stopping", OK);
        tempMap.put("stopped-partly", PAUSED);
        tempMap.put("toner-low", TONER_LOW);
        tempMap.put("toner-empty", NO_TONER);
        tempMap.put("spool-area-full", OUT_OF_MEMORY);
        tempMap.put("cover-open", DOOR_OPEN);
        tempMap.put("interlock-open", DOOR_OPEN);
        tempMap.put("door-open", DOOR_OPEN);
        tempMap.put("input-tray-missing", PAPER_PROBLEM);
        tempMap.put("media-low", PAPER_PROBLEM);
        tempMap.put("media-empty", PAPER_OUT);
        tempMap.put("output-tray-missing", PAPER_PROBLEM);
        //not a great match
        tempMap.put("output-area-almost-full", TONER_LOW);
        tempMap.put("output-area-full", OUTPUT_BIN_FULL);
        tempMap.put("marker-supply-low", TONER_LOW);
        tempMap.put("marker-supply-empty", NO_TONER);
        //not a great match
        tempMap.put("marker-waste-almost-full", TONER_LOW);
        tempMap.put("marker-waste-full", NO_TONER);
        tempMap.put("fuser-over-temp", WARMING_UP);
        tempMap.put("fuser-under-temp", WARMING_UP);
        //not a great match
        tempMap.put("opc-near-eol", TONER_LOW);
        tempMap.put("opc-life-over", NO_TONER);
        tempMap.put("developer-low", TONER_LOW);
        tempMap.put("developer-empty", NO_TONER);
        tempMap.put("interpreter-resource-unavailable", SERVER_UNKNOWN);
        //CUPS DEFINED STATES
        tempMap.put("offline", OFFLINE);
        tempMap.put("cups-insecure-filter", SERVER_UNKNOWN);
        tempMap.put("cups-missing-filter", ERROR);
        tempMap.put("cups-waiting-for-job-completed", PRINTING);
        return tempMap;
    }
}
