package qz.communication;


import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import qz.utils.SystemUtilities;

import javax.usb.util.UsbUtil;
import java.util.List;

public class H4J_HidUtilities {

    private static HidServices service = HidManager.getHidServices();


    public static List<HidDevice> getHidDevices() {
        List<HidDevice> devices = service.getAttachedHidDevices();

        // FIXME: Prevent hard crash on OSX
        // Per upstream Mac bug https://github.com/gary-rowe/hid4java/issues/37
        if (SystemUtilities.isMac()) {
            service.shutdown();
        }
        return devices;
    }

    public static JSONArray getHidDevicesJSON() throws JSONException {
        List<HidDevice> devices = getHidDevices();
        JSONArray devicesJSON = new JSONArray();

        for(HidDevice device : devices) {
            JSONObject deviceJSON = new JSONObject();

            deviceJSON.put("vendorId", UsbUtil.toHexString(device.getVendorId()))
                    .put("productId", UsbUtil.toHexString(device.getProductId()))
                    .put("usagePage", UsbUtil.toHexString((short)device.getUsagePage()))
                    .put("serial", device.getSerialNumber())
                    .put("manufacturer", device.getManufacturer())
                    .put("product", device.getProduct());

            devicesJSON.put(deviceJSON);
        }

        return devicesJSON;
    }


    public static HidDevice findDevice(Short vendorId, Short productId, Short usagePage, String serial) {
        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor ID cannot be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        List<HidDevice> devices = getHidDevices();
        for(HidDevice device : devices) {
            if (device.isVidPidSerial(vendorId, productId, serial)
                    && (usagePage == null || usagePage == (short)device.getUsagePage())) {
                return device;
            }
        }

        return null;
    }

}
