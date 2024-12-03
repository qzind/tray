package qz.communication;

import org.hid4java.HidDevice;
import qz.ws.SocketConnection;

import javax.usb.util.UsbUtil;

public class H4J_HidIO implements DeviceIO, DeviceListener {

    private HidDevice device;

    private boolean streaming;

    private DeviceOptions dOpts;
    private SocketConnection websocket;


    public H4J_HidIO(DeviceOptions dOpts, SocketConnection websocket) throws DeviceException {
        this(H4J_HidUtilities.findDevice(dOpts), dOpts, websocket);
    }

    private H4J_HidIO(HidDevice device, DeviceOptions dOpts, SocketConnection websocket) throws DeviceException {
        this.dOpts = dOpts;
        this.websocket = websocket;
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
        return !device.isClosed();
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

    public byte[] getFeatureReport(int responseSize, Byte reportId) throws DeviceException {
        if (reportId == null) { reportId = (byte)0x00; }
        byte[] response = new byte[responseSize];

        int read = device.getFeatureReport(response, reportId);
        if (read == -1) {
            throw new DeviceException("Failed to read from device");
        }

        return response;

    }

    public void sendFeatureReport(byte[] data, Byte reportId) throws DeviceException {
        if (reportId == null) { reportId = (byte)0x00; }

        int wrote = device.sendFeatureReport(data, reportId);
        if (wrote == -1) {
            throw new DeviceException("Failed to write to device");
        }
    }

    @Override
    public void close() {
        setStreaming(false);
        // Remove orphaned reference
        websocket.removeDevice(dOpts);
        if (isOpen()) {
            device.close();
        }
    }

}
