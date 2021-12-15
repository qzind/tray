package qz.printer.status.job;

import org.apache.logging.log4j.Level;
import qz.printer.status.NativeStatus;

/**
 * Created by kyle on 7/7/17.
 */
public enum NativeJobStatus implements NativeStatus {
    ABORTED(Level.ERROR),
    CANCELED(Level.WARN),
    COMPLETE(Level.INFO),
    DELETED(Level.WARN),
    DELETING(Level.WARN),
    ERROR(Level.ERROR),
    OFFLINE(Level.ERROR),
    PRINTING(Level.INFO),
    SPOOLING(Level.INFO),
    SCHEDULED(Level.INFO),
    PAPEROUT(Level.WARN),
    RETAINED(Level.INFO),
    PAUSED(Level.WARN),
    SENT(Level.INFO),
    RESTART(Level.WARN),
    RENDERING_LOCALLY(Level.INFO),
    USER_INTERVENTION(Level.WARN),
    UNMAPPED(Level.FATAL), // should never make it to the user
    UNKNOWN(Level.INFO);

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
        return UNKNOWN;
    }
}
