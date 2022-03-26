package qz.communication;

public interface DeviceIO {

    String getVendorId();

    String getProductId();


    void open() throws UsbException;

    boolean isOpen();

    void close() throws UsbException;


    void setStreaming(boolean streaming);

    boolean isStreaming();


    byte[] readData(int responseSize, Byte exchangeConfig) throws UsbException;

    void sendData(byte[] data, Byte exchangeConfig) throws UsbException;


    byte[] getFeatureReport(int responseSize, Byte reportId) throws UsbException;

    void sendFeatureReport(byte[] data, Byte reportId) throws UsbException;
}
