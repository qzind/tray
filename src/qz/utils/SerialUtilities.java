package qz.utils;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.communication.SerialIO;
import qz.communication.SerialOptions;
import qz.ws.PrintSocketClient;
import qz.ws.SocketConnection;
import qz.ws.StreamEvent;

import java.util.Arrays;
import java.util.List;

/**
 * @author Tres
 */
public class SerialUtilities {

    private static final Logger log = LoggerFactory.getLogger(SerialUtilities.class);

    private static final List<Integer> VALID_BAUD = Arrays.asList(SerialPort.BAUDRATE_110, SerialPort.BAUDRATE_300,
                                                                  SerialPort.BAUDRATE_600, SerialPort.BAUDRATE_1200,
                                                                  SerialPort.BAUDRATE_4800, SerialPort.BAUDRATE_9600,
                                                                  SerialPort.BAUDRATE_14400, SerialPort.BAUDRATE_19200,
                                                                  SerialPort.BAUDRATE_38400, SerialPort.BAUDRATE_57600,
                                                                  SerialPort.BAUDRATE_115200, SerialPort.BAUDRATE_128000,
                                                                  SerialPort.BAUDRATE_256000);

    public enum SerialDataType {
        PLAIN, FILE
    }


    /**
     * @return Array of serial ports available on the attached system.
     */
    public static String[] getSerialPorts() {
        return SerialPortList.getPortNames();
    }

    /**
     * @return JSON array of {@code getSerialPorts()} result.
     */
    public static JSONArray getSerialPortsJSON() {
        String[] ports = getSerialPorts();
        JSONArray portJSON = new JSONArray();

        for(String name : ports) {
            portJSON.put(name);
        }

        return portJSON;
    }

    public static SerialDataType getDataTypeJSON(JSONObject data) {
        if (data != null && !data.isNull("type")) {
            try {
                return SerialDataType.valueOf(data.getString("type"));
            }
            catch(JSONException e) {
                log.warn("Cannot read {} as a value for data type, using default", data.opt("type"));
            }
        }

        return SerialDataType.PLAIN;
    }


    /**
     * Turn a string into a character byte array.
     * First attempting to take the entire string as a character literal (for non-printable unicode).
     */
    public static byte[] characterBytes(String convert) {
        if (convert.length() > 2) {
            try {
                //try to interpret entire string as single char representation (such as "\u0000" or "0xFFFF")
                char literal = (char)Integer.parseInt(convert.substring(2), 16);
                return StringUtils.getBytesUtf8(String.valueOf(literal));
            }
            catch(NumberFormatException ignore) {}
        }

        //try escaping string using Apache (to get strings like "\r" as characters)
        return StringEscapeUtils.unescapeJava(convert).getBytes();
    }


    /**
     * Parses the SerialPort's {@code DATABITS_x} value that corresponds with the provided {@code data}.
     *
     * @param data Data bits value to parse
     * @return The passed data bits value as a {@code SerialPort} constant value if valid, or -1 if invalid;
     */
    public static int parseDataBits(String data) {
        data = data.trim();

        switch(data) {
            case "0":
                log.trace("Parsed serial setting: 0 (Auto)");
                return 0;
            case "5":
                log.trace("Parsed serial setting: DATABITS_5");
                return SerialPort.DATABITS_5;
            case "6":
                log.trace("Parsed serial setting: DATABITS_6");
                return SerialPort.DATABITS_6;
            case "7":
                log.trace("Parsed serial setting: DATABITS_7");
                return SerialPort.DATABITS_7;
            case "8":
                log.trace("Parsed serial setting: DATABITS_8");
                return SerialPort.DATABITS_8;
            default:
                log.error("Data bits value of {} not supported", data);
                return -1;
        }
    }

    /**
     * Parses the SerialPort's {@code STOPBITS_x} value that corresponds with the provided {@code stop}.
     *
     * @param stop Stop bits value to parse
     * @return The passed stop bits value as a {@code SerialPort} constant value if valid, or -1 if invalid;
     */
    public static int parseStopBits(String stop) {
        stop = stop.trim();

        switch(stop) {
            case "0":
                log.trace("Parsed serial setting: 0 (Auto)");
                return 0;
            case "":
            case "1":
                log.trace("Parsed serial setting: STOPBITS_1");
                return SerialPort.STOPBITS_1;
            case "2":
                log.trace("Parsed serial setting: STOPBITS_2");
                return SerialPort.STOPBITS_2;
            case "1.5":
            case "1_5":
                log.trace("Parsed serial setting: STOPBITS_1_5");
                return SerialPort.STOPBITS_1_5;
            default:
                log.error("Stop bits value of {} could not be parsed", stop);
                return -1;
        }
    }

