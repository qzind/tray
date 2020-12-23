package qz.printer.status.job;

import qz.printer.status.NativeStatus;

import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Tres on 12/23/2020
 */
public enum CupsJobStatusMap implements NativeStatus.NativeMap {
    FOO_BAR(NativeJobStatus.EMPTY); // "foo-bar" placeholder until @Vzor- figures these out

    private static SortedMap<String,NativeStatus> sortedLookupTable;

    private final NativeStatus parent;

    CupsJobStatusMap(NativeStatus parent) {
        this.parent = parent;
    }

    public static NativeStatus match(String code) {
        // Initialize a sorted map to speed up lookups
        if(sortedLookupTable == null) {
            sortedLookupTable = new TreeMap<>();
            for(CupsJobStatusMap value : values()) {
                sortedLookupTable.put(value.name().toLowerCase(Locale.ENGLISH).replace("_", "-"), value.parent);
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
        return name().toLowerCase(Locale.ENGLISH).replace("_", "-");
    }
}
