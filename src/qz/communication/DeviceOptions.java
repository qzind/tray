package qz.communication;

import org.codehaus.jettison.json.JSONObject;
import qz.utils.UsbUtilities;

public class DeviceOptions {

    private Short vendorId;
    private Short productId;

    //usb specific
    private Byte interfaceId;
    private Byte endpoint;
    private int interval;
    private int responseSize;

    //hid specific
    private Short usagePage;
    private String serial;


    public DeviceOptions(JSONObject parameters) {
        vendorId = UsbUtilities.hexToShort(parameters.optString("vendorId"));
        productId = UsbUtilities.hexToShort(parameters.optString("productId"));

        interfaceId = UsbUtilities.hexToByte(parameters.optString("interface"));
        endpoint = UsbUtilities.hexToByte(parameters.optString("endpoint", parameters.optString("reportId")));
        interval = parameters.optInt("interval", 100);
        responseSize = parameters.optInt("responseSize");

        usagePage = UsbUtilities.hexToShort(parameters.optString("usagePage"));
        serial = parameters.optString("serial", null);
    }

    public Short getVendorId() {
        return vendorId;
    }

    public Short getProductId() {
        return productId;
    }

    public Byte getInterfaceId() {
        return interfaceId;
    }

    public Byte getEndpoint() {
        return endpoint;
    }

    public int getInterval() {
        return interval;
    }

    public int getResponseSize() {
        return responseSize;
    }

    public Short getUsagePage() {
        return usagePage;
    }

    public String getSerial() {
        return serial;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != DeviceOptions.class) { return false; }

        DeviceOptions that = (DeviceOptions)obj;

        if (this.getVendorId().equals(that.getVendorId()) && this.getProductId().equals(that.getProductId())) {
            //if usb
            if ((this.getInterfaceId() == null || that.getInterfaceId() == null || this.getInterfaceId().equals(that.getInterfaceId()))
                    && (this.getEndpoint() == null || that.getEndpoint() == null || this.getEndpoint().equals(that.getEndpoint()))) {
                return true;
            }

            //if hid
            if ((this.getUsagePage() == null || that.getUsagePage() == null || this.getUsagePage().equals(that.getUsagePage()))
                    && (this.getSerial() == null || that.getSerial() == null || this.getSerial().equals(that.getSerial()))) {
                return true;
            }
        }

        return false;
    }
}
