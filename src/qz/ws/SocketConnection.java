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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SocketConnection {

    private static final Logger log = LogManager.getLogger(SocketConnection.class);


    private Certificate certificate;

    private DeviceListener deviceListener;

    // serial port -> open SerialIO
    private final HashMap<String,SerialIO> openSerialPorts = new HashMap<>();
    // socket 'host:port' -> open ProtocolIO
    private final HashMap<String,SocketIO> openNetworkSockets = new HashMap<>();

    // absolute path -> open file listener
    private final HashMap<Path,FileIO> openFiles = new HashMap<>();

    // DeviceOptions -> open DeviceIO
    private final HashMap<DeviceOptions,DeviceIO> openDevices = new HashMap<>();

    private final HashMap<UUID, Ipp.ServerEntry> ippServers = new HashMap<>();


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

    public void addServerEntry(UUID uuid, Ipp.ServerEntry serverEntry) {
        ippServers.put(uuid, serverEntry);
    }

    public Ipp.ServerEntry getServerEntry(UUID uuid) {
        return ippServers.get(uuid);
    }

    public UUID getUuid(Ipp.ServerEntry serverEntry) {
        for (Map.Entry<UUID,Ipp.ServerEntry> entry : ippServers.entrySet()) {
            if (entry.getValue().equals(serverEntry)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeServerEntry(UUID uuid) {
        ippServers.remove(uuid);
    }


    public void addNetworkSocket(String location, SocketIO io) {
        openNetworkSockets.put(location, io);
    }

    public SocketIO getNetworkSocket(String location) {
        return openNetworkSockets.get(location);
    }

    public void removeNetworkSocket(String location) {
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

    public void addFileListener(Path absolute, FileIO listener) {
        openFiles.put(absolute, listener);
    }

    public FileIO getFileListener(Path absolute) {
        return openFiles.get(absolute);
    }

    public void removeFileListener(Path absolute) {
        openFiles.remove(absolute);
    }

    public void removeAllFileListeners() {
        for(Path path : openFiles.keySet()) {
            openFiles.get(path).close();
            FileWatcher.deregisterWatch(openFiles.get(path));
        }

        openFiles.clear();
    }


    public void addDevice(DeviceOptions dOpts, DeviceIO io) {
        openDevices.put(dOpts, io);
    }

    public DeviceIO getDevice(DeviceOptions dOpts) {
        return openDevices.get(dOpts);
    }

    public void removeDevice(DeviceOptions dOpts) {
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