package qz.communication;

import jssc.*;
import org.apache.commons.codec.binary.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.ByteArrayBuilder;
import qz.utils.ByteUtilities;
import qz.utils.DeviceUtilities;
import qz.ws.SocketConnection;

import java.io.IOException;

/**
 * @author Tres
 */
public class SerialIO implements DeviceListener {

    private static final Logger log = LogManager.getLogger(SerialIO.class);

    // Timeout to wait before giving up on reading the specified amount of bytes
    private static final int TIMEOUT = 1200;

    private String portName;
    private SerialPort port;
    private SerialOptions serialOpts;

    private ByteArrayBuilder data = new ByteArrayBuilder();

    private SocketConnection websocket;


    /**
     * Controller for serial communications
     *
     * @param portName Port name to open, such as "COM1" or "/dev/tty0/"
     */
    public SerialIO(String portName, SocketConnection websocket) {
        this.portName = portName;
        this.websocket = websocket;
    }

    /**
     * Open the specified port name.
     *
     * @param opts Parsed serial options
     * @return Boolean indicating success.
     * @throws SerialPortException If the port fails to open.
     */
    public boolean open(SerialOptions opts) throws SerialPortException {
        if (isOpen()) {
            log.warn("Serial port [{}] is already open", portName);
            return false;
        }

        port = new SerialPort(portName);
        port.openPort();

        serialOpts = new SerialOptions();
        setOptions(opts);

        return port.isOpened();
    }

    public void applyPortListener(SerialPortEventListener listener) throws SerialPortException {
        port.addEventListener(listener);
    }

    /**
     * @return Boolean indicating if port is currently open.
     */
    public boolean isOpen() {
        return port != null && port.isOpened();
    }

    public String processSerialEvent(SerialPortEvent event) {
        SerialOptions.ResponseFormat format = serialOpts.getResponseFormat();

        try {
            // Receive data
            if (event.isRXCHAR()) {
                data.append(port.readBytes(event.getEventValue(), TIMEOUT));

                String response = null;
                if (format.isBoundNewline()) {
                    //process as line delimited

                    // check for CR AND NL
                    Integer endIdx = ByteUtilities.firstMatchingIndex(data.toByteArray(), new byte[] {'\r', '\n'});
                    int delimSize = 2;

                    // check for CR OR NL
                    if(endIdx == null) {
                        endIdx = min(
                                ByteUtilities.firstMatchingIndex(data.toByteArray(), new byte[] {'\r'}),
                                ByteUtilities.firstMatchingIndex(data.toByteArray(), new byte[] {'\n'}));
                        delimSize = 1;
                    }
                    if (endIdx != null) {
                        log.trace("Reading newline-delimited response");
                        byte[] output = new byte[endIdx];
                        System.arraycopy(data.toByteArray(), 0, output, 0, endIdx);
                        String buffer = new String(output, format.getEncoding());

                        if (!buffer.isEmpty()) {
                            //send non-empty string
                            response = buffer;
                        }

                        data.clearRange(0, endIdx + delimSize);
                    }
                } else if (format.getBoundStart() != null && format.getBoundStart().length > 0) {
                    //process as formatted response
                    Integer startIdx = ByteUtilities.firstMatchingIndex(data.toByteArray(), format.getBoundStart());

                    if (startIdx != null) {
                        int startOffset = startIdx + format.getBoundStart().length;

                        int copyLength = 0;
                        int endIdx = 0;

                        if (format.getBoundEnd() != null && format.getBoundEnd().length > 0) {
                            //process as bounded response
                            Integer boundEnd = ByteUtilities.firstMatchingIndex(data.toByteArray(), format.getBoundEnd(), startIdx);

                            if (boundEnd != null) {
                                log.trace("Reading bounded response");

                                copyLength = boundEnd - startOffset;
                                endIdx = boundEnd + 1;
                                if (format.isIncludeStart()) {
                                    //also include the ending bytes
                                    copyLength += format.getBoundEnd().length;
                                }
                            }
                        } else if (format.getFixedWidth() > 0) {
                            //process as fixed length prefixed response
                            log.trace("Reading fixed length prefixed response");

                            copyLength = format.getFixedWidth();
                            endIdx = startOffset + format.getFixedWidth();
                        } else if (format.getLength() != null) {
                            //process as dynamic formatted response
                            SerialOptions.ByteParam lengthParam = format.getLength();

                            if (data.getLength() > startOffset + lengthParam.getIndex() + lengthParam.getLength()) { //ensure there's length bytes to read
                                log.trace("Reading dynamic formatted response");

                                int expectedLength = ByteUtilities.parseBytes(data.toByteArray(), startOffset + lengthParam.getIndex(), lengthParam.getLength(), lengthParam.getEndian());
                                log.trace("Found length byte, expecting {} bytes", expectedLength);

                                startOffset += lengthParam.getIndex() + lengthParam.getLength(); // don't include the length byte(s) in the response
                                copyLength = expectedLength;
                                endIdx = startOffset + copyLength;

                                if (format.getCrc() != null) {
                                    SerialOptions.ByteParam crcParam = format.getCrc();

                                    log.trace("Expecting {} crc bytes", crcParam.getLength());
                                    int expand = crcParam.getIndex() + crcParam.getLength();

                                    //include crc in copy
                                    copyLength += expand;
                                    endIdx += expand;
                                }
                            }
                        } else {
                            //process as header formatted raw response - high risk of lost data, likely unintended settings
                            log.warn("Reading header formatted raw response, are you missing an rx option?");

                            copyLength = data.getLength() - startOffset;
                            endIdx = data.getLength();
                        }


                        if (copyLength > 0 && data.getLength() >= endIdx) {
                            log.debug("Response format readable, starting copy");

                            if (format.isIncludeStart()) {
                                //increase length to account for header bytes and bump offset back to include in copy
                                copyLength += (startOffset - startIdx);
                                startOffset = startIdx;
                            }

                            byte[] responseData = new byte[copyLength];
                            System.arraycopy(data.toByteArray(), startOffset, responseData, 0, copyLength);

                            response = new String(responseData, format.getEncoding());
                            data.clearRange(startIdx, endIdx);
                        }
                    }
                } else if (format.getFixedWidth() > 0) {
                    if (data.getLength() >= format.getFixedWidth()) {
                        //process as fixed width response
                        log.trace("Reading fixed length response");

                        byte[] output = new byte[format.getFixedWidth()];
                        System.arraycopy(data.toByteArray(), 0, output, 0, format.getFixedWidth());

                        response = StringUtils.newStringUtf8(output);
                        data.clearRange(0, format.getFixedWidth());
                    }
                } else {
                    //no processing, return raw
                    log.trace("Reading raw response");

                    response = new String(data.toByteArray(), format.getEncoding());
                    data.clear();
                }

                return response;
            }
        }
        catch(SerialPortException e) {
            log.error("Exception occurred while reading data from port.", e);
        }
        catch(SerialPortTimeoutException e) {
            log.error("Timeout occurred waiting for port to respond.", e);
        }

        return null;
    }

