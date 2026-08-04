package qz.communication;

import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

interface SerialPortAdapter {
    boolean openPort() throws SerialPortException;

    boolean closePort() throws SerialPortException;

    boolean isOpened();

    void addEventListener(SerialPortEventListener listener) throws SerialPortException;

    void setParams(int baudRate, int dataBits, int stopBits, int parity) throws SerialPortException;

    void setFlowControlMode(int mask) throws SerialPortException;

    void writeBytes(byte[] data) throws SerialPortException;

    byte[] readBytes(int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException;
}
