package qz.communication;

import qz.utils.UsbUtilities;

import javax.usb.*;
import javax.usb.util.UsbUtil;

public class UsbIO implements DeviceIO {

    private UsbDevice device;
    private UsbInterface iface;

    private boolean streaming;


    public UsbIO(DeviceOptions dOpts) throws DeviceException {
        UsbDevice device = UsbUtilities.findDevice(dOpts.getVendorId(), dOpts.getProductId());

        if (device == null) {
            throw new DeviceException("USB device could not be found");
        }
        if (dOpts.getInterfaceId() == null) {
            throw new IllegalArgumentException("Device interface cannot be null");
        }

        this.device = device;
        this.iface = device.getActiveUsbConfiguration().getUsbInterface(dOpts.getInterfaceId());
    }

    public void open() throws DeviceException {
        try {
            iface.claim(new UsbInterfacePolicy() {
                @Override
                public boolean forceClaim(UsbInterface usbInterface) {
                    // Releases kernel driver for systems that auto-claim usb devices
                    return true;
                }
            });
        }
        catch(UsbException e) {
            throw new DeviceException(e);
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

    public byte[] readData(int responseSize, Byte endpoint) throws DeviceException {
        try {
            byte[] response = new byte[responseSize];
            exchangeData(endpoint, response);
            return response;
        }
        catch(UsbException e) {
            throw new DeviceException(e);
        }
    }

    public void sendData(byte[] data, Byte endpoint) throws DeviceException {
        try {
            exchangeData(endpoint, data);
        }
        catch(UsbException e) {
            throw new DeviceException(e);
        }
    }

    /**
     * Data will be sent to or received from the open usb device, depending on the {@code endpoint} used.
     *
     * @param endpoint Endpoint on the usb device interface to pass data across
     * @param data     Byte array of data to send, or to be written from a receive
     */
    private synchronized void exchangeData(Byte endpoint, byte[] data) throws UsbException {
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

    public void close() throws DeviceException {
        if (iface.isClaimed()) {
            try {
                iface.release();
            }
            catch(UsbException e) {
                throw new DeviceException(e);
            }
        }
        streaming = false;
    }

}