    /**
     * Sets and caches the properties as to not set them every data call
     *
     * @throws SerialPortException If the properties fail to set
     */
    private void setOptions(SerialOptions opts) throws SerialPortException {
        if (opts == null) { return; }

        SerialOptions.PortSettings ps = opts.getPortSettings();
        if (ps != null && !ps.equals(serialOpts.getPortSettings())) {
            log.debug("Applying new port settings");
            port.setParams(ps.getBaudRate(), ps.getDataBits(), ps.getStopBits(), ps.getParity());
            port.setFlowControlMode(ps.getFlowControl());
            serialOpts.setPortSettings(ps);
        }

        SerialOptions.ResponseFormat rf = opts.getResponseFormat();
        if (rf != null) {
            log.debug("Applying new response formatting");
            serialOpts.setResponseFormat(rf);
        }
    }

    /**
     * Applies the port parameters and writes the buffered data to the serial port.
     */
    public void sendData(JSONObject params, SerialOptions opts) throws JSONException, IOException, SerialPortException {
        if (opts != null) {
            setOptions(opts);
        }

        log.debug("Sending data over [{}]", portName);
        port.writeBytes(DeviceUtilities.getDataBytes(params, serialOpts.getPortSettings().getEncoding()));
    }

    /**
     * Closes the serial port, if open.
     *
     * @throws SerialPortException If the port fails to close.
     */
    @Override
    public void close() {
        // Remove orphaned reference
        websocket.removeSerialPort(portName);

        if (!isOpen()) {
            log.warn("Serial port [{}] is not open.", portName);
        }

        try {
            boolean closed = port.closePort();
            if (closed) {
                log.info("Serial port [{}] closed successfully.", portName);
            } else {
                // Handle ambiguity in JSSCs API
                throw new SerialPortException(portName, "closePort", "Port not closed");
            }
        } catch(SerialPortException e) {
            log.warn("Serial port [{}] was not closed properly.", portName);
        }

        port = null;
        portName = null;
    }

    private Integer min(Integer a, Integer b) {
        if (a == null) { return b; }
        if (b == null) { return a; }
        return Math.min(a, b);
    }

}