    /**
     * Parses the SerialPort's {@code FLOWCONTROL_x} value that corresponds with the provided {@code control}.
     *
     * @param control Flow control value to parse
     * @return The passed flow control value as a {@code SerialPort} constant value if valid, or -1 if invalid;
     */
    public static int parseFlowControl(String control) {
        control = control.trim().toLowerCase();

        switch(control) {
            case "auto":
                log.trace("Parsed serial setting: Auto");
                return 0;
            case "":
            case "n":
            case "none":
                log.trace("Parsed serial setting: FLOWCONTROL_NONE");
                return SerialPort.FLOWCONTROL_NONE;
            case "xonxoff_in":
                log.trace("Parsed serial setting: FLOWCONTROL_XONXOFF_IN");
                return SerialPort.FLOWCONTROL_XONXOFF_IN;
            case "xonxoff_out":
                log.trace("Parsed serial setting: FLOWCONTROL_XONXOFF_OUT");
                return SerialPort.FLOWCONTROL_XONXOFF_OUT;
            case "x":
            case "xonxoff":
                log.trace("Parsed serial setting: FLOWCONTROL_XONXOFF_INOUT");
                return SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT;
            case "rtscts_in":
                log.trace("Parsed serial setting: FLOWCONTROL_RTSCTS_IN");
                return SerialPort.FLOWCONTROL_RTSCTS_IN;
            case "rtscts_out":
                log.trace("Parsed serial setting: FLOWCONTROL_RTSCTS_OUT");
                return SerialPort.FLOWCONTROL_RTSCTS_OUT;
            case "p":
            case "rtscts":
                log.trace("Parsed serial setting: FLOWCONTROL_RTSCTS_INOUT");
                return SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT;
            default:
                log.error("Flow control value of {} could not be parsed", control);
                return -1;
        }
    }

    /**
     * Parses the SerialPort's {@code PARITY_x} value that corresponds with the provided {@code parity}.
     *
     * @param parity Parity value to parse
     * @return The passed parity value as a {@code SerialPort} constant value if valid, or -1 if invalid.
     */
    public static int parseParity(String parity) {
        parity = parity.trim().toLowerCase();

        switch(parity) {
            case "auto":
                log.trace("Parsed serial setting: Auto");
                return 0;
            case "":
            case "n":
            case "none":
                log.trace("Parsed serial setting: PARITY_NONE");
                return SerialPort.PARITY_NONE;
            case "e":
            case "even":
                log.trace("Parsed serial setting: PARITY_EVEN");
                return SerialPort.PARITY_EVEN;
            case "o":
            case "odd":
                log.trace("Parsed serial setting: PARITY_ODD");
                return SerialPort.PARITY_ODD;
            case "m":
            case "mark":
                log.trace("Parsed serial setting: PARITY_MARK");
                return SerialPort.PARITY_MARK;
            case "s":
            case "space":
                log.trace("Parsed serial setting: PARITY_SPACE");
                return SerialPort.PARITY_SPACE;
            default:
                log.error("Parity value of {} not supported", parity);
                return -1;
        }
    }

    /**
     * Parses the SerialPort's {@code BAUDRATE_x} value that corresponds with the provided {@code rate}.
     *
     * @param rate Baud rate to parse
     * @return The passed baud rate as a {@code SerialPort} constant value if valid, or -1 if invalid.
     */
    public static int parseBaudRate(String rate) {
        int baud = -1;
        try { baud = Integer.decode(rate.trim()); } catch(NumberFormatException ignore) {}

        if (baud == 0) {
            log.trace("Parsed serial setting: 0 (Auto)");
        } else if (VALID_BAUD.contains(baud)) {
            log.trace("Parsed serial setting: BAUDRATE_{}", baud);
        } else {
            log.error("Baud rate of {} not supported", rate);
            baud = -1;
        }

        return baud;
    }


    public static void setupSerialPort(final Session session, String UID, SocketConnection connection, JSONObject params) throws JSONException {
        final String portName = params.getString("port");
        if (connection.getSerialPort(portName) != null) {
            PrintSocketClient.sendError(session, UID, String.format("Serial port [%s] is already open.", portName));
            return;
        }

        try {
            SerialOptions props = new SerialOptions(params.optJSONObject("options"), true);
            final SerialIO serial = new SerialIO(portName);

            if (serial.open(props)) {
                connection.addSerialPort(portName, serial);

                //apply listener here, so we can send all replies to the browser
                serial.applyPortListener(spe -> {
                    String output = serial.processSerialEvent(spe);

                    if (output != null) {
                        log.debug("Received serial output: {}", output);
                        StreamEvent event = new StreamEvent(StreamEvent.Stream.SERIAL, StreamEvent.Type.RECEIVE)
                                .withData("portName", portName).withData("output", output);
                        PrintSocketClient.sendStream(session, event);
                    }
                });

                PrintSocketClient.sendResult(session, UID, null);
            } else {
                PrintSocketClient.sendError(session, UID, String.format("Unable to open serial port [%s]", portName));
            }
        }
        catch(SerialPortException e) {
            PrintSocketClient.sendError(session, UID, e);
        }
    }

}
