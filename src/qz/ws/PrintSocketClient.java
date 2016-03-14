package qz.ws;

import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.auth.Certificate;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.communication.SerialIO;
import qz.communication.SerialProperties;
import qz.communication.UsbIO;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.PrintServiceMatcher;
import qz.printer.action.PrintProcessor;
import qz.utils.NetworkUtilities;
import qz.utils.PrintingUtilities;
import qz.utils.SerialUtilities;
import qz.utils.UsbUtilities;

import javax.print.PrintServiceLookup;
import javax.security.cert.CertificateParsingException;
import javax.usb.UsbException;
import javax.usb.util.UsbUtil;
import java.awt.print.PrinterAbortException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;


@WebSocket
public class PrintSocketClient {

    private static final Logger log = LoggerFactory.getLogger(PrintSocketClient.class);

    private final TrayManager trayManager = PrintSocketServer.getTrayManager();
    private static final AtomicBoolean dialogOpen = new AtomicBoolean(false);

    //websocket port -> Connection
    private static final HashMap<Integer,SocketConnection> openConnections = new HashMap<>();

    private enum StreamType {
        SERIAL, USB
    }

    private enum Method {
        PRINTERS_GET_DEFAULT("printers.getDefault", true, "access connected printers"),
        PRINTERS_FIND("printers.find", true, "access connected printers"),
        PRINT("print", true, "print to %s"),

        SERIAL_FIND_PORTS("serial.findPorts", true, "access serial ports"),
        SERIAL_OPEN_PORT("serial.openPort", true, "open a serial port"),
        SERIAL_SEND_DATA("serial.sendData", true, "send data over a serial port"),
        SERIAL_CLOSE_PORT("serial.closePort", true, "close a serial port"),

        USB_LIST_DEVICES("usb.listDevices", true, "access USB devices"),
        USB_LIST_INTERFACES("usb.listInterfaces", true, "access USB devices"),
        USB_LIST_ENDPOINTS("usb.listEndpoints", true, "access USB devices"),
        USB_CLAIM_DEVICE("usb.claimDevice", true, "claim a USB device"),
        USB_SEND_DATA("usb.sendData", true, "use a USB device"),
        USB_READ_DATA("usb.readData", true, "use a USB device"),
        USB_OPEN_STREAM("usb.openStream", true, "use a USB device"),
        USB_CLOSE_STREAM("usb.closeStream", true, "use a USB device"),
        USB_RELEASE_DEVICE("usb.releaseDevice", true, "release a USB device"),

        WEBSOCKET_GET_NETWORK_INFO("websocket.getNetworkInfo", true),
        GET_VERSION("getVersion", false),

        INVALID("", false);


        private String callName;
        private String dialogPrompt;
        private boolean dialogShown;

        Method(String callName, boolean dialogShown) {
            this(callName, dialogShown, "access local resources");
        }

        Method(String callName, boolean dialogShown, String dialogPrompt) {
            this.callName = callName;

            this.dialogShown = dialogShown;
            this.dialogPrompt = dialogPrompt;
        }

        public boolean isDialogShown() {
            return dialogShown;
        }

        public String getDialogPrompt() {
            return dialogPrompt;
        }

        public static Method findFromCall(String call) {
            for(Method m : Method.values()) {
                if (m.callName.equals(call)) {
                    return m;
                }
            }

            return INVALID;
        }
    }


    @OnWebSocketConnect
    public void onConnect(Session session) {
        log.info("Connection opened from {} on socket port {}", session.getRemoteAddress(), session.getLocalAddress().getPort());
        trayManager.displayInfoMessage("Client connected");

        //new connections are unknown until they send a proper certificate
        openConnections.put(session.getRemoteAddress().getPort(), new SocketConnection(Certificate.UNKNOWN));
    }

