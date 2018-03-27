package qz.communication;

import org.hid4java.HidDevice;

import javax.usb.util.UsbUtil;

public class H4J_HidIO implements DeviceIO {

    private HidDevice device;

    private boolean streaming;


    public H4J_HidIO(DeviceOptions dOpts) throws DeviceException {
        this(H4J_HidUtilities.findDevice(dOpts.getVendorId(), dOpts.getProductId(), dOpts.getUsagePage(), dOpts.getSerial()));
    }

    public H4J_HidIO(HidDevice device) throws DeviceException {
        if (device == null) {
            throw new DeviceException("HID device could not be found");
        }

        this.device = device;
    }

    public void open() {
        if (!isOpen()) {
            device.open();
        }
    }

    public boolean isOpen() {
        return device.isOpen();
    }

    public void setStreaming(boolean active) {
        streaming = active;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public String getVendorId() {
        return UsbUtil.toHexString(device.getVendorId());
    }

    public String getProductId() {
        return UsbUtil.toHexString(device.getProductId());
    }

    public byte[] readData(int responseSize, Byte unused) throws DeviceException {
        byte[] response = new byte[responseSize];

        int read = device.read(response);
        if (read == -1) {
            throw new DeviceException("Failed to read from device");
        }

        return response;
    }

    public void sendData(byte[] data, Byte reportId) throws DeviceException {
        if (reportId == null) { reportId = (byte)0x00; }

        int wrote = device.write(data, data.length, reportId);
        if (wrote == -1) {
            throw new DeviceException("Failed to write to device");
        }
    }

    public void close() {
        if (isOpen()) {
            device.close();
        }
        streaming = false;
    }

}
