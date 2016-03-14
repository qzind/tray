package qz.ws;

import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.communication.SerialIO;
import qz.communication.UsbIO;
import qz.utils.UsbUtilities;

import javax.usb.UsbException;
import java.util.HashMap;

public class SocketConnection {

    private static final Logger log = LoggerFactory.getLogger(SocketConnection.class);


    private Certificate certificate;

    // serial port -> open SerialIO
    private final HashMap<String,SerialIO> openSerialPorts = new HashMap<>();

    //vendor id -> product id -> open UsbIO
    private final HashMap<Short,HashMap<Short,UsbIO>> openUsbDevices = new HashMap<>();


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


    public void addUsbDevice(short vendor, short product, UsbIO io) {
        HashMap<Short,UsbIO> productMap = openUsbDevices.get(vendor);
        if (productMap == null) {
            productMap = new HashMap<>();
        }

        productMap.put(product, io);
        openUsbDevices.put(vendor, productMap);
    }

    public UsbIO getUsbDevice(String vendor, String product) {
        return getUsbDevice(UsbUtilities.hexToShort(vendor), UsbUtilities.hexToShort(product));
    }

    public UsbIO getUsbDevice(short vendor, short product) {
        HashMap<Short,UsbIO> productMap = openUsbDevices.get(vendor);
        if (productMap != null) {
            return productMap.get(product);
        }

        return null;
    }

    public void removeUsbDevice(short vendor, short product) {
        openUsbDevices.get(vendor).remove(product);
    }


    /**
     * Explicitly closes all open serial and usb connections setup through this object
     */
    public void disconnect() throws SerialPortException, UsbException {
        log.info("Closing all communication channels for {}", certificate.getCommonName());

        for(String p : openSerialPorts.keySet()) {
            openSerialPorts.get(p).close();
        }

        for(Short v : openUsbDevices.keySet()) {
            HashMap<Short,UsbIO> pm = openUsbDevices.get(v);
            for(Short p : pm.keySet()) {
                pm.get(p).close();
            }
        }
    }

}
