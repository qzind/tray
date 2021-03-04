package qz.printer.status.job;

import qz.printer.status.NativeStatus;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by tresf on 12/10/2020
 */
public enum WmiJobStatusMap implements NativeStatus.NativeMap {
    EMPTY(NativeJobStatus.UNKNOWN, -0x00000001), // Fallback for a no-status message
    PAUSED(NativeJobStatus.PAUSED, 0x00000001), // Job is paused
    ERROR(NativeJobStatus.ERROR, 0x00000002), // An error is associated with the job
    DELETING(NativeJobStatus.DELETING, 0x00000004), // Job is being deleted
    SPOOLING(NativeJobStatus.SPOOLING, 0x00000008),	// Job is spooling
    PRINTING(NativeJobStatus.PRINTING, 0x00000010), // Job is printing
    OFFLINE(NativeJobStatus.OFFLINE, 0x00000020), // Job is printing
    PAPEROUT(NativeJobStatus.PAPEROUT, 0x00000040), // Printer is out of paper
    PRINTED(NativeJobStatus.COMPLETE, 0x00000080), // Job has printed
    DELETED(NativeJobStatus.DELETED, 0x00000100), // Job has been deleted
    BLOCKED_DEVQ(NativeJobStatus.ABORTED, 0x00000200), // The driver cannot print the job
    RESTART(NativeJobStatus.RESTART, 0x00000800), // Job has been restarted
    COMPLETE(NativeJobStatus.SENT, 0x00001000), // Windows XP and later: Job is sent to the printer, but the job may not be printed yet
    RETAINED(NativeJobStatus.RETAINED, 0x00002000), // Windows Vista and later: Job has been retained in the print queue and cannot be deleted
    RENDERING_LOCALLY(NativeJobStatus.RENDERING_LOCALLY, 0x00004000), // Job rendering locally on the client
    USER_INTERVENTION(NativeJobStatus.USER_INTERVENTION, 0x40000000); // Printer has an error that requires the user to do something

    private static SortedMap<Integer,NativeJobStatus> sortedLookupTable;

    private final NativeJobStatus parent;
    private final int rawCode;

    WmiJobStatusMap(NativeJobStatus parent, int rawCode) {
        this.parent = parent;
        this.rawCode = rawCode;
    }

    public static NativeJobStatus match(int code) {
        // Initialize a sorted map to speed up lookups
        if(sortedLookupTable == null) {
            sortedLookupTable = new TreeMap<>();
            for(WmiJobStatusMap value : values()) {
                sortedLookupTable.put(value.rawCode, value.parent);
            }
        }

        return sortedLookupTable.get(code);
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
