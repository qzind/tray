package qz.communication;

import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;
import qz.utils.SystemUtilities;

import javax.usb.util.UsbUtil;
import java.io.IOException;

public class PJHA_HidIO implements DeviceIO {

    private HidDeviceInfo deviceInfo;
    private HidDevice device;

    private byte[] latestData;
    private boolean streaming;


    public PJHA_HidIO(Short vendorId, Short productId) throws DeviceException {
        this(PJHA_HidUtilities.findDevice(vendorId, productId));
    }

    public PJHA_HidIO(HidDeviceInfo deviceInfo) throws DeviceException {
        if (deviceInfo == null) {
            throw new DeviceException("HID device could not be found");
        }

        this.deviceInfo = deviceInfo;
    }

    public void open() throws DeviceException {
        if (!isOpen()) {
            try {
                device = PureJavaHidApi.openDevice(deviceInfo);
                device.setInputReportListener(new InputReportListener() {
                    @Override
                    public void onInputReport(HidDevice source, byte id, byte[] data, int len) {
                        latestData = data;
                    }
                });
            }
            catch(IOException ex) {
                throw new DeviceException(ex);
            }
        }
    }

    public boolean isOpen() {
        return device != null;
    }

    public void setStreaming(boolean active) {
        streaming = active;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public String getVendorId() {
        return UsbUtil.toHexString(deviceInfo.getVendorId());
    }

    public String getProductId() {
        return UsbUtil.toHexString(deviceInfo.getProductId());
    }

    public byte[] readData(int responseSize, Byte unused) throws DeviceException {
        byte[] response = new byte[responseSize];
        if (latestData == null) {
            return new byte[0]; //no data received yet
        }
        if (SystemUtilities.isWindows()) {
            //windows missing the leading byte
            System.arraycopy(latestData, 0, response, 1, Math.min(responseSize - 1, latestData.length));
        } else {
            System.arraycopy(latestData, 0, response, 0, Math.min(responseSize - 1, latestData.length));
        }
        return response;
    }

    public void sendData(byte[] data, Byte reportId) throws DeviceException {
        if (reportId == null) { reportId = (byte)0x00; }

        int wrote = device.setOutputReport(reportId, data, data.length);
        if (wrote == -1) {
            throw new DeviceException("Failed to write to device");
        }
    }

    public void close() {
        if (isOpen()) {
            device.setInputReportListener(null);
            device.close();
        }
        streaming = false;
    }

}
