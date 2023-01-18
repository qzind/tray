package qz.communication;

import org.hid4java.HidDevice;

import javax.usb.util.UsbUtil;

public class H4J_HidIO implements DeviceIO {

    private HidDevice device;

    private boolean streaming;


    public H4J_HidIO(UsbOptions dOpts) throws UsbException {
        this(H4J_HidUtilities.findDevice(dOpts));
    }

    public H4J_HidIO(HidDevice device) throws UsbException {
        if (device == null) {
            // FIXME: This shouldn't generate a general exception up the stack, but it does
            throw new UsbException("HID device could not be found");
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

    public byte[] readData(int responseSize, Byte unused) throws UsbException {
        byte[] response = new byte[responseSize];

        int read = device.read(response);
        if (read == -1) {
            throw new UsbException("Failed to read from device");
        }

        return response;
    }

    public void sendData(byte[] data, Byte reportId) throws UsbException {
        if (reportId == null) { reportId = (byte)0x00; }

        int wrote = device.write(data, data.length, reportId);
        if (wrote == -1) {
            throw new UsbException("Failed to write to device");
        }
    }

    public byte[] getFeatureReport(int responseSize, Byte reportId) throws UsbException {
        if (reportId == null) { reportId = (byte)0x00; }
        byte[] response = new byte[responseSize];

        int read = device.getFeatureReport(response, reportId);
        if (read == -1) {
            throw new UsbException("Failed to read from device");
        }

        return response;

    }

    public void sendFeatureReport(byte[] data, Byte reportId) throws UsbException {
        if (reportId == null) { reportId = (byte)0x00; }

        int wrote = device.sendFeatureReport(data, reportId);
        if (wrote == -1) {
            throw new UsbException("Failed to write to device");
        }
    }

    public void close() {
        if (isOpen()) {
            device.close();
        }
        streaming = false;
    }

}
