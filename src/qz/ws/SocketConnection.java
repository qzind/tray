package qz.ws;

import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.auth.Certificate;
import qz.communication.*;
import qz.printer.status.StatusMonitor;
import qz.utils.FileWatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class SocketConnection {

    private static final Logger log = LogManager.getLogger(SocketConnection.class);


    private Certificate certificate;

    private volatile DeviceListener deviceListener;

    // serial port -> open SerialIO
    private final ConcurrentHashMap<String,SerialIO> openSerialPorts = new ConcurrentHashMap<>();
    // socket 'host:port' -> open ProtocolIO
    private final ConcurrentHashMap<String,SocketIO> openNetworkSockets = new ConcurrentHashMap<>();

    // absolute path -> open file listener
    private final ConcurrentHashMap<Path,FileIO> openFiles = new ConcurrentHashMap<>();

    // DeviceOptions -> open DeviceIO
    private final ConcurrentHashMap<DeviceOptions,DeviceIO> openDevices = new ConcurrentHashMap<>();


    public SocketConnection(Certificate cert) {
        certificate = cert;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate newCert) {
        certificate = newCert;
    }


    public synchronized void addSerialPort(String port, SerialIO io) {
        openSerialPorts.put(port, io);
    }

    public synchronized SerialIO getSerialPort(String port) {
        return openSerialPorts.get(port);
    }

    public synchronized void removeSerialPort(String port) {
        openSerialPorts.remove(port);
    }


    public synchronized void addNetworkSocket(String location, SocketIO io) {
        openNetworkSockets.put(location, io);
    }

    public synchronized SocketIO getNetworkSocket(String location) {
        return openNetworkSockets.get(location);
    }

    public synchronized void removeNetworkSocket(String location) {
        openNetworkSockets.remove(location);
    }


    public boolean isDeviceListening() {
        return deviceListener != null;
    }

    public void startDeviceListening(DeviceListener listener) {
        deviceListener = listener;
    }

    public void stopDeviceListening() {
        if (deviceListener != null) {
            deviceListener.close();
        }
        deviceListener = null;
    }

    public synchronized void addFileListener(Path absolute, FileIO listener) {
        openFiles.put(absolute, listener);
    }

    public synchronized FileIO getFileListener(Path absolute) {
        return openFiles.get(absolute);
    }

    public synchronized void removeFileListener(Path absolute) {
        openFiles.remove(absolute);
    }

    public synchronized void removeAllFileListeners() {
        for(FileIO io : openFiles.values()) {
            if (io != null) {
                io.close();
                FileWatcher.deregisterWatch(io);
            }
        }
        openFiles.clear();
    }


    public synchronized void addDevice(DeviceOptions dOpts, DeviceIO io) {
        openDevices.put(dOpts, io);
    }

    public synchronized DeviceIO getDevice(DeviceOptions dOpts) {
        return openDevices.get(dOpts);
    }

    public synchronized void removeDevice(DeviceOptions dOpts) {
        openDevices.remove(dOpts);
    }

    public synchronized void openDevice(DeviceIO device, DeviceOptions dOpts) throws DeviceException {
        device.open();
        if (device.isOpen()) {
            addDevice(dOpts, device);
        }
    }

    /**
     * Explicitly closes all open serial and usb connections setup through this object
     */
    public synchronized void disconnect() throws SerialPortException, DeviceException, IOException {
        log.info("Closing all communication channels for {}", certificate.getCommonName());

        for(SerialIO sio : openSerialPorts.values()) {
            sio.close();
        }

        for(SocketIO pio : openNetworkSockets.values()) {
            pio.close();
        }

        for(DeviceIO dio : openDevices.values()) {
            dio.setStreaming(false);
            dio.close();
        }

        removeAllFileListeners();
        stopDeviceListening();
        StatusMonitor.stopListening(this);
    }

}