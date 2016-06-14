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

public class HidUtilities {

    private static HidServices service = HidManager.getHidServices();


    public static List<HidDevice> getHidDevices() {
        return service.getAttachedHidDevices();
    }

    public static JSONArray getHidDevicesJSON() throws JSONException {
        List<HidDevice> devices = getHidDevices();
        JSONArray devicesJSON = new JSONArray();

        for(HidDevice device : devices) {
            JSONObject deviceJSON = new JSONObject();

            deviceJSON.put("vendorId", UsbUtil.toHexString(device.getVendorId()))
                    .put("productId", UsbUtil.toHexString(device.getProductId()))
                    .put("manufacturer", device.getManufacturer())
                    .put("product", device.getProduct());

            devicesJSON.put(deviceJSON);
        }

        return devicesJSON;
    }

    public static HidDevice findDevice(Short vendorId, Short productId) {
        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor ID cannot be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        List<HidDevice> devices = service.getAttachedHidDevices();
        HidDevice device = null;
        for(HidDevice d : devices) {
            if (d.isVidPidSerial(vendorId, productId, null)) {
                device = d;
            }
        }

        // FIXME: Prevent hard crash on OSX
        // Per upstream Mac bug https://github.com/gary-rowe/hid4java/issues/37
        if (SystemUtilities.isMac()) {
            service.shutdown();
        }

        return device;
    }

}
