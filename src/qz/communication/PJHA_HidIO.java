package qz.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.InputReportListener;
import purejavahidapi.PureJavaHidApi;
import qz.utils.SystemUtilities;

import javax.usb.util.UsbUtil;
import java.io.IOException;
import java.util.Vector;

public class PJHA_HidIO implements DeviceIO {

    private static final Logger log = LogManager.getLogger(PJHA_HidIO.class);

    private HidDeviceInfo deviceInfo;
    private HidDevice device;

    private static final int BUFFER_SIZE = 32;
    private Vector<byte[]> dataBuffer;
    private boolean streaming;


    public PJHA_HidIO(UsbOptions dOpts) throws UsbException {
        this(PJHA_HidUtilities.findDevice(dOpts));
    }

    public PJHA_HidIO(HidDeviceInfo deviceInfo) throws UsbException {
        if (deviceInfo == null) {
            throw new UsbException("HID device could not be found");
        }

        this.deviceInfo = deviceInfo;

        dataBuffer = new Vector<byte[]>() {
            @Override
            public synchronized boolean add(byte[] e) {
                while(this.size() >= BUFFER_SIZE) {
                    this.remove(0);
                }
                return super.add(e);
            }
        };
    }

    public void open() throws UsbException {
        if (!isOpen()) {
            try {
                device = PureJavaHidApi.openDevice(deviceInfo);
                device.setInputReportListener(new InputReportListener() {
                    @Override
                    public void onInputReport(HidDevice source, byte id, byte[] data, int len) {
                        byte[] dataCopy = new byte[len];
                        System.arraycopy(data, 0, dataCopy, 0, len);
                        dataBuffer.add(dataCopy);
                    }
                });
            }
            catch(IOException ex) {
                throw new UsbException(ex);
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

    public byte[] readData(int responseSize, Byte unused) throws UsbException {
        byte[] response = new byte[responseSize];
        if (dataBuffer.isEmpty()) {
            return new byte[0]; //no data received yet
        }

        byte[] latestData = dataBuffer.remove(0);
        if (SystemUtilities.isWindows()) {
            //windows missing the leading byte
            System.arraycopy(latestData, 0, response, 1, Math.min(responseSize - 1, latestData.length));
        } else {
            System.arraycopy(latestData, 0, response, 0, Math.min(responseSize - 1, latestData.length));
        }
        return response;
    }

    public void sendData(byte[] data, Byte reportId) throws UsbException {
        if (reportId == null) { reportId = (byte)0x00; }

        int wrote = device.setOutputReport(reportId, data, data.length);
        if (wrote == -1) {
            throw new UsbException("Failed to write to device");
        }
    }

    public byte[] getFeatureReport(int responseSize, Byte unused) throws UsbException {
        byte[] response = new byte[responseSize];
        int read = device.getFeatureReport(response, responseSize);
        if (read == -1) {
            throw new UsbException("Failed to read from device");
        }
        return response;
    }

    public void sendFeatureReport(byte[] data, Byte reportId) throws UsbException {
        if (reportId == null) { reportId = (byte)0x00; }
        int wrote = device.setFeatureReport(reportId, data, data.length);

        if (wrote == -1) {
            throw new UsbException("Failed to write to device");
        }

    }

    public void close() {
        if (isOpen()) {
            try {
                device.setInputReportListener(null);
                device.close();
            }
            catch(IllegalStateException e) {
                log.warn("Device already closed");
            }
        }

        streaming = false;
        device = null;
    }

}
