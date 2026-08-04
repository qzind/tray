package qz.communication;

import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

class JsscSerialPortAdapter implements SerialPortAdapter {
    private final SerialPort port;

    JsscSerialPortAdapter(String portName) {
        port = new SerialPort(portName);
    }

    @Override
    public boolean openPort() throws SerialPortException {
        return port.openPort();
    }

    @Override
    public boolean closePort() throws SerialPortException {
        return port.closePort();
    }

    @Override
    public boolean isOpened() {
        return port.isOpened();
    }

    @Override
    public void addEventListener(SerialPortEventListener listener) throws SerialPortException {
        port.addEventListener(listener);
    }

    @Override
    public void setParams(int baudRate, int dataBits, int stopBits, int parity) throws SerialPortException {
        port.setParams(baudRate, dataBits, stopBits, parity);
    }

    @Override
    public void setFlowControlMode(int mask) throws SerialPortException {
        port.setFlowControlMode(mask);
    }

    @Override
    public void writeBytes(byte[] data) throws SerialPortException {
        port.writeBytes(data);
    }

    @Override
    public byte[] readBytes(int byteCount, int timeout) throws SerialPortException, SerialPortTimeoutException {
        return port.readBytes(byteCount, timeout);
    }
}