    @OnWebSocketClose
    public void onClose(Session session, int closeCode, String reason) {
        log.info("Connection closed: {} - {}", closeCode, reason);
        trayManager.displayInfoMessage("Client disconnected");

        Integer port = session.getRemoteAddress().getPort();
        SocketConnection closed = openConnections.remove(port);
        if (closed != null) {
            try {
                closed.disconnect();
            }
            catch(Exception e) {
                log.error("Failed to close communication channel", e);
            }
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        log.error("Connection error", error);
        trayManager.displayErrorMessage(error.getMessage());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, Reader reader) throws IOException {
        String message = IOUtils.toString(reader);

        if (message == null || message.isEmpty()) {
            sendError(session, null, "Message is empty");
            return;
        }
        if (Constants.PROBE_REQUEST.equals(message)) {
            try { session.getRemote().sendString(Constants.PROBE_RESPONSE); } catch(Exception ignore) {}
            log.warn("Second instance of {} likely detected, asking it to close", Constants.ABOUT_TITLE);
            return;
        }
        if ("ping".equals(message)) { return; } //keep-alive call / no need to process

        String UID = null;
        try {
            log.debug("Message: {}", message);
            JSONObject json = new JSONObject(message);
            UID = json.optString("uid");

            Integer connectionPort = session.getRemoteAddress().getPort();
            SocketConnection connection = openConnections.get(connectionPort);
            Certificate certificate = connection.getCertificate();

            //if sent a certificate use that instead for this connection
            if (json.has("certificate")) {
                try {
                    certificate = new Certificate(json.optString("certificate"));

                    connection.setCertificate(certificate);
                    log.debug("Received new certificate from connection through {}", connectionPort);
                }
                catch(CertificateParsingException ignore) {}

                if (allowedFromDialog(certificate, "connect to QZ")) {
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, "Connection blocked by client");
                    session.disconnect();
                }

                return; //this is a setup call, so no further processing is needed
            }

            //check request signature
            if (certificate != Certificate.UNKNOWN) {
                if (json.optLong("timestamp") + Constants.VALID_SIGNING_PERIOD < System.currentTimeMillis()
                        || json.optLong("timestamp") - Constants.VALID_SIGNING_PERIOD > System.currentTimeMillis()) {
                    //bad timestamps use the expired certificate
                    log.warn("Expired signature on request");
                    Certificate.EXPIRED.adjustStaticCertificate(certificate);
                    certificate = Certificate.EXPIRED;
                } else if (json.isNull("signature") || !validSignature(certificate, json)) {
                    //bad signatures use the unsigned certificate
                    log.warn("Bad signature on request");
                    Certificate.UNSIGNED.adjustStaticCertificate(certificate);
                    certificate = Certificate.UNSIGNED;
                } else {
                    log.trace("Valid signature from {}", certificate.getCommonName());
                }
            }

            processMessage(session, json, connection, certificate);
        }
        catch(JSONException e) {
            log.error("Bad JSON: {}", e.getMessage());
            sendError(session, UID, e);
        }
        catch(Exception e) {
            log.error("Problem processing message", e);
            sendError(session, UID, e);
        }
    }

    private boolean validSignature(Certificate certificate, JSONObject message) throws JSONException {
        JSONObject copy = new JSONObject(message, new String[] {"call", "params", "timestamp"});
        String signature = message.optString("signature");

        return certificate.isSignatureValid(signature, copy.toString().replaceAll("\\\\/", "/"));
    }

