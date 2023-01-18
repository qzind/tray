package qz.communication;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.UsbUtilities;

public class UsbOptions {

    public enum DeviceMode {
        HID,
        USB,
        UNKNOWN;
        public static DeviceMode parse(String callName) {
            if (callName != null) {
                if (callName.startsWith("usb")) {
                    return USB;
                } else if (callName.startsWith("hid")) {
                    return HID;
                }
            }
            return UNKNOWN;
        }
    }

    private DeviceMode deviceMode;

    private String vendorIdString;
    private String productIdString;

    private Integer vendorId;
    private Integer productId;

    //usb specific
    private Byte interfaceId;
    private Byte endpoint;
    private int interval;
    private int responseSize;

    //hid specific
    private Integer usagePage;
    private String serial;

    public UsbOptions(JSONObject parameters, DeviceMode deviceMode) {
        this.deviceMode = deviceMode;

        vendorIdString = parameters.optString("vendorId");
        vendorId = UsbUtilities.hexToInt(vendorIdString);
        productIdString = parameters.optString("productId");
        productId = UsbUtilities.hexToInt(productIdString);

        if (!parameters.isNull("interface")) {
            interfaceId = UsbUtilities.hexToByte(parameters.optString("interface"));
        }
        if (!parameters.isNull("endpoint")) {
            endpoint = UsbUtilities.hexToByte(parameters.optString("endpoint"));
        } else if (!parameters.isNull("reportId")) {
            endpoint = UsbUtilities.hexToByte(parameters.optString("reportId"));
        }
        interval = parameters.optInt("interval", 100);
        responseSize = parameters.optInt("responseSize");

        if (!parameters.isNull("usagePage")) {
            usagePage = UsbUtilities.hexToInt(parameters.optString("usagePage"));
        }
        if (!parameters.isNull("serial")) {
            serial = parameters.optString("serial", "");
            serial = serial.isEmpty() ? null : serial;
        }
    }

    public Integer getVendorId() {
        return vendorId;
    }

    public Integer getProductId() {
        return productId;
    }

    public String getVendorIdString() {
        return vendorIdString;
    }

    public String getProductIdString() {
        return productIdString;
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

    public Integer getUsagePage() {
        return usagePage;
    }

    public String getSerial() {
        return serial;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof UsbOptions)) { return false; }

        UsbOptions that = (UsbOptions)obj;

        if (this.getVendorId().equals(that.getVendorId()) && this.getProductId().equals(that.getProductId())) {
            if (deviceMode == DeviceMode.USB
                    && (this.getInterfaceId() == null || that.getInterfaceId() == null || this.getInterfaceId().equals(that.getInterfaceId()))
                    && (this.getEndpoint() == null || that.getEndpoint() == null || this.getEndpoint().equals(that.getEndpoint()))) {
                return true;
            }

            if (deviceMode == DeviceMode.HID
                    && (this.getUsagePage() == null || that.getUsagePage() == null || this.getUsagePage().equals(that.getUsagePage()))
                    && (this.getSerial() == null || that.getSerial() == null || this.getSerial().equals(that.getSerial()))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(deviceMode)
                .append(vendorId)
                .append(productId)
                .toHashCode();
    }

}
