package qz.printer.status.job;

import qz.printer.status.Mappable;
import qz.printer.status.Statusable;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by tresf on 12/10/2020
 */
public enum WmiJobStatusMap implements Mappable {
    EMPTY(JobStatus.EMPTY, -0x00000001), // Fallback for a no-status message
    PAUSED(JobStatus.PAUSED, 0x00000001), // Job is paused
    ERROR(JobStatus.ERROR, 0x00000002), //	An error is associated with the job
    DELETING(JobStatus.DELETING, 0x00000004), //	Job is being deleted
    SPOOLING(JobStatus.SPOOLING, 0x00000008),	// Job is spooling
    PRINTING(JobStatus.PRINTING, 0x00000010), // Job is printing
    OFFLINE(JobStatus.OFFLINE, 0x00000020), //	Job is printing
    PAPEROUT(JobStatus.PAPEROUT, 0x00000040), // Printer is out of paper
    RETAINED(JobStatus.RETAINED, 0x00002000), // Windows Vista and later: Job has been retained in the print queue and cannot be deleted
    PRINTED(JobStatus.PRINTED, 0x00000080), // Job has printed
    DELETED(JobStatus.DELETED, 0x00000100), // Job has been deleted
    BLOCKED_DEVQ(JobStatus.BLOCKED_DEVQ,0x00000200), //	The driver cannot print the job
    USER_INTERVENTION(JobStatus.USER_INTERVENTION, 0x40000000),	// Printer has an error that requires the user to do something
    RESTART(JobStatus.RESTART, 0x00000800), //	Job has been restarted
    COMPLETE(JobStatus.COMPLETE, 0x00001000), // Windows XP and later: Job is sent to the printer, but the job may not be printed yet
    RENDERING_LOCALLY(JobStatus.RENDERING_LOCALLY, 0x00004000); // Job rendering locally on the client

    private static SortedMap<Integer, Statusable.Codeable> sortedLookupTable;

    private final Statusable.Codeable parent;
    private final int code;

    WmiJobStatusMap(Statusable.Codeable parent, int code) {
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
            for(WmiJobStatusMap value : values()) {
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
