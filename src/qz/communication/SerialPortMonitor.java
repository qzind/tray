package qz.communication;

import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import qz.ws.SocketConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SerialPortMonitor {
    private static final Logger log = LogManager.getLogger(SerialPortMonitor.class);

    private static final HashMap<String, SharedSerialPort> openPorts = new HashMap<>();
    private static final HashMap<SocketConnection, SerialSession> serialSessions = new HashMap<>();
    private static final HashMap<String, List<SocketConnection>> portListeners = new HashMap<>();

    public synchronized static boolean startListening(SocketConnection connection, Session session,
                                                       String portName, SerialOptions opts) throws SerialPortException {
        if (isListening(connection, portName)) {
            log.warn("Connection already listening to port [{}]", portName);
            return true;
        }

        SerialSession serialSession = serialSessions.computeIfAbsent(connection, c -> new SerialSession(session));

        SharedSerialPort sharedPort = openPorts.get(portName);
        if (sharedPort == null) {
            sharedPort = new SharedSerialPort(portName);
            final String pn = portName;
            sharedPort.setOnCloseCallback(() -> {
                synchronized (SerialPortMonitor.class) {
                    openPorts.remove(pn);
                    portListeners.remove(pn);
                    log.info("Shared port [{}] removed from monitor", pn);
                }
            });

            if (!sharedPort.open(opts)) {
                log.error("Failed to open serial port [{}]", portName);
                return false;
            }

            openPorts.put(portName, sharedPort);
            log.info("Opened new shared port [{}]", portName);
        } else {
            if (opts != null && opts.isPortSettingsExplicitlySet()) {
                SerialOptions.PortSettings existingSettings = sharedPort.getPortSettings();
                SerialOptions.PortSettings requestedSettings = opts.getPortSettings();

                if (requestedSettings != null && existingSettings != null && !requestedSettings.equals(existingSettings)) {
                    throw new SerialPortException(portName, "openPort",
                        "Port is already open with different settings. Remove explicit settings to join the shared port.");
                }
            }

            if (opts != null && opts.isEncodingExplicitlySet()) {
                SerialOptions.PortSettings existingSettings = sharedPort.getPortSettings();
                SerialOptions.PortSettings requestedSettings = opts.getPortSettings();

                if (requestedSettings != null && existingSettings != null &&
                    !requestedSettings.getEncoding().equals(existingSettings.getEncoding())) {
                    throw new SerialPortException(portName, "openPort",
                        "Port is already open with different encoding. Remove encoding option to join the shared port.");
                }
            }

            if (opts != null && opts.isRxExplicitlySet()) {
                SerialOptions.ResponseFormat existingFormat = sharedPort.getResponseFormat();
                SerialOptions.ResponseFormat requestedFormat = opts.getResponseFormat();

                if (requestedFormat != null && existingFormat != null && !requestedFormat.equals(existingFormat)) {
                    throw new SerialPortException(portName, "openPort",
                        "Port is already open with different rx settings. Remove rx options to join the shared port.");
                }
            }
        }

        sharedPort.addListener(connection, serialSession);
        portListeners.computeIfAbsent(portName, k -> new ArrayList<>()).add(connection);

        log.info("Connection now listening to port [{}], total listeners: {}", portName, sharedPort.getListenerCount());
        return true;
    }

    public synchronized static void stopListening(SocketConnection connection, String portName) {
        SharedSerialPort sharedPort = openPorts.get(portName);
        if (sharedPort == null) {
            return;
        }

        boolean portClosed = sharedPort.removeListener(connection);

        List<SocketConnection> listeners = portListeners.get(portName);
        if (listeners != null) {
            listeners.remove(connection);
            if (listeners.isEmpty()) {
                portListeners.remove(portName);
            }
        }

        if (portClosed) {
            log.info("Port [{}] closed (last listener removed)", portName);
        }
    }

    public synchronized static void stopListening(SocketConnection connection) {
        List<String> portsToRemove = new ArrayList<>();
        for (Map.Entry<String, List<SocketConnection>> entry : portListeners.entrySet()) {
            if (entry.getValue().contains(connection)) {
                portsToRemove.add(entry.getKey());
            }
        }

        for (String portName : portsToRemove) {
            stopListening(connection, portName);
        }

        serialSessions.remove(connection);
    }

    public synchronized static boolean isListening(SocketConnection connection, String portName) {
        List<SocketConnection> listeners = portListeners.get(portName);
        return listeners != null && listeners.contains(connection);
    }

    public synchronized static void sendData(SocketConnection connection, String portName,
                                              JSONObject params, SerialOptions opts)
            throws JSONException, IOException, SerialPortException {

        if (!isListening(connection, portName)) {
            throw new SerialPortException(portName, "sendData", "Connection is not listening to this port");
        }

        SharedSerialPort sharedPort = openPorts.get(portName);
        if (sharedPort == null || !sharedPort.isOpen()) {
            throw new SerialPortException(portName, "sendData", "Port is not open");
        }

        sharedPort.sendData(params, opts);
    }

    public synchronized static int getListenerCount(String portName) {
        SharedSerialPort sharedPort = openPorts.get(portName);
        return sharedPort != null ? sharedPort.getListenerCount() : 0;
    }

    public synchronized static boolean isPortOpen(String portName) {
        SharedSerialPort sharedPort = openPorts.get(portName);
        return sharedPort != null && sharedPort.isOpen();
    }

    public synchronized static List<String> getPortsForConnection(SocketConnection connection) {
        List<String> ports = new ArrayList<>();
        for (Map.Entry<String, List<SocketConnection>> entry : portListeners.entrySet()) {
            if (entry.getValue().contains(connection)) {
                ports.add(entry.getKey());
            }
        }
        return ports;
    }
}
