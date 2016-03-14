package qz.communication;

import jssc.*;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.ByteUtilities;
import qz.utils.SerialUtilities;

import java.util.Arrays;

/**
 * @author Tres
 */
public class SerialIO {

    private static final Logger log = LoggerFactory.getLogger(SerialIO.class);

    // Timeout to wait before giving up on reading the specified amount of bytes
    private static final int TIMEOUT = 1200;

    private String portName;
    private SerialPort port;

    // bytes denoting boundaries for messages received from serial port
    private byte[] dataBegin;
    private byte[] dataEnd;

    //length of fixed-width responses
    private Integer width;


    /**
     * Controller for Serial communication.
     *
     * @param portName  Port name to open, such as "COM1" or "/dev/tty0/"
     * @param dataBegin Starting character bytes of serial responses.
     * @param dataEnd   Ending character bytes of serial response.
     */
    public SerialIO(String portName, byte[] dataBegin, byte[] dataEnd) {
        this.portName = portName;

        this.dataBegin = Arrays.copyOf(dataBegin, dataBegin.length);
        this.dataEnd = Arrays.copyOf(dataEnd, dataEnd.length);
    }

    /**
     * Controller for Serial communication.
     *
     * @param portName Port name to open, such as "COM1" or "/dev/tty0/"
     * @param width    Length of fixed-width responses.
     */
    public SerialIO(String portName, int width) {
        this.portName = portName;

        this.width = width;
    }


    /**
     * Open the specified port name.
     *
     * @return Boolean indicating success.
     * @throws SerialPortException If the port fails to open.
     */
    public boolean open() throws SerialPortException {
        if (isOpen()) {
            log.warn("Serial port [{}] is already open", portName);
            return false;
        }

        port = new SerialPort(portName);
        port.openPort();

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
                byte[] data = port.readBytes(event.getEventValue(), TIMEOUT);

                if (width == null) {
                    //delimited response
                    Integer[] beginPos = ByteUtilities.indicesOfMatches(data, dataBegin);
                    Integer[] endPos = ByteUtilities.indicesOfMatches(data, dataEnd);

                    if (beginPos.length > 0 && endPos.length > 0) {
                        int begin = beginPos[beginPos.length - 1];
                        int end = endPos[endPos.length - 1];

                        byte[] output = new byte[end - begin];
                        System.arraycopy(data, begin, output, 0, end - begin);

                        return StringUtils.newStringUtf8(output);
                    }
                } else {
                    //fixed width response
                    byte[] output = new byte[width];
                    System.arraycopy(data, 0, output, 0, width);

                    return new String(output);
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
     * Applies the port parameters and writes the buffered data to the serial port.
     */
    public void sendData(SerialProperties props, String data) throws SerialPortException {
        port.setParams(props.getBaudRate(), props.getDataBits(), props.getStopBits(), props.getParity());
        port.setFlowControlMode(props.getFlowControl());

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
