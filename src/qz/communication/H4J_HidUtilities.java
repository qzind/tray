package qz.communication;


import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

import javax.usb.util.UsbUtil;
import java.util.HashSet;
import java.util.List;

public class H4J_HidUtilities {
    private static final HidServices service = HidManager.getHidServices();

    public static List<HidDevice> getHidDevices() {
        return service.getAttachedHidDevices();
    }

    public static JSONArray getHidDevicesJSON() throws JSONException {
        List<HidDevice> devices = getHidDevices();
        JSONArray devicesJSON = new JSONArray();

        HashSet<String> unique = new HashSet<>();
        for(HidDevice device : devices) {
            JSONObject deviceJSON = new JSONObject();

            deviceJSON.put("vendorId", UsbUtil.toHexString(device.getVendorId()))
                    .put("productId", UsbUtil.toHexString(device.getProductId()))
                    .put("usagePage", UsbUtil.toHexString((short)device.getUsagePage()))
                    .put("serial", device.getSerialNumber())
                    .put("manufacturer", device.getManufacturer())
                    .put("product", device.getProduct());

            String uid = String.format("v%sp%su%ss%s", deviceJSON.optString("vendorId"), deviceJSON.optString("productId"), deviceJSON.optString("usagePage"), deviceJSON.optString("serial"));
            if (!unique.contains(uid)) {
                devicesJSON.put(deviceJSON);
                unique.add(uid);
            }
        }

        return devicesJSON;
    }

    public static HidDevice findDevice(DeviceOptions dOpts) {
        if (dOpts.getVendorId() == null) {
            throw new IllegalArgumentException("Vendor ID cannot be null");
        }
        if (dOpts.getProductId() == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }

        List<HidDevice> devices = getHidDevices();
        for(HidDevice device : devices) {
            if (device.isVidPidSerial(dOpts.getVendorId(), dOpts.getProductId(), dOpts.getSerial())
                    && (dOpts.getUsagePage() == null || dOpts.getUsagePage() == device.getUsagePage())) {
                return device;
            }
        }

        return null;
    }
}
