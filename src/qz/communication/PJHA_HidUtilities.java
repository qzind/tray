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

    public static HidDeviceInfo findDevice(DeviceOptions dOpts) {
        if (dOpts.getVendorId() == null) {
            throw new IllegalArgumentException("Vendor ID cannot be null");
        }
        if (dOpts.getProductId() == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }


        List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
        for(HidDeviceInfo device : devList) {
            if (device.getVendorId() == dOpts.getVendorId().shortValue() && device.getProductId() == dOpts.getProductId().shortValue()
                    && (dOpts.getUsagePage() == null || dOpts.getUsagePage().shortValue() == device.getUsagePage())
                    && (dOpts.getSerial() == null || dOpts.getSerial().equals(device.getSerialNumberString()))) {
                return device;
            }
        }

        return null;
    }

}
