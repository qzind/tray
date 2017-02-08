package qz.communication;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import purejavahidapi.DeviceRemovalListener;
import purejavahidapi.HidDevice;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import javax.usb.util.UsbUtil;

public class PJHA_HidListener implements DeviceListener, DeviceRemovalListener {

    private static final Logger log = LoggerFactory.getLogger(PJHA_HidListener.class);

    private Session session;
    private HidDevice device;


    public PJHA_HidListener(Session session) {
        this.session = session;
    }

    public void setDevice(HidDevice device) {
        this.device = device;
        device.setDeviceRemovalListener(this);
    }

    private StreamEvent createStreamAction(HidDevice device, String action) {
        return new StreamEvent(StreamEvent.Stream.HID, StreamEvent.Type.ACTION)
                .withData("vendorId", UsbUtil.toHexString(device.getHidDeviceInfo().getVendorId()))
                .withData("productId", UsbUtil.toHexString(device.getHidDeviceInfo().getProductId()))
                .withData("actionType", action);
    }


    @Override
    public void close() {
        device.setDeviceRemovalListener(null);
    }

    @Override
    public void onDeviceRemoval(HidDevice device) {
        log.debug("Device detached: {}", device.getHidDeviceInfo().getProductString());
        PrintSocketClient.sendStream(session, createStreamAction(device, "Device Detached"));
    }
}
