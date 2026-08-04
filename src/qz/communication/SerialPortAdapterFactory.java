package qz.communication;

interface SerialPortAdapterFactory {
    SerialPortAdapter create(String portName);
}
