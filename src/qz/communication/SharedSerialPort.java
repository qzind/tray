package qz.communication;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.common.ByteArrayBuilder;
import qz.utils.ByteUtilities;
import qz.utils.DeviceUtilities;
import qz.ws.SocketConnection;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedSerialPort implements SerialPortEventListener {
    private static final Logger log = LogManager.getLogger(SharedSerialPort.class);

    private static final int READ_TIMEOUT = 1200;

    private final String portName;
    private SerialPort port;
    private SerialOptions serialOpts;
    private final ByteArrayBuilder data = new ByteArrayBuilder();

    private final Map<SocketConnection, SerialSession> listeners = new HashMap<>();
    private Runnable onCloseCallback;

    public SharedSerialPort(String portName) {
        this.portName = portName;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public synchronized boolean open(SerialOptions opts) throws SerialPortException {
        if (isOpen()) {
            log.warn("Serial port [{}] is already open", portName);
            return true;
        }

        port = new SerialPort(portName);
        port.openPort();

        serialOpts = new SerialOptions();
        applyOptions(opts);
        port.addEventListener(this);

        log.info("Shared serial port [{}] opened", portName);
        return port.isOpened();
    }

    public synchronized boolean isOpen() {
        return port != null && port.isOpened();
    }

    public synchronized SerialOptions.PortSettings getPortSettings() {
        return serialOpts != null ? serialOpts.getPortSettings() : null;
    }

    public synchronized SerialOptions.ResponseFormat getResponseFormat() {
        return serialOpts != null ? serialOpts.getResponseFormat() : null;
    }

    public synchronized void addListener(SocketConnection connection, SerialSession session) {
        if (listeners.containsKey(connection)) {
            log.warn("Connection already listening to port [{}]", portName);
            return;
        }

        listeners.put(connection, session);
        log.info("Added listener to shared port [{}], now {} listeners", portName, listeners.size());
    }

    public synchronized boolean removeListener(SocketConnection connection) {
        if (listeners.remove(connection) != null) {
            log.info("Removed listener from shared port [{}], now {} listeners", portName, listeners.size());
        }

        if (listeners.isEmpty()) {
            close();
            return true;
        }
        return false;
    }

    public synchronized int getListenerCount() {
        return listeners.size();
    }

    public synchronized boolean hasListener(SocketConnection connection) {
        return listeners.containsKey(connection);
    }

    public synchronized void sendData(JSONObject params, SerialOptions opts) throws JSONException, IOException, SerialPortException {
        if (!isOpen()) {
            throw new SerialPortException(portName, "sendData", "Port is not open");
        }

        Charset encoding = serialOpts.getPortSettings().getEncoding();
        if (opts != null && opts.getPortSettings() != null) {
            SerialOptions.PortSettings requested = opts.getPortSettings();
            SerialOptions.PortSettings current = serialOpts.getPortSettings();
            if (requested.getBaudRate() != current.getBaudRate() ||
                requested.getDataBits() != current.getDataBits() ||
                requested.getStopBits() != current.getStopBits() ||
                requested.getParity() != current.getParity() ||
                requested.getFlowControl() != current.getFlowControl()) {
                log.warn("sendData options for port [{}] contain port settings that are ignored on shared ports", portName);
            }
            encoding = requested.getEncoding();
        }

        if (opts != null && opts.getResponseFormat() != null) {
            log.warn("sendData options for port [{}] contain rx settings that are ignored on shared ports", portName);
        }

        log.debug("Sending data over shared port [{}]", portName);
        port.writeBytes(DeviceUtilities.getDataBytes(params, encoding));
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        String output = processSerialEvent(event);
        if (output != null) {
            log.debug("Received serial output on [{}]: {}", portName, output);
            dispatchToListeners(output);
        }
    }

    private void dispatchToListeners(String output) {
        List<Map.Entry<SocketConnection, SerialSession>> listenersCopy;
        synchronized (this) {
            listenersCopy = new ArrayList<>(listeners.entrySet());
        }

        for (Map.Entry<SocketConnection, SerialSession> entry : listenersCopy) {
            SocketConnection connection = entry.getKey();
            SerialSession session = entry.getValue();
            session.sendSerialEvent(portName, output, () -> {
                log.warn("Failed to send to listener on port [{}], removing", portName);
                SerialPortMonitor.stopListening(connection, portName);
            });
        }
    }

    private String processSerialEvent(SerialPortEvent event) {
        SerialOptions.ResponseFormat format = serialOpts.getResponseFormat();

        try {
            if (event.isRXCHAR()) {
                data.append(port.readBytes(event.getEventValue(), READ_TIMEOUT));

                String response = null;
                if (format.isBoundNewline()) {
                    response = processNewlineDelimited(format);
                } else if (format.getBoundStart() != null && format.getBoundStart().length > 0) {
                    response = processFormatted(format);
                } else if (format.getFixedWidth() > 0) {
                    response = processFixedWidth(format);
                } else {
                    log.trace("Reading raw response");
                    response = new String(data.toByteArray(), format.getEncoding());
                    data.clear();
                }

                return response;
            }
        } catch (SerialPortException e) {
            log.error("Exception occurred while reading data from port.", e);
        } catch (Exception e) {
            log.error("Error processing serial event.", e);
        }

        return null;
    }

    private String processNewlineDelimited(SerialOptions.ResponseFormat format) {
        Integer endIdx = ByteUtilities.firstMatchingIndex(data.toByteArray(), new byte[] {'\r', '\n'});
        int delimSize = 2;

        if (endIdx == null) {
            Integer crIdx = ByteUtilities.firstMatchingIndex(data.toByteArray(), new byte[] {'\r'});
            Integer nlIdx = ByteUtilities.firstMatchingIndex(data.toByteArray(), new byte[] {'\n'});
            endIdx = minNullable(crIdx, nlIdx);
            delimSize = 1;
        }

        if (endIdx != null) {
            log.trace("Reading newline-delimited response");
            byte[] output = new byte[endIdx];
            System.arraycopy(data.toByteArray(), 0, output, 0, endIdx);
            String buffer = new String(output, format.getEncoding());

            if (!buffer.isEmpty()) {
                data.clearRange(0, endIdx + delimSize);
                return buffer;
            }
            data.clearRange(0, endIdx + delimSize);
        }
        return null;
    }

    private String processFormatted(SerialOptions.ResponseFormat format) {
        Integer startIdx = ByteUtilities.firstMatchingIndex(data.toByteArray(), format.getBoundStart());

        if (startIdx != null) {
            int startOffset = startIdx + format.getBoundStart().length;
            int copyLength = 0;
            int endIdx = 0;

            if (format.getBoundEnd() != null && format.getBoundEnd().length > 0) {
                Integer boundEnd = ByteUtilities.firstMatchingIndex(data.toByteArray(), format.getBoundEnd(), startIdx);
                if (boundEnd != null) {
                    log.trace("Reading bounded response");
                    copyLength = boundEnd - startOffset;
                    endIdx = boundEnd + 1;
                    if (format.isIncludeStart()) {
                        copyLength += format.getBoundEnd().length;
                    }
                }
            } else if (format.getFixedWidth() > 0) {
                log.trace("Reading fixed length prefixed response");
                copyLength = format.getFixedWidth();
                endIdx = startOffset + format.getFixedWidth();
            } else if (format.getLength() != null) {
                SerialOptions.ByteParam lengthParam = format.getLength();
                if (data.getLength() > startOffset + lengthParam.getIndex() + lengthParam.getLength()) {
                    log.trace("Reading dynamic formatted response");
                    int expectedLength = ByteUtilities.parseBytes(data.toByteArray(),
                            startOffset + lengthParam.getIndex(), lengthParam.getLength(), lengthParam.getEndian());
                    startOffset += lengthParam.getIndex() + lengthParam.getLength();
                    copyLength = expectedLength;
                    endIdx = startOffset + copyLength;

                    if (format.getCrc() != null) {
                        SerialOptions.ByteParam crcParam = format.getCrc();
                        int expand = crcParam.getIndex() + crcParam.getLength();
                        copyLength += expand;
                        endIdx += expand;
                    }
                }
            } else {
                log.warn("Reading header formatted raw response, are you missing an rx option?");
                copyLength = data.getLength() - startOffset;
                endIdx = data.getLength();
            }

            if (copyLength > 0 && data.getLength() >= endIdx) {
                log.debug("Response format readable, starting copy");

                if (format.isIncludeStart()) {
                    copyLength += (startOffset - startIdx);
                    startOffset = startIdx;
                }

                byte[] responseData = new byte[copyLength];
                System.arraycopy(data.toByteArray(), startOffset, responseData, 0, copyLength);

                String response = new String(responseData, format.getEncoding());
                data.clearRange(startIdx, endIdx);
                return response;
            }
        }
        return null;
    }

    private String processFixedWidth(SerialOptions.ResponseFormat format) {
        if (data.getLength() >= format.getFixedWidth()) {
            log.trace("Reading fixed length response");
            byte[] output = new byte[format.getFixedWidth()];
            System.arraycopy(data.toByteArray(), 0, output, 0, format.getFixedWidth());

            String response = new String(output, format.getEncoding());
            data.clearRange(0, format.getFixedWidth());
            return response;
        }
        return null;
    }

    private void applyOptions(SerialOptions opts) throws SerialPortException {
        if (opts == null) return;

        SerialOptions.PortSettings ps = opts.getPortSettings();
        if (ps != null && !ps.equals(serialOpts.getPortSettings())) {
            log.debug("Applying port settings");
            port.setParams(ps.getBaudRate(), ps.getDataBits(), ps.getStopBits(), ps.getParity());
            port.setFlowControlMode(ps.getFlowControl());
            serialOpts.setPortSettings(ps);
        }

        SerialOptions.ResponseFormat rf = opts.getResponseFormat();
        if (rf != null) {
            log.debug("Applying response formatting");
            serialOpts.setResponseFormat(rf);
        }
    }

    public synchronized void close() {
        if (port == null) {
            return;
        }

        log.info("Closing shared serial port [{}]", portName);

        try {
            if (port.isOpened()) {
                port.closePort();
            }
        } catch (SerialPortException e) {
            log.warn("Error closing serial port [{}]: {}", portName, e.getMessage());
        }

        port = null;

        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private Integer minNullable(Integer a, Integer b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }
}