    /**
     * Determine which method was called from web API
     *
     * @param session WebSocket session
     * @param json    JSON received from web API
     */
    private void processMessage(Session session, JSONObject json, SocketConnection connection, Certificate shownCertificate) throws JSONException, SerialPortException, UsbException {
        String UID = json.optString("uid");
        Method call = Method.findFromCall(json.optString("call"));
        JSONObject params = json.optJSONObject("params");
        if (params == null) { params = new JSONObject(); }

        if (call == Method.INVALID && (UID == null || UID.isEmpty())) {
            //incorrect message format, likely incompatible qz version
            session.close(4003, "Connected to incompatible QZ Tray version");
            return;
        }

        String prompt = call.getDialogPrompt();
        if (call == Method.PRINT) {
            //special formatting for print dialogs
            JSONObject pr = params.optJSONObject("printer");
            if (pr != null) {
                prompt = String.format(prompt, pr.optString("name", pr.optString("file", pr.optString("host", "an undefined location"))));
            } else {
                sendError(session, UID, "A printer must be specified before printing");
                return;
            }
        }

        if (call.isDialogShown() && !allowedFromDialog(shownCertificate, prompt)) {
            sendError(session, UID, "Request blocked");
            return;
        }


        //call appropriate methods
        switch(call) {
            case PRINTERS_GET_DEFAULT:
                sendResult(session, UID, PrintServiceLookup.lookupDefaultPrintService().getName());
                break;
            case PRINTERS_FIND:
                if (params.has("query")) {
                    String name = PrintServiceMatcher.getPrinterJSON(params.getString("query"));
                    if (name != null) {
                        sendResult(session, UID, name);
                    } else {
                        sendError(session, UID, "Specified printer could not be found.");
                    }
                } else {
                    sendResult(session, UID, PrintServiceMatcher.getPrintersJSON());
                }
                break;

            case PRINT:
                processPrintRequest(session, UID, params);
                break;

            case SERIAL_FIND_PORTS:
                sendResult(session, UID, SerialUtilities.getSerialPortsJSON());
                break;
            case SERIAL_OPEN_PORT:
                setupSerialPort(session, UID, connection, params);
                break;
            case SERIAL_SEND_DATA: {
                SerialProperties props = new SerialProperties(params.optJSONObject("properties"));
                SerialIO serial = connection.getSerialPort(params.optString("port"));
                if (serial != null) {
                    serial.sendData(props, params.optString("data"));
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("Serial port [%s] must be opened first.", params.optString("port")));
                }
                break;
            }
            case SERIAL_CLOSE_PORT: {
                SerialIO serial = connection.getSerialPort(params.optString("port"));
                if (serial != null) {
                    serial.close();
                    connection.removeSerialPort(params.optString("port"));
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("Serial port [%s] is not open.", params.optString("port")));
                }
                break;
            }

            case USB_LIST_DEVICES:
                sendResult(session, UID, UsbUtilities.getUsbDevicesJSON(params.getBoolean("includeHubs")));
                break;
            case USB_LIST_INTERFACES:
                sendResult(session, UID, UsbUtilities.getDeviceInterfacesJSON(UsbUtilities.hexToShort(params.getString("vendorId")),
                                                                              UsbUtilities.hexToShort(params.getString("productId"))));
                break;
            case USB_LIST_ENDPOINTS:
                sendResult(session, UID, UsbUtilities.getInterfaceEndpointsJSON(UsbUtilities.hexToShort(params.getString("vendorId")),
                                                                                UsbUtilities.hexToShort(params.getString("productId")),
                                                                                UsbUtilities.hexToByte(params.getString("interface"))));
                break;
            case USB_CLAIM_DEVICE: {
                short vendorId = UsbUtilities.hexToShort(params.optString("vendorId"));
                short productId = UsbUtilities.hexToShort(params.optString("productId"));

                if (connection.getUsbDevice(params.optString("vendorId"), params.optString("productId")) == null) {
                    UsbIO usb = new UsbIO(vendorId, productId);
                    usb.open(UsbUtilities.hexToByte(params.optString("interface")));
                    connection.addUsbDevice(vendorId, productId, usb);

                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] is already claimed.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }
            case USB_SEND_DATA: {
                UsbIO usb = connection.getUsbDevice(params.optString("vendorId"), params.optString("productId"));
                if (usb != null) {
                    usb.sendData(UsbUtilities.hexToByte(params.optString("endpoint")), StringUtils.getBytesUtf8(params.optString("data")));
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] must be claimed first.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }
            case USB_READ_DATA: {
                UsbIO usb = connection.getUsbDevice(params.optString("vendorId"), params.optString("productId"));
                if (usb != null) {
                    byte[] response = usb.readData(UsbUtilities.hexToByte(params.optString("endpoint")), params.optInt("responseSize"));
                    JSONArray hex = new JSONArray();
                    for(byte b : response) {
                        hex.put(UsbUtil.toHexString(b));
                    }
                    sendResult(session, UID, hex);
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] must be claimed first.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }
            case USB_OPEN_STREAM: {
                setupUsbStream(session, UID, connection, params);
                break;
            }
            case USB_CLOSE_STREAM: {
                UsbIO usb = connection.getUsbDevice(params.optString("vendorId"), params.optString("productId"));
                if (usb != null && usb.isStreaming()) {
                    usb.setStreaming(false);
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] is not streaming data.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }
            case USB_RELEASE_DEVICE: {
                UsbIO usb = connection.getUsbDevice(params.optString("vendorId"), params.optString("productId"));
                if (usb != null) {
                    usb.close();
                    connection.removeUsbDevice(UsbUtilities.hexToShort(params.optString("vendorId")), UsbUtilities.hexToShort(params.optString("productId")));

                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] is not claimed.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }

