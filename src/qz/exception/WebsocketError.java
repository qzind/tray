package qz.exception;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ws.SocketMethod;

import java.util.ArrayList;
import java.util.Arrays;

import static qz.exception.WebsocketError.WebsocketErrorType.*;

public enum WebsocketError {
    // Websocket
    CONNECTION_BLOCKED(WEBSOCKET_ERROR, "Connection blocked by client", 0),
    REQUEST_BLOCKED(WEBSOCKET_ERROR, "Request blocked", 1),
    MESSAGE_EMPTY(WEBSOCKET_ERROR, "Message is empty", 2),

    // Printing
    PRINTER_NOT_SPECIFIED(PRINTING_ERROR, "A printer must be specified before printing", 10),
    PRINTER_NOT_FOUND(PRINTING_ERROR, "Specified printer could not be found.", 11),
    PRINTER_NOT_LISTENING(PRINTING_ERROR, "No printer listeners started for this client.", 14),
    PRINTING_FAILED(PRINTING_ERROR, "Failed to print", 17),
    PRINTING_CANCELLED(PRINTING_ERROR, "Printing cancelled", 18),

    // Serial
    SERIAL_PORT_NOT_OPEN(SERIAL_ERROR, "Serial port [%s] is not open.", 20),
    SERIAL_PORT_NOT_FOUND(SERIAL_ERROR, "Serial port [%s] not found", 21),
    SERIAL_PORT_OPEN_FAILED(SERIAL_ERROR, "Unable to open serial port [%s]", 22),
    SERIAL_PORT_ALREADY_OPEN(SERIAL_ERROR, "Serial port [%s] is already open.", 23),


    // Socket
    SOCKET_NOT_OPEN(SOCKET_ERROR, "Socket [%s] is not open.", 30),
    SOCKET_OPEN_FAILED(SOCKET_ERROR, "Unable to open socket [%s]", 32),
    SOCKET_ALREADY_OPEN(SOCKET_ERROR, "Socket [%s] is already open", 33),

    // USB/HID (The word "HID" or "USB" will be determined by the <code>SocketMethod</code>)
    USB_NOT_CLAIMED(USB_ERROR, "USB Device [v:%s p:%s] is not claimed.", 40),
    USB_CLAIM_FAILED(USB_ERROR, "Failed to open connection to %s device", 42),
    USB_ALREADY_CLAIMED(USB_ERROR, "%s Device [v:%s p:%s] is already claimed", 43),
    USB_ALREADY_LISTENING(USB_ERROR, "Already listening %s device events", 44),
    USB_NOT_LISTENING(USB_ERROR, "Not already listening %s device events", 45),
    USB_NOT_STREAMING(USB_ERROR, "USB Device [v:%s p:%s] is not streaming data.", 46),
    USB_ALREADY_STREAMING(USB_ERROR, "USB Device [v:%s p:%s] is already streaming data", 47),

    // FileIO
    FILE_PATH_NOT_EXIST(FILE_ERROR, "Path does not exist", 51),
    FILE_ALREADY_LISTENING(FILE_ERROR, "Already listening to path events", 54),
    FILE_NOT_LISTENING(FILE_ERROR, "Not already listening to path events", 55),
    FILE_PATH_NOT_READABLE(FILE_ERROR, "Path is not readable", 56),

    FILE_NOT_DIRECTORY(FILE_ERROR, "Path is not a directory", 58),
    FILE_EXCEPTION(FILE_ERROR, "FileIO exception occurred: %s: %s", 59),

    // Misc
    INVALID_FUNCTION_CALL(GENERAL_ERROR, "Invalid function call: %s", 998),
    UNHANDLED(GENERAL_ERROR, "An unhandled exception has occurred: %s", 999);

    private static final Logger log = LogManager.getLogger(WebsocketError.class);

    public enum WebsocketErrorType {
        GENERAL_ERROR,
        WEBSOCKET_ERROR,
        PRINTING_ERROR,
        FILE_ERROR,
        USB_ERROR,
        SERIAL_ERROR,
        SOCKET_ERROR;
    }

    private WebsocketErrorType type;
    private String description;
    private int id;

    WebsocketError(WebsocketErrorType type, String description, int id) {
        this.type = type;
        this.description = description;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /**
     * Prepends socket method prefix (e.g. "HID", "USB"), to list of arguments
     */
    public String format(SocketMethod method, String ... args) {
        ArrayList<String> argsList = new ArrayList<>(args == null ? 1 : args.length + 1);
        argsList.add(method == null ? "null" : method.name().split("_")[0]);
        if(args != null) {
            argsList.addAll(Arrays.asList(args));
        }
        return format(argsList.toArray(new String[argsList.size()]));
    }

    /**
     * Appends the exception message (or class name if missing) to the list of arguments
     */
    public String format(Throwable t, String ... args) {
        ArrayList<String> argsList = new ArrayList<>(args == null ? 1 : args.length + 1);
        if(args != null) {
            argsList.addAll(Arrays.asList(args));
        }
        // If empty, send class name; it's better than nothing
        String message = t == null ? "null" : t.getLocalizedMessage();
        argsList.add(message == null ? t.getClass().getSimpleName() : message);
        log.error(t);
        // FIXME: Why does this print "%s" in the logs/UI?
        return format(argsList.toArray(new String[argsList.size()]));
    }


    public String format(String ... args) {
        try {
            if(args != null) {
                return String.format(description, args);
            }
        } catch(Throwable t) {
            log.error(t);
        } finally {
            return description;
        }
    }
}
