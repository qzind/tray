package qz.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.usb.*;
import javax.usb.util.UsbUtil;
import java.util.ArrayList;
import java.util.List;

public class UsbUtilities {

    public static short hexToShort(String hex) {
        if (hex == null || hex.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty");
        }

        if (hex.startsWith("0x")) { hex = hex.substring(2); }
        return (short)Integer.parseInt(hex, 16);
    }

    public static byte hexToByte(String hex) {
        if (hex == null || hex.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty");
        }

        if (hex.startsWith("0x")) { hex = hex.substring(2); }
        return (byte)Integer.parseInt(hex, 16);
    }


    public static List<UsbDevice> getUsbDevices(boolean includeHubs) throws UsbException {
        return getUsbDevices(UsbHostManager.getUsbServices().getRootUsbHub(), includeHubs);
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

            try {
                descJSON.put("manufacturer", device.getManufacturerString());
                descJSON.put("product", device.getProductString());
            }
            catch(Exception ignore) {} //various problems prevent reading this additional information

            deviceJSON.put(descJSON);
        }

        return deviceJSON;
    }

    public static UsbDevice findDevice(short vendorId, short productId) throws UsbException {
        return findDevice(UsbHostManager.getUsbServices().getRootUsbHub(), vendorId, productId);
    }

    private static UsbDevice findDevice(UsbHub hub, short vendorId, short productId) {
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

    public static List getDeviceInterfaces(short vendorId, short productId) throws UsbException {
        return findDevice(vendorId, productId).getActiveUsbConfiguration().getUsbInterfaces();
    }

    public static JSONArray getDeviceInterfacesJSON(short vendorId, short productId) throws UsbException {
        JSONArray ifaceJSON = new JSONArray();

        List ifaces = getDeviceInterfaces(vendorId, productId);
        for(Object o : ifaces) {
            UsbInterface iface = (UsbInterface)o;
            UsbInterfaceDescriptor desc = iface.getUsbInterfaceDescriptor();

            ifaceJSON.put(UsbUtil.toHexString(desc.bInterfaceNumber()));
        }

        return ifaceJSON;
    }

    public static List getInterfaceEndpoints(short vendorId, short productId, byte iface) throws UsbException {
        return findDevice(vendorId, productId).getActiveUsbConfiguration().getUsbInterface(iface).getUsbEndpoints();
    }

    public static JSONArray getInterfaceEndpointsJSON(short vendorId, short productId, byte iface) throws UsbException {
        JSONArray endJSON = new JSONArray();

        List endpoints = getInterfaceEndpoints(vendorId, productId, iface);
        for(Object o : endpoints) {
            UsbEndpoint endpoint = (UsbEndpoint)o;
            UsbEndpointDescriptor desc = endpoint.getUsbEndpointDescriptor();

            endJSON.put(UsbUtil.toHexString(desc.bEndpointAddress()));
        }

        return endJSON;
    }

}
