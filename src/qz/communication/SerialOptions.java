package qz.communication;

import jssc.SerialPort;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ByteUtilities;
import qz.utils.DeviceUtilities;
import qz.utils.LoggerUtilities;
import qz.utils.SerialUtilities;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;

public class SerialOptions {

    private static final Logger log = LogManager.getLogger(SerialOptions.class);

    private static final String DEFAULT_BEGIN = "0x0002";
    private static final String DEFAULT_END = "0x000D";

    private PortSettings portSettings = null;
    private ResponseFormat responseFormat = null;

    /**
     * Creates an empty/default options object
     */
    public SerialOptions() {
        portSettings = new PortSettings();
        responseFormat = new ResponseFormat();
    }

    /**
     * Parses the provided JSON object into relevant SerialPort constants
     */
    public SerialOptions(JSONObject serialOpts, boolean isOpening) {
        if (serialOpts == null) { return; }

        //only apply port settings if opening or explicitly set in a send data call
        if (isOpening || serialOpts.has("baudRate") || serialOpts.has("dataBits") || serialOpts.has("stopBits") || serialOpts.has("parity") || serialOpts.has("flowControl")) {
            portSettings = new PortSettings();

            if (!serialOpts.isNull("baudRate")) {
                try { portSettings.baudRate = SerialUtilities.parseBaudRate(serialOpts.getString("baudRate")); }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "string", "baudRate", serialOpts.opt("baudRate")); }
            }

