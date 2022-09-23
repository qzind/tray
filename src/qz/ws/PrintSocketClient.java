package qz.ws;

import jssc.SerialPortException;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.CloseException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.usb4java.LoaderException;
import qz.auth.Certificate;
import qz.auth.RequestState;
import qz.common.Constants;
import qz.common.TrayManager;
import qz.communication.*;
import qz.printer.PrintServiceMatcher;
import qz.printer.status.StatusMonitor;
import qz.printer.status.StatusSession;
import qz.utils.*;

import javax.management.ListenerNotFoundException;
import javax.usb.util.UsbUtil;
import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;


@WebSocket
public class PrintSocketClient {

    private static final Logger log = LogManager.getLogger(PrintSocketClient.class);

    private final TrayManager trayManager = PrintSocketServer.getTrayManager();
    private static final Semaphore dialogAvailable = new Semaphore(1, true);

    //websocket port -> Connection
    private static final HashMap<Integer,SocketConnection> openConnections = new HashMap<>();

    private Server server;

    public PrintSocketClient(Server server) {
        this.server = server;
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
        if (error instanceof EOFException) { return; }

        if (error instanceof CloseException && error.getCause() instanceof TimeoutException) {
            log.error("Timeout error (Lost connection with client)", error);
            return;
        }

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
            JSONObject json = cleanupMessage(new JSONObject(message));
            log.debug("Message: {}", json);
            UID = json.optString("uid");

            Integer connectionPort = session.getRemoteAddress().getPort();
            SocketConnection connection = openConnections.get(connectionPort);
            RequestState request = new RequestState(connection.getCertificate(), json);

            //if sent a certificate use that instead for this connection
            if (json.has("certificate")) {
                try {
                    Certificate certificate = new Certificate(json.optString("certificate"));
                    connection.setCertificate(certificate);

                    request.markNewConnection(certificate);

                    log.debug("Received new certificate from connection through {}", connectionPort);
                }
                catch(CertificateException ignore) {
                    request.markNewConnection(Certificate.UNKNOWN);
                }

                if (allowedFromDialog(request, "connect to " + Constants.ABOUT_TITLE,
                                      findDialogPosition(session, json.optJSONObject("position")))) {
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, "Connection blocked by client");
                    session.disconnect();
                }

                return; //this is a setup call, so no further processing is needed
            }

            //check request signature
            SocketMethod call = SocketMethod.findFromCall(json.optString("call"));
            if (request.hasCertificate() && call.isDialogShown()) {
                if (json.optLong("timestamp") + Constants.VALID_SIGNING_PERIOD < System.currentTimeMillis()
                        || json.optLong("timestamp") - Constants.VALID_SIGNING_PERIOD > System.currentTimeMillis()) {
                    //bad timestamps use the expired certificate
                    log.warn("Expired signature on request");
                    request.setStatus(RequestState.Validity.EXPIRED);
                } else if (json.isNull("signature") || !validSignature(request.getCertUsed(), json)) {
                    //bad signatures use the unsigned certificate
                    log.warn("Bad signature on request");
                    request.setStatus(RequestState.Validity.UNSIGNED);
                } else {
                    log.trace("Valid signature from {}", request.getCertName());
                    request.setStatus(RequestState.Validity.TRUSTED);
                }
            }

