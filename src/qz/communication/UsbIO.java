package qz.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.UsbUtilities;
import qz.ws.SocketConnection;

import javax.usb.*;
import javax.usb.util.UsbUtil;

public class UsbIO implements DeviceIO {
    private static final Logger log = LogManager.getLogger(UsbIO.class);
    private UsbDevice device;
    private UsbInterface iface;

    private boolean streaming;

    private DeviceOptions dOpts;
    private SocketConnection websocket;

    public UsbIO(DeviceOptions dOpts, SocketConnection websocket) throws DeviceException {
        this.dOpts = dOpts;
        this.websocket = websocket;
        UsbDevice device = UsbUtilities.findDevice(dOpts.getVendorId().shortValue(), dOpts.getProductId().shortValue());
        if (device == null) {
            throw new DeviceException("USB device could not be found");
        }
        if (dOpts.getInterfaceId() == null) {
            throw new IllegalArgumentException("Device interface cannot be null");
        }
        this.iface = device.getActiveUsbConfiguration().getUsbInterface(dOpts.getInterfaceId());
        if (iface == null) {
            throw new DeviceException(String.format("Could not find USB interface matching [ vendorId: '%s', productId: '%s', interface: '%s' ]",
                                                    "0x" + UsbUtil.toHexString(dOpts.getVendorId()),
                                                    "0x" + UsbUtil.toHexString(dOpts.getProductId()),
                                                    "0x" + UsbUtil.toHexString(dOpts.getInterfaceId())));
        }
        this.device = device;

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

    @Override
    public DeviceOptions getDeviceOptions() {
        return dOpts;
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

    public byte[] getFeatureReport(int responseSize, Byte reportId) throws DeviceException {
        throw new DeviceException("USB feature reports are not supported");
    }

    public void sendFeatureReport(byte[] data, Byte reportId) throws DeviceException {
        throw new DeviceException("USB feature reports are not supported");
    }

    /**
     * Data will be sent to or received from the open usb device, depending on the {@code endpoint} used.
     *
     * @param endpoint Endpoint on the usb device interface to pass data across
     * @param data     Byte array of data to send, or to be written from a receive
     */
    private synchronized void exchangeData(Byte endpoint, byte[] data) throws UsbException, DeviceException {
        if (endpoint == null) {
            throw new IllegalArgumentException("Interface endpoint cannot be null");
        }

        UsbEndpoint usbEndpoint = iface.getUsbEndpoint(endpoint);
        if(usbEndpoint == null) {
            throw new DeviceException(String.format("Could not find USB endpoint matching [ endpoint: '%s' ]",
                                                    "0x" + UsbUtil.toHexString(endpoint)));
        }
        UsbPipe pipe = usbEndpoint.getUsbPipe();
        if (!pipe.isOpen()) { pipe.open(); }

        try {
            pipe.syncSubmit(data);
        }
        finally {
            if(pipe != null) {
                pipe.close();
            }
        }
    }

    @Override
    public void close() {
        setStreaming(false);
        // Remove orphaned reference
        websocket.removeDevice(dOpts);
        if (iface.isClaimed()) {
            try {
                iface.release();
            }
            catch(UsbException e) {
                log.error("Unable to close USB device", e);
            }
        }
    }

}
