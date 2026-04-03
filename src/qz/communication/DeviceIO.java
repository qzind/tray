package qz.communication;

public interface DeviceIO extends DeviceListener {

    String getVendorId();

    String getProductId();

    DeviceOptions getDeviceOptions();

    void open() throws DeviceException;

    boolean isOpen();

    void close();

    void setStreaming(boolean streaming);

    boolean isStreaming();


    byte[] readData(int responseSize, Byte exchangeConfig) throws DeviceException;

    void sendData(byte[] data, Byte exchangeConfig) throws DeviceException;


    byte[] getFeatureReport(int responseSize, Byte reportId) throws DeviceException;

    void sendFeatureReport(byte[] data, Byte reportId) throws DeviceException;
}