            processMessage(session, json, connection, request);
        }
        catch(UnsatisfiedLinkError | LoaderException e) {
            log.error("A component is missing or broken, preventing this feature from working", e);
            sendError(session, UID, "Sorry, this feature is unavailable at this time");
        }
        catch(JSONException e) {
            log.error("Bad JSON: {}", e.getMessage());
            sendError(session, UID, e);
        }
        catch(InvalidPathException | FileSystemException e) {
            log.error("FileIO exception occurred", e);
            sendError(session, UID, String.format("FileIO exception occurred: %s: %s", e.getClass().getSimpleName(), e.getMessage()));
        }
        catch(Exception e) {
            log.error("Problem processing message", e);
            sendError(session, UID, e);
        }
    }

    private JSONObject cleanupMessage(JSONObject msg) {
        msg.remove("promise"); //never needed java side

        //remove unused properties from older js api's
        SocketMethod call = SocketMethod.findFromCall(msg.optString("call"));
        if (!call.isDialogShown()) {
            msg.remove("signature");
            msg.remove("signAlgorithm");
        }

        return msg;
    }

    private boolean validSignature(Certificate certificate, JSONObject message) throws JSONException {
        JSONObject copy = new JSONObject(message, new String[] {"call", "params", "timestamp"});
        String signature = message.optString("signature");
        String algorithm = message.optString("signAlgorithm", "SHA1").toUpperCase(Locale.ENGLISH);

        return certificate.isSignatureValid(Certificate.Algorithm.valueOf(algorithm), signature, copy.toString().replaceAll("\\\\/", "/"));
    }

    /**
     * Determine which method was called from web API
     *
     * @param session WebSocket session
     * @param json    JSON received from web API
     */
    private void processMessage(Session session, JSONObject json, SocketConnection connection, RequestState request) throws JSONException, SerialPortException, DeviceException, IOException, ListenerNotFoundException {
        String UID = json.optString("uid");
        SocketMethod call = SocketMethod.findFromCall(json.optString("call"));
        JSONObject params = json.optJSONObject("params");
        if (params == null) { params = new JSONObject(); }

        if (call == SocketMethod.INVALID && (UID == null || UID.isEmpty())) {
            //incorrect message format, likely incompatible qz version
            session.close(4003, "Connected to incompatible " + Constants.ABOUT_TITLE + " version");
            return;
        }

        String prompt = call.getDialogPrompt();
        if (call == SocketMethod.PRINT) {
            //special formatting for print dialogs
            JSONObject pr = params.optJSONObject("printer");
            if (pr != null) {
                prompt = String.format(prompt, pr.optString("name", pr.optString("file", pr.optString("host", "an undefined location"))));
            } else {
                sendError(session, UID, "A printer must be specified before printing");
                return;
            }
        }

        if (call.isDialogShown()
                && !allowedFromDialog(request, prompt, findDialogPosition(session, json.optJSONObject("position")))) {
            sendError(session, UID, "Request blocked");
            return;
        }

        if (call != SocketMethod.GET_VERSION) {
            trayManager.voidIdleActions();
        }

        // used in usb calls
        DeviceOptions dOpts = new DeviceOptions(params, DeviceOptions.DeviceMode.parse(call.getCallName()));


        //call appropriate methods
        switch(call) {
            case PRINTERS_GET_DEFAULT:
                sendResult(session, UID, PrintServiceMatcher.getDefaultPrinter() == null? null:
                        PrintServiceMatcher.getDefaultPrinter().getName());
                break;
            case PRINTERS_FIND:
                if (params.has("query")) {
                    String name = PrintServiceMatcher.findPrinterName(params.getString("query"));
                    if (name != null) {
                        sendResult(session, UID, name);
                    } else {
                        sendError(session, UID, "Specified printer could not be found.");
                    }
                } else {
                    JSONArray services = PrintServiceMatcher.getPrintersJSON(false);
                    JSONArray names = new JSONArray();
                    for(int i = 0; i < services.length(); i++) {
                        names.put(services.getJSONObject(i).getString("name"));
                    }

                    sendResult(session, UID, names);
                }
                break;
            case PRINTERS_DETAIL:
                sendResult(session, UID, PrintServiceMatcher.getPrintersJSON(true));
                break;
            case PRINTERS_START_LISTENING:
                if (!connection.hasStatusListener()) {
                    connection.startStatusListener(new StatusSession(session));
                }
                StatusMonitor.startListening(connection, params.getJSONArray("printerNames"));
                sendResult(session, UID, null);
                break;
            case PRINTERS_GET_STATUS:
                if (connection.hasStatusListener()) {
                    StatusMonitor.sendStatuses(connection);
                } else {
                    sendError(session, UID, "No printer listeners started for this client.");
                }
                sendResult(session, UID, null);
                break;
            case PRINTERS_STOP_LISTENING:
                if (connection.hasStatusListener()) {
                    connection.stopStatusListener();
                }
                sendResult(session, UID, null);
                break;
            case PRINT:
                PrintingUtilities.processPrintRequest(session, UID, params);
                break;

            case SERIAL_FIND_PORTS:
                sendResult(session, UID, SerialUtilities.getSerialPortsJSON());
                break;
            case SERIAL_OPEN_PORT:
                SerialUtilities.setupSerialPort(session, UID, connection, params);
                break;
            case SERIAL_SEND_DATA: {
                SerialOptions opts = null;
                //properties param is deprecated legacy here and will be overridden by options if provided
                if (!params.isNull("properties")) {
                    opts = new SerialOptions(params.optJSONObject("properties"), false);
                }
                if (!params.isNull("options")) {
                    opts = new SerialOptions(params.optJSONObject("options"), false);
                }

                SerialIO serial = connection.getSerialPort(params.optString("port"));
                if (serial != null) {
                    serial.sendData(params, opts);
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

            case SOCKET_OPEN_PORT:
                SocketUtilities.setupSocket(session, UID, connection, params);
                break;
            case SOCKET_SEND_DATA: {
                String location = String.format("%s:%s", params.optString("host"), params.optInt("port"));
                SocketIO socket = connection.getNetworkSocket(location);
                if (socket != null) {
                    socket.sendData(params);
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("Socket [%s] is not open.", location));
                }
                break;
            }
            case SOCKET_CLOSE_PORT: {
                String location = String.format("%s:%s", params.optString("host"), params.optInt("port"));
                SocketIO socket = connection.getNetworkSocket(location);
                if (socket != null) {
                    socket.close();
                    connection.removeNetworkSocket(location);
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("Socket [%s] is not open.", location));
                }
                break;
            }

            case USB_LIST_DEVICES:
                sendResult(session, UID, UsbUtilities.getUsbDevicesJSON(params.getBoolean("includeHubs")));
                break;
            case USB_LIST_INTERFACES:
                sendResult(session, UID, UsbUtilities.getDeviceInterfacesJSON(dOpts));
                break;
            case USB_LIST_ENDPOINTS:
                sendResult(session, UID, UsbUtilities.getInterfaceEndpointsJSON(dOpts));
                break;
            case HID_LIST_DEVICES:
                if (SystemUtilities.isWindows()) {
                    sendResult(session, UID, PJHA_HidUtilities.getHidDevicesJSON());
                } else {
                    sendResult(session, UID, H4J_HidUtilities.getHidDevicesJSON());
                }
                break;
            case HID_START_LISTENING:
                if (!connection.isDeviceListening()) {
                    if (SystemUtilities.isWindows()) {
                        connection.startDeviceListening(new PJHA_HidListener(session));
                    } else {
                        connection.startDeviceListening(new H4J_HidListener(session));
                    }
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, "Already listening HID device events");
                }
                break;
            case HID_STOP_LISTENING:
                if (connection.isDeviceListening()) {
                    connection.stopDeviceListening();
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, "Not already listening HID device events");
                }
                break;

            case USB_CLAIM_DEVICE:
            case HID_CLAIM_DEVICE: {
                if (connection.getDevice(dOpts) == null) {
                    DeviceIO device;
                    if (call == SocketMethod.USB_CLAIM_DEVICE) {
                        device = new UsbIO(dOpts);
                    } else {
                        if (SystemUtilities.isWindows()) {
                            device = new PJHA_HidIO(dOpts);
                        } else {
                            device = new H4J_HidIO(dOpts);
                        }
                    }

                    if (session.isOpen()) {
                        connection.openDevice(device, dOpts);
                    }

                    if (device.isOpen()) {
                        sendResult(session, UID, null);
                    } else {
                        sendError(session, UID, "Failed to open connection to device");
                    }
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] is already claimed.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }
            case USB_CLAIMED:
            case HID_CLAIMED: {
                sendResult(session, UID, connection.getDevice(dOpts) != null);
                break;
            }
            case USB_SEND_DATA:
            case HID_SEND_FEATURE_REPORT:
            case HID_SEND_DATA: {
                DeviceIO usb = connection.getDevice(dOpts);
                if (usb != null) {

                    if (call == SocketMethod.HID_SEND_FEATURE_REPORT) {
                        usb.sendFeatureReport(DeviceUtilities.getDataBytes(params, null), dOpts.getEndpoint());
                    } else {
                        usb.sendData(DeviceUtilities.getDataBytes(params, null), dOpts.getEndpoint());
                    }

                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] must be claimed first.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }
            case USB_READ_DATA:
            case HID_GET_FEATURE_REPORT:
            case HID_READ_DATA: {
                DeviceIO usb = connection.getDevice(dOpts);
                if (usb != null) {
                    byte[] response;

                    if (call == SocketMethod.HID_GET_FEATURE_REPORT) {
                        response = usb.getFeatureReport(dOpts.getResponseSize(), dOpts.getEndpoint());
                    } else {
                        response = usb.readData(dOpts.getResponseSize(), dOpts.getEndpoint());
                    }


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
            case USB_OPEN_STREAM:
            case HID_OPEN_STREAM: {
                StreamEvent.Stream stream = (call == SocketMethod.USB_OPEN_STREAM? StreamEvent.Stream.USB:StreamEvent.Stream.HID);
                UsbUtilities.setupUsbStream(session, UID, connection, dOpts, stream);
                break;
            }
            case USB_CLOSE_STREAM:
            case HID_CLOSE_STREAM: {
                DeviceIO usb = connection.getDevice(dOpts);
                if (usb != null && usb.isStreaming()) {
                    usb.setStreaming(false);
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] is not streaming data.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }
            case USB_RELEASE_DEVICE:
            case HID_RELEASE_DEVICE: {
                DeviceIO usb = connection.getDevice(dOpts);
                if (usb != null) {
                    usb.close();
                    connection.removeDevice(dOpts);

                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, String.format("USB Device [v:%s p:%s] is not claimed.", params.opt("vendorId"), params.opt("productId")));
                }

                break;
            }

            case FILE_START_LISTENING: {
                FileParams fileParams = new FileParams(params);
                Path absPath = FileUtilities.getAbsolutePath(params, request, true);
                FileIO fileIO = new FileIO(session, params, fileParams.getPath(), absPath);

                if (connection.getFileListener(absPath) == null && !fileIO.isWatching()) {
                    connection.addFileListener(absPath, fileIO);

                    FileUtilities.setupListener(fileIO);
                    sendResult(session, UID, null);
                } else {
                    sendError(session, UID, "Already listening to path events");
                }

                break;
            }
            case FILE_STOP_LISTENING: {
                if (params.isNull("path")) {
                    connection.removeAllFileListeners();
                    sendResult(session, UID, null);
                } else {
                    Path absPath = FileUtilities.getAbsolutePath(params, request, true);
                    FileIO fileIO = connection.getFileListener(absPath);

                    if (fileIO != null) {
                        fileIO.close();
                        FileWatcher.deregisterWatch(fileIO);
                        connection.removeFileListener(absPath);
                        sendResult(session, UID, null);
                    } else {
                        sendError(session, UID, "Not already listening to path events");
                    }
                }

                break;
            }
            case FILE_LIST: {
                Path absPath = FileUtilities.getAbsolutePath(params, request, true);

                if (Files.exists(absPath)) {
                    if (Files.isDirectory(absPath)) {
                        ArrayList<String> files = new ArrayList<>();
                        Files.list(absPath).forEach(file -> files.add(file.getFileName().toString()));
                        sendResult(session, UID, new JSONArray(files));
                    } else {
                        log.error("Failed to list '{}' (not a directory)", absPath);
                        sendError(session, UID, "Path is not a directory");
                    }
                } else {
                    log.error("Failed to list '{}' (does not exist)", absPath);
                    sendError(session, UID, "Path does not exist");
                }

                break;
            }
            case FILE_READ: {
                Path absPath = FileUtilities.getAbsolutePath(params, request, false);
                if (Files.exists(absPath)) {
                    if (Files.isReadable(absPath)) {
                        sendResult(session, UID, new String(Files.readAllBytes(absPath)));
                    } else {
                        log.error("Failed to read '{}' (not readable)", absPath);
                        sendError(session, UID, "Path is not readable");
                    }
                } else {
                    log.error("Failed to read '{}' (does not exist)", absPath);
                    sendError(session, UID, "Path does not exist");
                }

                break;
            }
            case FILE_WRITE: {
                FileParams fileParams = new FileParams(params);
                Path absPath = FileUtilities.getAbsolutePath(params, request, false, true);

                Files.write(absPath, fileParams.getData(), StandardOpenOption.CREATE, fileParams.getAppendMode());
                FileUtilities.inheritParentPermissions(absPath);
                sendResult(session, UID, null);
                break;
            }
            case FILE_REMOVE: {
                Path absPath = FileUtilities.getAbsolutePath(params, request, false);

                if (Files.exists(absPath)) {
                    Files.delete(absPath);
                    sendResult(session, UID, null);
                } else {
                    log.error("Failed to remove '{}' (does not exist)", absPath);
                    sendError(session, UID, "Path does not exist");
                }

                break;
            }
            case NETWORKING_DEVICE_LEGACY:
                try {
                    JSONObject networkDevice = NetworkUtilities.getDeviceJSON(params);
                    JSONObject legacyDevice = new JSONObject();
                    legacyDevice.put("ipAddress", networkDevice.optString("ip", null));
                    legacyDevice.put("macAddress", networkDevice.optString("mac", null));
                    sendResult(session, UID, legacyDevice);
                } catch(IOException e) {
                    sendError(session, UID, "Unable to determine primary network device: " + e.getClass().getSimpleName() + " " + e.getMessage());
                }
                break;
            case NETWORKING_DEVICE:
                try {
                    sendResult(session, UID, NetworkUtilities.getDeviceJSON(params));
                } catch(IOException e) {
                    sendError(session, UID, "Unable to determine primary network device: " + e.getClass().getSimpleName() + " " + e.getMessage());
                }
                break;
            case NETWORKING_DEVICES:
                sendResult(session, UID, NetworkUtilities.getDevicesJSON(params));
                break;
            case NETWORKING_HOSTNAME:
                sendResult(session, UID, SystemUtilities.getHostName());
                break;
            case GET_VERSION:
                sendResult(session, UID, Constants.VERSION);
                break;
            case WEBSOCKET_STOP:
                log.info("Another instance of {} is asking this to close", Constants.ABOUT_TITLE);
                String challenge = json.optString("challenge", "");
                if(SystemUtilities.validateSaltedChallenge(challenge)) {
                    log.info("Challenge validated: {}, honoring shutdown request", challenge);

                    session.close(SingleInstanceChecker.REQUEST_INSTANCE_TAKEOVER);
                    try {
                        server.stop();
                    } catch(Exception ignore) {}
                    trayManager.exit(0);
                } else {
                    log.warn("A valid challenge was not provided: {}, ignoring request to close", challenge);
                }
                break;
            case INVALID:
            default:
                sendError(session, UID, "Invalid function call: " + json.optString("call", "NONE"));
                break;
        }
    }

    private boolean allowedFromDialog(RequestState request, String prompt, Point position) {
        //If cert can be resolved before the lock, do so and return
        if (request.hasBlockedCert()) {
            return false;
        }
        if (request.hasSavedCert()) {
            return true;
        }

        //wait until previous prompts are closed
        try {
            dialogAvailable.acquire();
        }
        catch(InterruptedException e) {
            log.warn("Failed to acquire dialog", e);
            return false;
        }

        //prompt user for access
        boolean allowed = trayManager.showGatewayDialog(request, prompt, position);

        dialogAvailable.release();

        return allowed;
    }

    private Point findDialogPosition(Session session, JSONObject positionData) {
        Point pos = new Point(0, 0);
        if (session.getRemoteAddress().getAddress().isLoopbackAddress() && positionData != null
                && !positionData.isNull("x") && !positionData.isNull("y")) {
            pos.move(positionData.optInt("x"), positionData.optInt("y"));
        }

        return pos;
    }


    /**
     * Send JSON reply to web API for call {@code messageUID}
     *
     * @param session     WebSocket session
     * @param messageUID  ID of call from web API
     * @param returnValue Return value of method call, can be {@code null}
     */
    public static void sendResult(Session session, String messageUID, Object returnValue) {
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
    public static void sendError(Session session, String messageUID, Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = ex.getClass().getSimpleName();
        }

        sendError(session, messageUID, message);
    }

    /**
     * Send JSON error reply to web API for call {@code messageUID}
     *
     * @param session    WebSocket session
     * @param messageUID ID of call from web API
     * @param errorMsg   Error from method call
     */
    public static void sendError(Session session, String messageUID, String errorMsg) {
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
     * @param event   StreamEvent with data to send down to web API
     */
    public static void sendStream(Session session, StreamEvent event) {
        try {
            JSONObject stream = new JSONObject();
            stream.put("type", event.getStreamType());
            stream.put("event", event.toJSON());
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
    private static synchronized void send(Session session, JSONObject reply) throws WebSocketException {
        try {
            session.getRemote().sendString(reply.toString());
        }
        catch(IOException e) {
            log.error("Could not send message", e);
        }
    }

}
