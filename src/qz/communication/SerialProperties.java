package qz.communication;

import jssc.SerialPort;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.SerialUtilities;

public class SerialProperties {

    private static final Logger log = LoggerFactory.getLogger(SerialProperties.class);

    // Serial port attributes obtained from the system
    private int baudRate = SerialPort.BAUDRATE_9600;
    private int dataBits = SerialPort.DATABITS_8;
    private int stopBits = SerialPort.STOPBITS_1;
    private int parity = SerialPort.PARITY_NONE;
    private int flowControl = SerialPort.FLOWCONTROL_NONE;


    /**
     * Parses the provided JSON object into relevant SerialPort constants
     */
    public SerialProperties(JSONObject serialProps) {
        if (serialProps == null) { return; }

        if (!serialProps.isNull("baudRate")) {
            try { baudRate = SerialUtilities.parseBaudRate(serialProps.getString("baudRate")); }
            catch(JSONException e) { log.warn("Cannot read {} as a value for baud rate, using default", serialProps.opt("baudRate")); }
        }

        if (!serialProps.isNull("dataBits")) {
            try { dataBits = SerialUtilities.parseDataBits(serialProps.getString("dataBits")); }
            catch(JSONException e) { log.warn("Cannot read {} as a value for data bits, using default", serialProps.opt("dataBits")); }
        }

        if (!serialProps.isNull("stopBits")) {
            try { stopBits = SerialUtilities.parseStopBits(serialProps.getString("stopBits")); }
            catch(JSONException e) { log.warn("Cannot read {} as a value for stop bits, using default", serialProps.opt("stopBits")); }
        }

        if (!serialProps.isNull("parity")) {
            try { parity = SerialUtilities.parseParity(serialProps.getString("parity")); }
            catch(JSONException e) { log.warn("Cannot read {} as a value for parity, using default", serialProps.opt("parity")); }
        }

        if (!serialProps.isNull("flowControl")) {
            try { flowControl = SerialUtilities.parseFlowControl(serialProps.getString("flowControl")); }
            catch(JSONException e) { log.warn("Cannot read {} as a value for flow control, using default", serialProps.opt("flowControl")); }
        }
    }


    public int getBaudRate() {
        return baudRate;
    }

    public int getDataBits() {
        return dataBits;
    }

    public int getStopBits() {
        return stopBits;
    }

    public int getParity() {
        return parity;
    }

    public int getFlowControl() {
        return flowControl;
    }

}
