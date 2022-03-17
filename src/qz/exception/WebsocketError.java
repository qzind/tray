package qz.exception;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static qz.exception.WebsocketError.WebsocketErrorType.*;

public enum WebsocketError {
    CONNECTION_BLOCKED(WEBSOCKET_ERROR, "Connection blocked by client", 0),
    REQUEST_BLOCKED(WEBSOCKET_ERROR, "Request blocked", 1),
    MESSAGE_EMPTY(WEBSOCKET_ERROR, "Message is empty", 2),

    PRINTER_NOT_FOUND(PRINTING_ERROR, "Specified printer could not be found.", 10),
    PRINTER_LISTENER_NOT_FOUND(PRINTING_ERROR, "No printer listeners started for this client.", 11),

    SERIAL_PORT_NOT_OPEN(SERIALIO_ERROR, "Serial port [%s] must be opened first.", 20),
    SERIAL_PORT_NOT_OPEN2(SERIALIO_ERROR, "Serial port [%s] is not open.", 21), // FIXME: Redundant?

    SOCKET_NOT_OPEN(SOCKETIO_ERROR,"Socket [%s] is not open.", 30),

    HID_ALREADY_LISTENING(HIDIO_ERROR, "Already listening HID device events", 40),
    HID_NOT_LISTENING(HIDIO_ERROR, "Not already listening HID device events", 41),
    HID_FAILED_CLAIM(HIDIO_ERROR, "Failed to open connection to device", 42),
    HID_ALREADY_CLAIMED(HIDIO_ERROR, "USB Device [v:%s p:%s] is already claimed.", 43),

    FILEIO_EXCEPTION(FILEIO_ERROR, "FileIO exception occurred: %s: %s", 99999); // FIXME: Numbering


    private static final Logger log = LogManager.getLogger(WebsocketError.class);

    public enum WebsocketErrorType {
        GENERAL_ERROR,
        WEBSOCKET_ERROR,
        PRINTING_ERROR,
        FILEIO_ERROR,
        HIDIO_ERROR,
        USBIO_ERROR,
        SERIALIO_ERROR,
        SOCKETIO_ERROR;
    }

    private WebsocketErrorType type;
    private String description;

    WebsocketError(WebsocketErrorType type, String description, int id) {
        this.type = type;
        this.description = description;
    }

    public String format(String ... args) {
        try {
            return String.format(description, args);
        } catch(Throwable t) {
            log.error(t);
            return description;
        }
    }
}
