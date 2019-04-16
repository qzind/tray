package qz.communication;

import jssc.*;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.ByteArrayBuilder;
import qz.utils.ByteUtilities;
import qz.utils.SerialUtilities;

import java.io.IOException;
import java.net.URL;

/**
 * @author Tres
 */
public class SerialIO {

    private static final Logger log = LoggerFactory.getLogger(SerialIO.class);

    // Timeout to wait before giving up on reading the specified amount of bytes
    private static final int TIMEOUT = 1200;

    private String portName;
    private SerialPort port;
    private SerialOptions serialOpts;

    private ByteArrayBuilder data = new ByteArrayBuilder();


    /**
     * Controller for serial communications
     *
     * @param portName Port name to open, such as "COM1" or "/dev/tty0/"
     */
    public SerialIO(String portName) {
        this.portName = portName;
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
        serialOpts = opts;

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
                if (format.getBoundStart() != null && format.getBoundStart().length > 0) {
                    //process as formatted response
                    Integer startIdx = ByteUtilities.firstMatchingIndex(data.getByteArray(), format.getBoundStart());

                    if (startIdx != null) {
                        int startOffset = startIdx + format.getBoundStart().length;

                        int copyLength = 0;
                        int endIdx = 0;

                        if (format.getBoundEnd() != null && format.getBoundEnd().length > 0) {
                            //process as bounded response
                            log.trace("Reading bounded response");

                            Integer boundEnd = ByteUtilities.firstMatchingIndex(data.getByteArray(), format.getBoundEnd());

                            if (boundEnd != null) {
                                copyLength = boundEnd - startOffset;
                                endIdx = boundEnd + 1;
                            }
                        } else if (format.getFixedWidth() > 0) {
                            //process as fixed length prefixed response
                            log.trace("Reading fixed length prefixed response");

                            copyLength = format.getFixedWidth();
                            endIdx = startOffset + format.getFixedWidth();
                        } else if (format.getLength() != null) {
                            //process as dynamic formatted response
                            log.trace("Reading dynamic formatted response");

                            SerialOptions.ByteParam lengthParam = format.getLength();
                            SerialOptions.ByteParam crcParam = format.getCrc();

                            if (data.getLength() > startOffset + lengthParam.getIndex() + lengthParam.getLength()) { //ensure there's a length byte to read
                                int expectedLength = ByteUtilities.parseBytes(data.getByteArray(), startOffset + lengthParam.getIndex(), lengthParam.getLength(), lengthParam.getEndian());
                                log.trace("Found length byte, expected data length: {}", expectedLength);

                                startOffset++; // don't include the length byte itself in the response
                                copyLength = expectedLength + crcParam.getIndex() + crcParam.getLength(); // data of length + crc
                                endIdx = startOffset + copyLength + 1;
                            }
                        } else {
                            //process as header formatted raw response
                            log.trace("Reading header formatted raw response");

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
                            System.arraycopy(data.getByteArray(), startOffset, responseData, 0, copyLength);

                            response = new String(responseData, format.getEncoding());
                            data.clearRange(startIdx, endIdx);
                        }
                    }
                } else if (format.getFixedWidth() > 0) {
                    if (data.getLength() >= format.getFixedWidth()) {
                        //process as fixed width response
                        log.trace("Reading fixed length response");

                        byte[] output = new byte[format.getFixedWidth()];
                        System.arraycopy(data.getByteArray(), 0, output, 0, format.getFixedWidth());

                        response = StringUtils.newStringUtf8(output);
                        data.clearRange(0, format.getFixedWidth());
                    }
                } else {
                    //no processing, return raw
                    log.trace("Reading raw response");

                    response = new String(data.getByteArray(), format.getEncoding());
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
    private void setOptions(SerialOptions props) throws SerialPortException {
        if (props == null) { return; }

        SerialOptions.PortSettings ps = props.getPortSettings();
        if (ps != null && !serialOpts.getPortSettings().equals(ps)) {
            port.setParams(ps.getBaudRate(), ps.getDataBits(), ps.getStopBits(), ps.getParity());
            port.setFlowControlMode(ps.getFlowControl());
        }

        //TODO - check for updated rx options
    }

    /**
     * Applies the port parameters and writes the buffered data to the serial port.
     */
    public void sendData(String data, SerialOptions opts, SerialUtilities.SerialDataType type) throws IOException, SerialPortException {
        if (opts != null) {
            setOptions(opts);
        }

        log.debug("Sending data over [{}]", portName);
        if (type == SerialUtilities.SerialDataType.FILE) {
            port.writeBytes(IOUtils.toByteArray(new URL(data)));
        } else {
            port.writeBytes(SerialUtilities.characterBytes(data));
        }
    }

    /**
     * Closes the serial port, if open.
     *
     * @return Boolean indicating success.
     * @throws SerialPortException If the port fails to close.
     */
    public boolean close() throws SerialPortException {
        if (!isOpen()) {
            log.warn("Serial port [{}] is not open.", portName);
            return false;
        }

        boolean closed = port.closePort();
        if (closed) {
            log.info("Serial port [{}] closed successfully.", portName);
        } else {
            log.warn("Serial port [{}] was not closed properly.", portName);
        }

        port = null;
        portName = null;

        return closed;
    }

}
