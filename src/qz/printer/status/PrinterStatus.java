package qz.printer.status;

import org.apache.log4j.Level;
import qz.ws.StreamEvent;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
* Created by kyle on 5/18/17.
*/
public enum PrinterStatus {

    OK("OK", Level.INFO, 0),
    PAUSED("PAUSED", Level.WARN, 1<<0),
    ERROR("ERROR", Level.FATAL, 1<<1),
    PENDING_DELETION("PENDING_DELETION", Level.WARN, 1<<2),
    PAPER_JAM("PAPER_JAM", Level.FATAL, 1<<3),
    PAPER_OUT("PAPER_OUT", Level.FATAL, 1<<4),
    MANUAL_FEED("MANUAL_FEED", Level.INFO, 1<<5),
    PAPER_PROBLEM("PAPER_PROBLEM", Level.FATAL, 1<<6),
    OFFLINE("OFFLINE", Level.FATAL, 1<<7),
    IO_ACTIVE("IO_ACTIVE", Level.INFO, 1<<8),
    BUSY("BUSY", Level.INFO, 1<<9),
    PRINTING("PRINTING", Level.INFO, 1<<10),
    OUTPUT_BIN_FULL("OUTPUT_BIN_FULL", Level.FATAL, 1<<11),
    NOT_AVAILABLE("NOT_AVAILABLE", Level.FATAL, 1<<12),
    WAITING("WAITING", Level.INFO, 1<<13),
    PROCESSING("PROCESSING", Level.INFO, 1<<14),
    INITIALIZING("INITIALIZING", Level.INFO, 1<<15),
    WARMING_UP("WARMING_UP", Level.INFO, 1<<16),
    TONER_LOW("TONER_LOW", Level.WARN, 1<<17),
    NO_TONER("NO_TONER", Level.FATAL, 1<<18),
    PAGE_PUNT("PAGE_PUNT", Level.FATAL, 1<<19),
    USER_INTERVENTION("USER_INTERVENTION", Level.WARN, 1<<20),
    OUT_OF_MEMORY("OUT_OF_MEMORY", Level.FATAL, 1<<21),
    DOOR_OPEN("DOOR_OPEN", Level.WARN, 1<<22),
    SERVER_UNKNOWN("SERVER_UNKNOWN", Level.WARN, 1<<23),
    POWER_SAVE("POWER_SAVE", Level.INFO, 1<<24),
    UNKNOWN_STATUS("UNKNOWN_STATUS", Level.FATAL, 1<<25);

    private static final SortedMap<Integer, PrinterStatus> codeLookupTable = Collections.unmodifiableSortedMap(initCodeMap());
    private static final SortedMap<String, PrinterStatus> cupsLookupTable = Collections.unmodifiableSortedMap(initCupsMap());

    private final String statusName;
    private final Level severity;
    private final int code;

    PrinterStatus(String statusName, Level severity, int code) {
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

    private static SortedMap<Integer, PrinterStatus> initCodeMap() {
        SortedMap<Integer, PrinterStatus> tempMap = new TreeMap<>();
        for (PrinterStatus s : PrinterStatus.values()) {
            tempMap.put(s.code, s);
        }
        return tempMap;
    }
    private static SortedMap<String, PrinterStatus> initCupsMap() {
        SortedMap<String, PrinterStatus> tempMap = new TreeMap<>();
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
        tempMap.put("media-low", TONER_LOW);
        tempMap.put("media-empty", PAPER_OUT);
        tempMap.put("output-tray-missing", PAPER_PROBLEM);
        tempMap.put("output-area-almost-full", TONER_LOW);
        tempMap.put("output-area-full", OUTPUT_BIN_FULL);
        tempMap.put("marker-supply-low", TONER_LOW);
        tempMap.put("marker-supply-empty", NO_TONER);
        tempMap.put("marker-waste-almost-full", TONER_LOW);
        tempMap.put("marker-waste-full", NO_TONER);
        tempMap.put("fuser-over-temp", WARMING_UP);
        tempMap.put("fuser-under-temp", WARMING_UP);
        tempMap.put("opc-near-eol", TONER_LOW);
        tempMap.put("opc-life-over", NO_TONER);
        tempMap.put("developer-low", TONER_LOW);
        tempMap.put("developer-empty", NO_TONER);
        tempMap.put("interpreter-resource-unavailable", SERVER_UNKNOWN);
        return tempMap;
    }

    public static PrinterStatus[] getFromWMICode(int code) {
        if (code == 0) return new PrinterStatus[]{OK};

        int bitPopulation = Integer.bitCount(code);
        PrinterStatus[] statusArray = new PrinterStatus[bitPopulation];
        int mask = 1;

        while (bitPopulation > 0){
            if ((mask&code) > 0) statusArray[--bitPopulation] = codeLookupTable.get(mask);
            mask = mask<<1;
        }
        return statusArray;
    }

    public static PrinterStatus[] getFromCupsString(String reason) {
        //WIP
        return new PrinterStatus[]{OK};
    }
}