package qz.communication;


import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;

import javax.usb.util.UsbUtil;
import java.util.List;

public class PJHA_HidUtilities {

    public static JSONArray getHidDevicesJSON() throws JSONException {
        List<HidDeviceInfo> devices = PureJavaHidApi.enumerateDevices();
        JSONArray devicesJSON = new JSONArray();

        for(HidDeviceInfo device : devices) {
            JSONObject deviceJSON = new JSONObject();

            deviceJSON.put("vendorId", UsbUtil.toHexString(device.getVendorId()))
                    .put("productId", UsbUtil.toHexString(device.getProductId()))
                    .put("usagePage", UsbUtil.toHexString(device.getUsagePage()))
                    .put("serial", device.getSerialNumberString())
                    .put("manufacturer", device.getManufacturerString())
                    .put("product", device.getProductString());

            devicesJSON.put(deviceJSON);
        }

        return devicesJSON;
    }

    public static HidDeviceInfo findDevice(Short vendorId, Short productId, Short usagePage, String serial) {
        if (vendorId == null) {
            throw new IllegalArgumentException("Vendor ID cannot be null");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }


        List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
        for(HidDeviceInfo device : devList) {
            if (device.getVendorId() == vendorId && device.getProductId() == productId
                    && (usagePage == null || usagePage == device.getUsagePage())
                    && (serial == null || serial.equals(device.getSerialNumberString()))) {
                return device;
            }
        }

        return null;
    }

}
