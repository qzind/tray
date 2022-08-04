package qz.ws;

public enum SocketMethod {
    PRINTERS_GET_DEFAULT("printers.getDefault", true, "access connected printers"),
    PRINTERS_FIND("printers.find", true, "access connected printers"),
    PRINTERS_DETAIL("printers.detail", true, "access connected printers"),
    PRINTERS_START_LISTENING("printers.startListening", true, "listen for printer status"),
    PRINTERS_GET_STATUS("printers.getStatus", false),
    PRINTERS_STOP_LISTENING("printers.stopListening", false),
    PRINT("print", true, "print to %s"),

    SERIAL_FIND_PORTS("serial.findPorts", true, "access serial ports"),
    SERIAL_OPEN_PORT("serial.openPort", true, "open a serial port"),
    SERIAL_SEND_DATA("serial.sendData", true, "send data over a serial port"),
    SERIAL_CLOSE_PORT("serial.closePort", true, "close a serial port"),

    SOCKET_OPEN_PORT("socket.open", true, "open a socket"),
    SOCKET_SEND_DATA("socket.sendData", true, "send data over a socket"),
    SOCKET_CLOSE_PORT("socket.close", true, "close a socket"),

    USB_LIST_DEVICES("usb.listDevices", true, "access USB devices"),
    USB_LIST_INTERFACES("usb.listInterfaces", true, "access USB devices"),
    USB_LIST_ENDPOINTS("usb.listEndpoints", true, "access USB devices"),
    USB_CLAIM_DEVICE("usb.claimDevice", true, "claim a USB device"),
    USB_CLAIMED("usb.isClaimed", false, "check USB claim status"),
    USB_SEND_DATA("usb.sendData", true, "use a USB device"),
    USB_READ_DATA("usb.readData", true, "use a USB device"),
    USB_OPEN_STREAM("usb.openStream", true, "use a USB device"),
    USB_CLOSE_STREAM("usb.closeStream", false, "use a USB device"),
    USB_RELEASE_DEVICE("usb.releaseDevice", false, "release a USB device"),

    HID_LIST_DEVICES("hid.listDevices", true, "access USB devices"),
    HID_START_LISTENING("hid.startListening", true, "listen for USB devices"),
    HID_STOP_LISTENING("hid.stopListening", false),
    HID_CLAIM_DEVICE("hid.claimDevice", true, "claim a USB device"),
    HID_CLAIMED("hid.isClaimed", false, "check USB claim status"),
    HID_SEND_DATA("hid.sendData", true, "use a USB device"),
    HID_READ_DATA("hid.readData", true, "use a USB device"),
    HID_SEND_FEATURE_REPORT("hid.sendFeatureReport", true, "use a USB device"),
    HID_GET_FEATURE_REPORT("hid.getFeatureReport", true, "use a USB device"),
    HID_OPEN_STREAM("hid.openStream", true, "use a USB device"),
    HID_CLOSE_STREAM("hid.closeStream", false, "use a USB device"),
    HID_RELEASE_DEVICE("hid.releaseDevice", false, "release a USB device"),

    FILE_LIST("file.list", true, "view the filesystem"),
    FILE_START_LISTENING("file.startListening", true, "listen for filesystem events"),
    FILE_STOP_LISTENING("file.stopListening", false),
    FILE_READ("file.read", true, "read the content of a file"),
    FILE_WRITE("file.write", true, "write to a file"),
    FILE_REMOVE("file.remove", true, "delete a file"),

    NETWORKING_DEVICE("networking.device", true),
    NETWORKING_DEVICES("networking.devices", true),
    NETWORKING_HOSTNAME("networking.hostname", true),
    NETWORKING_DEVICE_LEGACY("websocket.getNetworkInfo", true),
    GET_VERSION("getVersion", false),

    WEBSOCKET_STOP("websocket.stop", false),

    INVALID("", false);


    private String callName;
    private String dialogPrompt;
    private boolean dialogShown;

    SocketMethod(String callName, boolean dialogShown) {
        this(callName, dialogShown, "access local resources");
    }

    SocketMethod(String callName, boolean dialogShown, String dialogPrompt) {
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

    public static SocketMethod findFromCall(String call) {
        for(SocketMethod m : SocketMethod.values()) {
            if (m.callName.equals(call)) {
                return m;
            }
        }

        return INVALID;
    }

    public String getCallName() {
        return callName;
    }

}
