package qz.communication;

public interface DeviceIO {

    String getVendorId();

    String getProductId();


    void open() throws DeviceException;

    boolean isOpen();

    void close() throws DeviceException;


    void setStreaming(boolean streaming);

    boolean isStreaming();


    byte[] readData(int responseSize, Byte exchangeConfig) throws DeviceException;

    void sendData(byte[] data, Byte exchangeConfig) throws DeviceException;

}
