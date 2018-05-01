package qz.communication;

import jssc.*;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.ByteArrayBuilder;
import qz.utils.ByteUtilities;
import qz.utils.SerialUtilities;

/**
 * @author Tres
 */
public class SerialIO {

    private static final Logger log = LoggerFactory.getLogger(SerialIO.class);

    // Timeout to wait before giving up on reading the specified amount of bytes
    private static final int TIMEOUT = 1200;

    private String portName;
    private SerialPort port;
    private SerialProperties props;

    private ByteArrayBuilder data = new ByteArrayBuilder();

    // bytes denoting boundaries for messages received from serial port
    private byte[] dataBegin;
    private byte[] dataEnd;

    //length of fixed-width responses
    private Integer width;


    /**
     * Controller for serial communications
     *
     * @param portName Port name to open, such as "COM1" or "/dev/tty0/"
     */
    public SerialIO(String portName) throws SerialPortException {
        this.portName = portName;
    }

    /**
     * Open the specified port name.
     *
     * @param props Parsed serial properties
     * @return Boolean indicating success.
     * @throws SerialPortException If the port fails to open.
     */
    public boolean open(SerialProperties props) throws SerialPortException {
        if (isOpen()) {
            log.warn("Serial port [{}] is already open", portName);
            return false;
        }

        port = new SerialPort(portName);
        port.openPort();
        setProperties(props);

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
        try {
            // Receive data
            if (event.isRXCHAR()) {
                data.append(port.readBytes(event.getEventValue(), TIMEOUT));

                if (width == null) {
                    //delimited response
                    Integer[] beginPos = ByteUtilities.indicesOfMatches(data.getByteArray(), dataBegin);
                    Integer[] endPos = ByteUtilities.indicesOfMatches(data.getByteArray(), dataEnd);

                    if (beginPos.length > 0 && endPos.length > 0) {
                        int begin = beginPos[0] + 1;
                        int end = endPos[0];

                        byte[] output = new byte[end - begin];
                        System.arraycopy(data.getByteArray(), begin, output, 0, end - begin);

                        data.clearRange(begin - 1, end + 1); //begin/end indexes don't include delimiters
                        return StringUtils.newStringUtf8(output);
                    }
                } else if (data.getLength() >= width) {
                    //fixed width response
                    byte[] output = new byte[width];
                    System.arraycopy(data.getByteArray(), 0, output, 0, width);

                    data.clearRange(0, width);
                    return StringUtils.newStringUtf8(output);
                }
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
    private void setProperties(SerialProperties props) throws SerialPortException {
        if (props == null) { return; }

        if (props.getBoundWidth() == null) {
            dataBegin = SerialUtilities.characterBytes(props.getBoundBegin());
            dataEnd = SerialUtilities.characterBytes(props.getBoundEnd());
        } else {
            width = props.getBoundWidth();
        }

        boolean equals = this.props != null &&
                this.props.getBaudRate() == props.getBaudRate() &&
                this.props.getDataBits() == props.getDataBits() &&
                this.props.getFlowControl() == props.getFlowControl() &&
                this.props.getParity() == props.getParity() &&
                this.props.getStopBits() == props.getStopBits();

        if (!equals) {
            port.setParams(props.getBaudRate(), props.getDataBits(), props.getStopBits(), props.getParity());
            port.setFlowControlMode(props.getFlowControl());
            this.props = props;
        }
    }

    /**
     * Applies the port parameters and writes the buffered data to the serial port.
     */
    public void sendData(String data, SerialProperties props) throws SerialPortException {
        if (props != null) { setProperties(props); }

        log.debug("Sending data over [{}]", portName);
        port.writeBytes(SerialUtilities.characterBytes(data));
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
