package qz.communication;

import qz.utils.UsbUtilities;

import javax.usb.*;
import javax.usb.util.UsbUtil;

public class UsbIO {

    private UsbDevice device;
    private UsbInterface iface;

    private boolean streaming;


    public UsbIO(short vendorId, short productId) throws UsbException {
        this(UsbUtilities.findDevice(vendorId, productId));
    }

    public UsbIO(UsbDevice device) throws UsbException {
        if (device == null) {
            throw new UsbException("USB device could not be found");
        }

        this.device = device;
    }

    public void open(byte ifc) throws UsbException {
        iface = device.getActiveUsbConfiguration().getUsbInterface(ifc);

        iface.claim(new UsbInterfacePolicy() {
            @Override
            public boolean forceClaim(UsbInterface usbInterface) {
                // Releases kernel driver for systems that auto-claim usb devices
                return true;
            }
        });
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

    public byte[] readData(byte endpoint, int responseSize) throws UsbException {
        byte[] response = new byte[responseSize];
        exchangeData(endpoint, response);
        return response;
    }

    public void sendData(byte endpoint, byte[] data) throws UsbException {
        exchangeData(endpoint, data);
    }

    /**
     * Data will be sent to or received from the open usb device, depending on the {@code endpoint} used.
     *
     * @param endpoint Endpoint on the usb device interface to pass data across
     * @param data     Byte array of data to send, or to be written from a receive
     */
    private synchronized void exchangeData(byte endpoint, byte[] data) throws UsbException {
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
        iface.release();
        streaming = false;
    }

}
