package qz.printer.status.printer;

import org.apache.log4j.Level;
import qz.printer.status.NativeStatus;

import static org.apache.log4j.Level.*;

/**
 * Created by kyle on 7/7/17.
 */
public enum NativePrinterStatus implements NativeStatus {
    OK(INFO),
    PAUSED(WARN),
    ERROR(FATAL),
    PENDING_DELETION(WARN),
    PAPER_JAM(FATAL),
    PAPER_OUT(FATAL),
    MANUAL_FEED(INFO),
    PAPER_PROBLEM(FATAL),
    OFFLINE(FATAL),
    IO_ACTIVE(INFO),
    BUSY(INFO),
    PRINTING(INFO),
    OUTPUT_BIN_FULL(FATAL),
    NOT_AVAILABLE(FATAL),
    WAITING(INFO),
    PROCESSING(INFO),
    INITIALIZING(INFO),
    WARMING_UP(INFO),
    TONER_LOW(WARN),
    NO_TONER(FATAL),
    PAGE_PUNT(FATAL),
    USER_INTERVENTION(WARN),
    OUT_OF_MEMORY(FATAL),
    DOOR_OPEN(WARN),
    SERVER_UNKNOWN(WARN),
    POWER_SAVE(INFO),
    UNKNOWN_STATUS(FATAL);

    private Level level;

    NativePrinterStatus(Level level) {
        this.level = level;
    }

    @Override
    public NativeStatus getDefault() {
        return UNKNOWN_STATUS;
    }

    @Override
    public Level getLevel() {
        return level;
    }
}