            case WEBSOCKET_GET_NETWORK_INFO:
                sendResult(session, UID, NetworkUtilities.getNetworkJSON());
                break;
            case GET_VERSION:
                sendResult(session, UID, Constants.VERSION);
                break;

            case INVALID: default:
                sendError(session, UID, "Invalid function call: " + json.optString("call", "NONE"));
                break;
        }
    }

    private boolean allowedFromDialog(Certificate certificate, String prompt) {
        //wait until previous prompts are closed
        while(dialogOpen.get()) {
            try { Thread.sleep(1000); } catch(Exception ignore) {}
        }

        dialogOpen.set(true);
        //prompt user for access
        boolean allowed = trayManager.showGatewayDialog(certificate, prompt);
        dialogOpen.set(false);

        return allowed;
    }

    /**
     * Determine print variables and send data to printer
     *
     * @param session WebSocket session
     * @param UID     ID of call from web API
     * @param params  Params of call from web API
     */
    private void processPrintRequest(Session session, String UID, JSONObject params) {
        try {
            PrintOutput output = new PrintOutput(params.optJSONObject("printer"));
            PrintOptions options = new PrintOptions(params.optJSONObject("options"));

            PrintProcessor processor = PrintingUtilities.getPrintProcessor(params.getJSONArray("data"));
            log.debug("Using {} to print", processor.getClass().getName());

            processor.parseData(params.optJSONArray("data"), options);
            processor.print(output, options);
            log.info("Printing complete");

            sendResult(session, UID, null);
        }
        catch(PrinterAbortException e) {
            log.warn("Printing cancelled");
            sendError(session, UID, "Printing cancelled");
        }
        catch(Exception e) {
            log.error("Failed to print", e);
            sendError(session, UID, e);
        }
    }

    private void setupSerialPort(final Session session, String UID, SocketConnection connection, JSONObject params) throws JSONException {
        final String portName = params.getString("port");
        if (connection.getSerialPort(portName) != null) {
            sendError(session, UID, String.format("Serial port [%s] is already open.", portName));
            return;
        }

        final SerialIO serial;
        JSONObject bounds = params.getJSONObject("bounds");
        if (bounds.isNull("width")) {
            serial = new SerialIO(portName,
                                  SerialUtilities.characterBytes(bounds.optString("start", "0x0002")),
                                  SerialUtilities.characterBytes(bounds.optString("end", "0x000D")));
        } else {
            serial = new SerialIO(portName, bounds.getInt("width"));
        }

        try {
            if (serial.open()) {
                connection.addSerialPort(portName, serial);

                //apply listener here, so we can send all replies to the browser
                serial.applyPortListener(new SerialPortEventListener() {
                    public void serialEvent(SerialPortEvent spe) {
                        String output = serial.processSerialEvent(spe);
                        log.debug("Received serial output: {}", output);

                        sendStream(session, StreamType.SERIAL, portName, output);
                    }
                });

                sendResult(session, UID, null);
            } else {
                sendError(session, UID, String.format("Unable to open serial port [%s]", portName));
            }
        }
        catch(SerialPortException e) {
            sendError(session, UID, e);
        }
    }

    private void setupUsbStream(final Session session, String UID, SocketConnection connection, final JSONObject params) throws JSONException {
        final UsbIO usb = connection.getUsbDevice(params.optString("vendorId"), params.optString("productId"));

        if (usb != null) {
            if (!usb.isStreaming()) {
                usb.setStreaming(true);

                new Thread() {
                    @Override
                    public void run() {
                        int interval = params.optInt("interval", 100);
                        byte endpoint = UsbUtilities.hexToByte(params.optString("endpoint"));
                        int size = params.optInt("responseSize");

                        JSONArray streamKey = new JSONArray();
                        streamKey.put(usb.getVendorId())
                                .put(usb.getProductId())
                                .put(usb.getInterface())
                                .put(UsbUtil.toHexString(endpoint));

                        try {
                            while(usb.isOpen() && usb.isStreaming()) {
                                byte[] response = usb.readData(endpoint, size);
                                JSONArray hex = new JSONArray();
                                for(byte b : response) {
                                    hex.put(UsbUtil.toHexString(b));
                                }
                                sendStream(session, StreamType.USB, streamKey, hex);

                                try { Thread.sleep(interval); } catch(Exception ignore) {}
                            }
                        }
                        catch(UsbException e) {
                            usb.setStreaming(false);
                            sendStreamError(session, StreamType.USB, streamKey, e);
                        }
                    }
                }.start();

                sendResult(session, UID, null);
            } else {
                sendError(session, UID, String.format("USB Device [v:%s p:%s] is already streaming data.", params.opt("vendorId"), params.opt("productId")));
            }
        } else {
            sendError(session, UID, String.format("USB Device [v:%s p:%s] must be claimed first.", params.opt("vendorId"), params.opt("productId")));
        }
    }


    /**
     * Send JSON reply to web API for call {@code messageUID}
     *
     * @param session     WebSocket session
     * @param messageUID  ID of call from web API
     * @param returnValue Return value of method call, can be {@code null}
     */
    private void sendResult(Session session, String messageUID, Object returnValue) {
        try {
            JSONObject reply = new JSONObject();
            reply.put("uid", messageUID);
            reply.put("result", returnValue);
            send(session, reply);
        }
        catch(JSONException e) {
            log.error("Send result failed", e);
        }
    }

    /**
     * Send JSON error reply to web API for call {@code messageUID}
     *
     * @param session    WebSocket session
     * @param messageUID ID of call from web API
     * @param ex         Exception to get error message from
     */
    private void sendError(Session session, String messageUID, Exception ex) {
        String message = ex.getMessage();
        if (message == null) { message = ex.getClass().getSimpleName(); }

        sendError(session, messageUID, message);
    }

    /**
     * Send JSON error reply to web API for call {@code messageUID}
     *
     * @param session    WebSocket session
     * @param messageUID ID of call from web API
     * @param errorMsg   Error from method call
     */
    private void sendError(Session session, String messageUID, String errorMsg) {
        try {
            JSONObject reply = new JSONObject();
            reply.putOpt("uid", messageUID);
            reply.put("error", errorMsg);
            send(session, reply);
        }
        catch(JSONException e) {
            log.error("Send error failed", e);
        }
    }

    /**
     * Send JSON data to web API, to be retrieved by callbacks.
     * Used for data sent apart from API calls, since UID's correspond to a single response.
     *
     * @param session WebSocket session
     * @param type    Type of stream, so appropriate callback in web can be used.
     * @param key     ID associated with stream data
     * @param data    Data to send
     */
    private void sendStream(Session session, StreamType type, Object key, Object data) {
        try {
            JSONObject stream = new JSONObject();
            stream.put("type", type.name());
            stream.put("key", key);
            stream.put("data", data);
            send(session, stream);
        }
        catch(JSONException e) {
            log.error("Send stream failed", e);
        }
    }

    private void sendStreamError(Session session, StreamType type, Object key, Exception ex) {
        String message = ex.getMessage();
        if (message == null) { message = ex.getClass().getSimpleName(); }

        sendStreamError(session, type, key, message);
    }

    private void sendStreamError(Session session, StreamType type, Object key, Object errorMsg) {
        try {
            JSONObject stream = new JSONObject();
            stream.put("type", type.name());
            stream.put("key", key);
            stream.put("error", errorMsg);
            send(session, stream);
        }
        catch(JSONException e) {
            log.error("Send stream failed", e);
        }
    }

    /**
     * Raw send method for replies
     *
     * @param session WebSocket session
     * @param reply   JSON Object of reply to web API
     */
    private void send(Session session, JSONObject reply) {
        try {
            session.getRemote().sendString(reply.toString());
        }
        catch(IOException e) {
            log.error("Send failed", e);
        }
    }

}
