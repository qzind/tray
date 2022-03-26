package qz.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.communication.UsbException;
import qz.communication.DeviceIO;
import qz.communication.UsbOptions;
import qz.exception.WebsocketError;
import qz.ws.PrintSocketClient;
import qz.ws.SocketConnection;
import qz.ws.StreamEvent;

import javax.usb.*;
import javax.usb.util.UsbUtil;
import java.util.ArrayList;
import java.util.List;

public class UsbUtilities {

    private static final Logger log = LogManager.getLogger(UsbUtilities.class);

    public static Integer hexToInt(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }

        if (hex.startsWith("0x")) { hex = hex.substring(2); }
        return Integer.parseInt(hex, 16);
    }

    public static Byte hexToByte(String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }

        if (hex.startsWith("0x")) { hex = hex.substring(2); }
        return (byte)Integer.parseInt(hex, 16);
    }


    public static List<UsbDevice> getUsbDevices(boolean includeHubs) throws UsbException {
        try {
            return getUsbDevices(UsbHostManager.getUsbServices().getRootUsbHub(), includeHubs);
        }
        catch(javax.usb.UsbException e) {
            throw new UsbException(e);
        }
    }

    private static List<UsbDevice> getUsbDevices(UsbHub hub, boolean includeHubs) {
        List<UsbDevice> devices = new ArrayList<>();

        for(Object attached : hub.getAttachedUsbDevices()) {
            UsbDevice device = (UsbDevice)attached;

            if (device.isUsbHub()) {
                if (includeHubs) {
                    devices.add(device);
                }

                devices.addAll(getUsbDevices((UsbHub)device, includeHubs));
            } else {
                devices.add(device);
            }
        }

        return devices;
    }

    public static JSONArray getUsbDevicesJSON(boolean includeHubs) throws UsbException, JSONException {
        List<UsbDevice> devices = getUsbDevices(includeHubs);
        JSONArray deviceJSON = new JSONArray();

        for(UsbDevice device : devices) {
            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();

            JSONObject descJSON = new JSONObject();
            descJSON.put("vendorId", UsbUtil.toHexString(desc.idVendor()));
            descJSON.put("productId", UsbUtil.toHexString(desc.idProduct()));
            descJSON.put("hub", device.isUsbHub());

            deviceJSON.put(descJSON);
        }

        return deviceJSON;
    }

    public static UsbDevice findDevice(Short vendorId, Short productId) throws UsbException {
        try {
            return findDevice(UsbHostManager.getUsbServices().getRootUsbHub(), vendorId, productId);
        }
        catch(javax.usb.UsbException e) {
            throw new UsbException(e);
        }
    }

    private static UsbDevice findDevice(UsbHub hub, Short vendorId, Short productId) {
        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor ID cannot be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        for(Object attached : hub.getAttachedUsbDevices()) {
            UsbDevice device = (UsbDevice)attached;

            UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
            if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
                return device;
            }

            if (device.isUsbHub()) {
                device = findDevice((UsbHub)device, vendorId, productId);
                if (device != null) {
                    return device;
                }
            }
        }

        return null;
    }

    public static List getDeviceInterfaces(Short vendorId, Short productId) throws UsbException {
        return findDevice(vendorId, productId).getActiveUsbConfiguration().getUsbInterfaces();
    }

    public static JSONArray getDeviceInterfacesJSON(UsbOptions dOpts) throws UsbException {
        JSONArray ifaceJSON = new JSONArray();

        List ifaces = getDeviceInterfaces(dOpts.getVendorId().shortValue(), dOpts.getProductId().shortValue());
        for(Object o : ifaces) {
            UsbInterface iface = (UsbInterface)o;
            UsbInterfaceDescriptor desc = iface.getUsbInterfaceDescriptor();

            ifaceJSON.put(UsbUtil.toHexString(desc.bInterfaceNumber()));
        }

        return ifaceJSON;
    }

    public static List getInterfaceEndpoints(Short vendorId, Short productId, Byte iface) throws UsbException {
        if (iface == null) {
            throw new IllegalArgumentException("Device interface cannot be null");
        }

        return findDevice(vendorId, productId).getActiveUsbConfiguration().getUsbInterface(iface).getUsbEndpoints();
    }

    public static JSONArray getInterfaceEndpointsJSON(UsbOptions dOpts) throws UsbException {
        JSONArray endJSON = new JSONArray();

        List endpoints = getInterfaceEndpoints(dOpts.getVendorId().shortValue(), dOpts.getProductId().shortValue(), dOpts.getInterfaceId());
        for(Object o : endpoints) {
            UsbEndpoint endpoint = (UsbEndpoint)o;
            UsbEndpointDescriptor desc = endpoint.getUsbEndpointDescriptor();

            endJSON.put(UsbUtil.toHexString(desc.bEndpointAddress()));
        }

        return endJSON;
    }


    // shared by usb and hid streaming
    public static void setupUsbStream(final Session session, String UID, SocketConnection connection, final UsbOptions dOpts, final StreamEvent.Stream streamType) {
        final DeviceIO usb = connection.getDevice(dOpts);

        if (usb != null) {
            if (!usb.isStreaming()) {
                usb.setStreaming(true);

                new Thread() {
                    @Override
                    public void run() {
                        int interval = dOpts.getInterval();
                        int size = dOpts.getResponseSize();
                        Byte endpoint = dOpts.getEndpoint();

                        StreamEvent event = new StreamEvent(streamType, StreamEvent.Type.RECEIVE)
                                .withData("vendorId", usb.getVendorId()).withData("productId", usb.getProductId());

                        try {
                            while(usb.isOpen() && usb.isStreaming()) {
                                byte[] response = usb.readData(size, endpoint);
                                JSONArray hex = new JSONArray();
                                for(byte b : response) {
                                    hex.put(UsbUtil.toHexString(b));
                                }

                                PrintSocketClient.sendStream(session, event.withData("output", hex));

                                try { Thread.sleep(interval); } catch(Exception ignore) {}
                            }
                        }
                        catch(WebSocketException e) {
                            usb.setStreaming(false);
                            log.error("USB stream error", e);
                        }
                        catch(UsbException e) {
                            usb.setStreaming(false);
                            log.error("USB stream error", e);

                            StreamEvent eventErr = new StreamEvent(streamType, StreamEvent.Type.ERROR).withException(e)
                                    .withData("vendorId", usb.getVendorId()).withData("productId", usb.getProductId());
                            PrintSocketClient.sendStream(session, eventErr);
                        }
                    }
                }.start();

                PrintSocketClient.sendResult(session, UID, null);
            } else {
                PrintSocketClient.sendError(session, UID, WebsocketError.USB_ALREADY_STREAMING, dOpts.getVendorIdString(), dOpts.getProductIdString());
            }
        } else {
            PrintSocketClient.sendError(session, UID, WebsocketError.USB_NOT_CLAIMED, dOpts.getVendorIdString(), dOpts.getProductIdString());
        }
    }

}
