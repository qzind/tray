package qz.printer.status.job;

import org.apache.log4j.Level;
import qz.printer.status.NativeStatus;

/**
 * Created by kyle on 7/7/17.
 */
public enum NativeJobStatus implements NativeStatus {
    EMPTY(Level.INFO),
    PAUSED(Level.WARN),
    ERROR(Level.ERROR),
    DELETING(Level.WARN),
    SPOOLING(Level.INFO),
    PRINTING(Level.INFO),
    OFFLINE(Level.ERROR),
    PAPEROUT(Level.WARN),
    RETAINED(Level.INFO),
    PRINTED(Level.INFO),
    DELETED(Level.WARN),
    BLOCKED_DEVQ(Level.ERROR),
    USER_INTERVENTION(Level.WARN),
    RESTART(Level.WARN),
    COMPLETE(Level.INFO),
    RENDERING_LOCALLY(Level.INFO);

    private Level level;

    NativeJobStatus(Level level) {
        this.level = level;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public NativeStatus getDefault() {
        return EMPTY;
    }
}