            if (!serialOpts.isNull("dataBits")) {
                try { portSettings.dataBits = SerialUtilities.parseDataBits(serialOpts.getString("dataBits")); }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "string", "dataBits", serialOpts.opt("dataBits")); }
            }

            if (!serialOpts.isNull("stopBits")) {
                try { portSettings.stopBits = SerialUtilities.parseStopBits(serialOpts.getString("stopBits")); }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "string", "stopBits", serialOpts.opt("stopBits")); }
            }

            if (!serialOpts.isNull("parity")) {
                try { portSettings.parity = SerialUtilities.parseParity(serialOpts.getString("parity")); }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "string", "parity", serialOpts.opt("parity")); }
            }

            if (!serialOpts.isNull("flowControl")) {
                try { portSettings.flowControl = SerialUtilities.parseFlowControl(serialOpts.getString("flowControl")); }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "string", "flowControl", serialOpts.opt("flowControl")); }
            }

            if (!serialOpts.isNull("encoding") && !serialOpts.optString("encoding").isEmpty()) {
                try { portSettings.encoding = Charset.forName(serialOpts.getString("encoding")); }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "string", "encoding", serialOpts.opt("encoding")); }
            }
        }

        if (!serialOpts.isNull("rx")) {
            responseFormat = new ResponseFormat();
            //Make the response encoding default to the port encoding. If this is removed it will default to UTF-8
            responseFormat.encoding = portSettings.encoding;

            JSONObject respOpts = serialOpts.optJSONObject("rx");
            if (respOpts != null) {
                if (!respOpts.isNull("start")) {
                    try {
                        JSONArray startBits = respOpts.getJSONArray("start");
                        ArrayList<Byte> bytes = new ArrayList<>();
                        for(int i = 0; i < startBits.length(); i++) {
                            byte[] charByte = DeviceUtilities.characterBytes(startBits.getString(i), responseFormat.encoding);
                            for(byte b : charByte) { bytes.add(b); }
                        }
                        responseFormat.boundStart = ArrayUtils.toPrimitive(bytes.toArray(new Byte[0]));
                    }
                    catch(JSONException e) {
                        try { responseFormat.boundStart = DeviceUtilities.characterBytes(respOpts.getString("start"), responseFormat.encoding); }
                        catch(JSONException e2) { LoggerUtilities.optionWarn(log, "string", "start", respOpts.opt("start")); }
                    }
                }

                if (!respOpts.isNull("includeHeader")) {
                    try { responseFormat.includeStart = respOpts.getBoolean("includeHeader"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "boolean", "includeHeader", respOpts.opt("includeHeader")); }
                }

                if (!respOpts.isNull("end")) {
                    try { responseFormat.boundEnd = DeviceUtilities.characterBytes(respOpts.getString("end"), responseFormat.encoding); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "string", "end", respOpts.opt("end")); }

                    if (responseFormat.boundStart == null || responseFormat.boundStart.length == 0) {
                        log.warn("End bound set without start bound defined");
                    }
                }

                if (!respOpts.isNull("untilNewline")) {
                    try { responseFormat.boundNewline = respOpts.getBoolean("untilNewline"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "boolean", "untilNewline", respOpts.opt("untilNewline")); }
                }

                if (!respOpts.isNull("width")) {
                    try { responseFormat.fixedWidth = respOpts.getInt("width"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "width", respOpts.opt("width")); }
                }

                if (!respOpts.isNull("lengthBytes")) {
                    try {
                        JSONObject lengthOpts = respOpts.optJSONObject("lengthBytes");
                        responseFormat.length = new ByteParam();

                        if (lengthOpts != null) {
                            if (!lengthOpts.isNull("index")) {
                                try { responseFormat.length.index = lengthOpts.getInt("index"); }
                                catch(JSONException se) { LoggerUtilities.optionWarn(log, "integer", "lengthBytes.index", lengthOpts.opt("index")); }
                            }

                            if (!lengthOpts.isNull("length")) {
                                try { responseFormat.length.length = lengthOpts.getInt("length"); }
                                catch(JSONException se) { LoggerUtilities.optionWarn(log, "integer", "lengthBytes.length", lengthOpts.opt("length")); }
                            }

                            if (!lengthOpts.isNull("endian")) {
                                try { responseFormat.length.endian = ByteUtilities.Endian.valueOf(lengthOpts.getString("endian").toUpperCase(Locale.ENGLISH)); }
                                catch(JSONException se) { LoggerUtilities.optionWarn(log, "string", "lengthBytes.endian", lengthOpts.opt("endian")); }
                            }
                        } else {
                            responseFormat.length.index = respOpts.getInt("lengthBytes");
                        }
                    }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "lengthBytes", respOpts.opt("lengthBytes")); }

                    if (responseFormat.boundStart == null || responseFormat.boundStart.length == 0) {
                        log.warn("Length byte(s) defined without start bound defined");
                    }
                }

                if (!respOpts.isNull("crcBytes")) {
                    try {
                        JSONObject crcOpts = respOpts.optJSONObject("crcBytes");
                        responseFormat.crc = new ByteParam();

                        if (crcOpts != null) {
                            if (!crcOpts.isNull("index")) {
                                try { responseFormat.crc.index = crcOpts.getInt("index"); }
                                catch(JSONException se) { LoggerUtilities.optionWarn(log, "integer", "crcBytes.index", crcOpts.opt("index")); }
                            }

                            if (!crcOpts.isNull("length")) {
                                try { responseFormat.crc.length = crcOpts.getInt("length"); }
                                catch(JSONException se) { LoggerUtilities.optionWarn(log, "integer", "crcBytes.length", crcOpts.opt("length")); }
                            }
                        } else {
                            responseFormat.crc.length = respOpts.getInt("crcBytes");
                        }
                    }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "crcBytes", respOpts.opt("crcBytes")); }

                    if (responseFormat.boundStart == null || responseFormat.boundStart.length == 0) {
                        log.warn("CRC byte(s) defined without start bound defined");
                    }
                }

                if (!respOpts.isNull("encoding") && !respOpts.optString("encoding").isEmpty()) {
                    try { responseFormat.encoding = Charset.forName(respOpts.getString("encoding")); }
                    catch(JSONException | IllegalArgumentException e) { LoggerUtilities.optionWarn(log, "charset", "encoding", respOpts.opt("encoding")); }
                }
            } else {
                LoggerUtilities.optionWarn(log, "JSONObject", "rx", serialOpts.opt("rx"));
            }
        } else if (isOpening) {
            // legacy support - only applies on port open
            responseFormat = new ResponseFormat();

            // legacy start only supports string, not an array
            if (!serialOpts.isNull("start")) {
                responseFormat.boundStart = DeviceUtilities.characterBytes(serialOpts.optString("start", DEFAULT_BEGIN), responseFormat.encoding);
            } else {
                responseFormat.boundStart = DeviceUtilities.characterBytes(DEFAULT_BEGIN, responseFormat.encoding);
            }

            if (!serialOpts.isNull("end")) {
                responseFormat.boundEnd = DeviceUtilities.characterBytes(serialOpts.optString("end", DEFAULT_END), responseFormat.encoding);
            } else {
                responseFormat.boundEnd = DeviceUtilities.characterBytes(DEFAULT_END, responseFormat.encoding);
            }

            if (!serialOpts.isNull("width")) {
                try {
                    responseFormat.fixedWidth = serialOpts.getInt("width");
                    if (responseFormat.boundEnd.length > 0) {
                        log.warn("Combining 'width' property with 'end' property has undefined behavior and should not be used");
                    }
                }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "width", serialOpts.opt("width")); }
            }
        }
    }

    public PortSettings getPortSettings() {
        return portSettings;
    }

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public void setPortSettings(PortSettings portSettings) {
        this.portSettings = portSettings;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    public class PortSettings {

        private Charset encoding = Charset.forName("UTF-8");
        private int baudRate = SerialPort.BAUDRATE_9600;
        private int dataBits = SerialPort.DATABITS_8;
        private int stopBits = SerialPort.STOPBITS_1;
        private int parity = SerialPort.PARITY_NONE;
        private int flowControl = SerialPort.FLOWCONTROL_NONE;


        public Charset getEncoding() {
            return encoding;
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

        @Override
        public boolean equals(Object o) {
            if (o instanceof PortSettings) {
                PortSettings that = (PortSettings)o;

                return getEncoding().equals(that.getEncoding()) &&
                        getBaudRate() == that.getBaudRate() &&
                        getDataBits() == that.getDataBits() &&
                        getStopBits() == that.getStopBits() &&
                        getParity() == that.getParity() &&
                        getFlowControl() == that.getFlowControl();
            } else {
                return false;
            }
        }
    }

    public class ResponseFormat {

        private Charset encoding = Charset.forName("UTF-8");    //Response charset
        private byte[] boundStart;                              //Character(s) denoting start of new response
        private byte[] boundEnd;                                //Character denoting end of a response
        private boolean boundNewline;                           //If the response should be split on \r?\n
        private int fixedWidth;                                 //Fixed length response bounds
        private ByteParam length;                               //Info about the data length byte(s)
        private ByteParam crc;                                  //Info about the data crc byte(s)
        private boolean includeStart;                           //If the response headers should be sent as well


        public Charset getEncoding() {
            return encoding;
        }

        public byte[] getBoundStart() {
            return boundStart;
        }

        public byte[] getBoundEnd() {
            return boundEnd;
        }

        public int getFixedWidth() {
            return fixedWidth;
        }

        public ByteParam getLength() {
            return length;
        }

        public ByteParam getCrc() {
            return crc;
        }

        public boolean isIncludeStart() {
            return includeStart;
        }

        public boolean isBoundNewline() {
            return boundNewline;
        }
    }

    public class ByteParam {

        private int index = 0;
        private int length = 1;
        private ByteUtilities.Endian endian = ByteUtilities.Endian.BIG;


        public int getIndex() {
            return index;
        }

        public int getLength() {
            return length;
        }

        public ByteUtilities.Endian getEndian() {
            return endian;
        }
    }

}
