package qz.printer.status;

import org.apache.log4j.Level;
import qz.utils.ByteUtilities;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by tresf on 12/10/2020
 */
public enum JobStatusType {
    EMPTY("", Level.INFO, -0x00000001), // Fallback for a no-status message
    PAUSED("PAUSED", Level.WARN, 0x00000001), // Job is paused
    ERROR("ERROR", Level.ERROR, 0x00000002), //	An error is associated with the job
    DELETING("DELETING", Level.WARN,0x00000004), //	Job is being deleted
    SPOOLING("SPOOLING", Level.INFO, 0x00000008),	// Job is spooling
    PRINTING("PRINTING", Level.INFO, 0x00000010), // Job is printing
    OFFLINE("OFFLINE", Level.ERROR, 0x00000020), //	Job is printing
    PAPEROUT("PAPEROUT", Level.WARN, 0x00000040), // Printer is out of paper
    RETAINED("RETAINED", Level.INFO, 0x00002000), // Windows Vista and later: Job has been retained in the print queue and cannot be deleted
    PRINTED("PRINTED", Level.INFO, 0x00000080), // Job has printed
    DELETED("DELETED", Level.WARN, 0x00000100), // Job has been deleted
    BLOCKED_DEVQ("BLOCKED", Level.ERROR,0x00000200), //	The driver cannot print the job
    USER_INTERVENTION("INTERVENTION", Level.WARN, 0x40000000),	// Printer has an error that requires the user to do something
    RESTART("RESTART", Level.WARN, 0x00000800), //	Job has been restarted
    COMPLETE("COMPLETE", Level.INFO, 0x00001000), // Windows XP and later: Job is sent to the printer, but the job may not be printed yet
    RENDERING_LOCALLY("RENDERING", Level.INFO,  0x00004000); // Job rendering locally on the client

    public static final SortedMap<Integer,JobStatusType> codeLookupTable = Collections.unmodifiableSortedMap(initCodeMap());
    public static final SortedMap<String,JobStatusType> cupsLookupTable = Collections.unmodifiableSortedMap(initCupsMap());

    private final String statusName;
    private final Level severity;
    private final int code;

    JobStatusType(String statusName, Level severity, int code) {
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

    private static SortedMap<Integer,JobStatusType> initCodeMap() {
        SortedMap<Integer,JobStatusType> tempMap = new TreeMap<>();
        for(JobStatusType s : JobStatusType.values()) {
            tempMap.put(s.code, s);
        }
        return tempMap;
    }

    private static SortedMap<String,JobStatusType> initCupsMap() {
        SortedMap<String,JobStatusType> tempMap = new TreeMap<>();
        /** FIXME tempMap.put("other", UNKNOWN_STATUS);
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
         **/
        return tempMap;
    }

    public static JobStatusType match(int code) {
        for(JobStatusType value : values()) {
            if(value.getCode() == code) {
                return value;
            }
        }
        return EMPTY;
    }

    static JobStatusType[] unwind(int bitwiseCode) {
        int size = Integer.bitCount(bitwiseCode);
        JobStatusType[] matches = new JobStatusType[size];
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
