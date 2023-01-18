package qz.communication;

import qz.utils.UsbUtilities;

import javax.usb.*;
import javax.usb.util.UsbUtil;

public class UsbIO implements DeviceIO {

    private UsbDevice device;
    private UsbInterface iface;

    private boolean streaming;


    public UsbIO(UsbOptions dOpts) throws UsbException {
        UsbDevice device = UsbUtilities.findDevice(dOpts.getVendorId().shortValue(), dOpts.getProductId().shortValue());

        if (device == null) {
            throw new UsbException("USB device could not be found");
        }
        if (dOpts.getInterfaceId() == null) {
            throw new IllegalArgumentException("Device interface cannot be null");
        }

        this.device = device;
        this.iface = device.getActiveUsbConfiguration().getUsbInterface(dOpts.getInterfaceId());
    }

    public void open() throws UsbException {
        try {
            iface.claim(new UsbInterfacePolicy() {
                @Override
                public boolean forceClaim(UsbInterface usbInterface) {
                    // Releases kernel driver for systems that auto-claim usb devices
                    return true;
                }
            });
        }
        catch(javax.usb.UsbException e) {
            throw new UsbException(e);
        }
    }

    public boolean isOpen() {
        return iface.isClaimed();
    }

    public void setStreaming(boolean active) {
        streaming = active;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public String getVendorId() {
        return UsbUtil.toHexString(device.getUsbDeviceDescriptor().idVendor());
    }

    public String getProductId() {
        return UsbUtil.toHexString(device.getUsbDeviceDescriptor().idProduct());
    }

    public String getInterface() {
        return UsbUtil.toHexString(iface.getUsbInterfaceDescriptor().iInterface());
    }

    public byte[] readData(int responseSize, Byte endpoint) throws UsbException {
        try {
            byte[] response = new byte[responseSize];
            exchangeData(endpoint, response);
            return response;
        }
        catch(javax.usb.UsbException e) {
            throw new UsbException(e);
        }
    }

    public void sendData(byte[] data, Byte endpoint) throws UsbException {
        try {
            exchangeData(endpoint, data);
        }
        catch(javax.usb.UsbException e) {
            throw new UsbException(e);
        }
    }

    public byte[] getFeatureReport(int responseSize, Byte reportId) throws UsbException {
        throw new UsbException("USB feature reports are not supported");
    }

    public void sendFeatureReport(byte[] data, Byte reportId) throws UsbException {
        throw new UsbException("USB feature reports are not supported");
    }

    /**
     * Data will be sent to or received from the open usb device, depending on the {@code endpoint} used.
     *
     * @param endpoint Endpoint on the usb device interface to pass data across
     * @param data     Byte array of data to send, or to be written from a receive
     */
    private synchronized void exchangeData(Byte endpoint, byte[] data) throws javax.usb.UsbException {
        if (endpoint == null) {
            throw new IllegalArgumentException("Interface endpoint cannot be null");
        }

        UsbPipe pipe = iface.getUsbEndpoint(endpoint).getUsbPipe();
        if (!pipe.isOpen()) { pipe.open(); }

        try {
            pipe.syncSubmit(data);
        }
        finally {
            pipe.close();
        }
    }

    public void close() throws UsbException {
        if (iface.isClaimed()) {
            try {
                iface.release();
            }
            catch(javax.usb.UsbException e) {
                throw new UsbException(e);
            }
        }
        streaming = false;
    }

}
