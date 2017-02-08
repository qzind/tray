package qz.ws;

import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.communication.DeviceException;
import qz.communication.DeviceIO;
import qz.communication.DeviceListener;
import qz.communication.SerialIO;
import qz.utils.UsbUtilities;

import java.util.HashMap;

public class SocketConnection {

    private static final Logger log = LoggerFactory.getLogger(SocketConnection.class);


    private Certificate certificate;

    private DeviceListener deviceListener;

    // serial port -> open SerialIO
    private final HashMap<String,SerialIO> openSerialPorts = new HashMap<>();

    //vendor id -> product id -> open DeviceIO
    private final HashMap<Short,HashMap<Short,DeviceIO>> openDevices = new HashMap<>();


    public SocketConnection(Certificate cert) {
        certificate = cert;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate newCert) {
        certificate = newCert;
    }


    public void addSerialPort(String port, SerialIO io) {
        openSerialPorts.put(port, io);
    }

    public SerialIO getSerialPort(String port) {
        return openSerialPorts.get(port);
    }

    public void removeSerialPort(String port) {
        openSerialPorts.remove(port);
    }


    public boolean isListening() {
        return deviceListener != null;
    }

    public void startListening(DeviceListener listener) {
        deviceListener = listener;
    }

    public void stopListening() {
        if (deviceListener != null) {
            deviceListener.close();
        }
        deviceListener = null;
    }


    public void addDevice(short vendor, short product, DeviceIO io) {
        HashMap<Short,DeviceIO> productMap = openDevices.get(vendor);
        if (productMap == null) {
            productMap = new HashMap<>();
        }

        productMap.put(product, io);
        openDevices.put(vendor, productMap);
    }

    public DeviceIO getDevice(String vendor, String product) {
        return getDevice(UsbUtilities.hexToShort(vendor), UsbUtilities.hexToShort(product));
    }

    public DeviceIO getDevice(Short vendor, Short product) {
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor ID cannot be null");
        }
        if (product == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        HashMap<Short,DeviceIO> productMap = openDevices.get(vendor);
        if (productMap != null) {
            return productMap.get(product);
        }

        return null;
    }

    public void removeDevice(Short vendor, Short product) {
        openDevices.get(vendor).remove(product);
    }

    public synchronized void openDevice(DeviceIO device, short vendorId, short productId) throws DeviceException {
        device.open();
        addDevice(vendorId, productId, device);
    }

    /**
     * Explicitly closes all open serial and usb connections setup through this object
     */
    public synchronized void disconnect() throws SerialPortException, DeviceException {
        log.info("Closing all communication channels for {}", certificate.getCommonName());

        for(String p : openSerialPorts.keySet()) {
            openSerialPorts.get(p).close();
        }

        for(Short v : openDevices.keySet()) {
            HashMap<Short,DeviceIO> pm = openDevices.get(v);
            for(Short p : pm.keySet()) {
                pm.get(p).close();
            }
        }

        stopListening();
    }

}
