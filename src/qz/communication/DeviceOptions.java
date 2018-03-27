package qz.communication;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.jettison.json.JSONObject;
import qz.utils.UsbUtilities;

public class DeviceOptions {

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

    public DeviceOptions(JSONObject parameters, DeviceMode deviceMode) {
        this.deviceMode = deviceMode;

        vendorId = UsbUtilities.hexToShort(parameters.optString("vendorId"));
        productId = UsbUtilities.hexToShort(parameters.optString("productId"));

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
            usagePage = UsbUtilities.hexToShort(parameters.optString("usagePage"));
        }
        if (!parameters.isNull("serial")) {
            serial = parameters.optString("serial", "");
            serial = serial.isEmpty() ? null : serial;
        }
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
        if (obj == null || !(obj instanceof DeviceOptions)) { return false; }

        DeviceOptions that = (DeviceOptions)obj;

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
