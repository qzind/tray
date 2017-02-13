package qz.communication;

import org.eclipse.jetty.websocket.api.Session;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServicesListener;
import org.hid4java.event.HidServicesEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import javax.usb.util.UsbUtil;

public class H4J_HidListener implements DeviceListener, HidServicesListener {

    private static final Logger log = LoggerFactory.getLogger(H4J_HidListener.class);

    private Session session;


    public H4J_HidListener(Session session) {
        HidManager.getHidServices().addHidServicesListener(this);

        this.session = session;
    }


    @Override
    public void hidFailure(HidServicesEvent hidServicesEvent) {
        log.debug("Device failure: {}", hidServicesEvent.getHidDevice().getProduct());
        PrintSocketClient.sendStream(session, createStreamAction(hidServicesEvent.getHidDevice(), "Device Failure"));
    }

    @Override
    public void hidDeviceDetached(HidServicesEvent hidServicesEvent) {
        log.debug("Device detached: {}", hidServicesEvent.getHidDevice().getProduct());
        PrintSocketClient.sendStream(session, createStreamAction(hidServicesEvent.getHidDevice(), "Device Detached"));
    }

    @Override
    public void hidDeviceAttached(HidServicesEvent hidServicesEvent) {
        log.debug("Device attached: {}", hidServicesEvent.getHidDevice().getProduct());
        PrintSocketClient.sendStream(session, createStreamAction(hidServicesEvent.getHidDevice(), "Device Attached"));
    }

    private StreamEvent createStreamAction(HidDevice device, String action) {
        return new StreamEvent(StreamEvent.Stream.HID, StreamEvent.Type.ACTION)
                .withData("vendorId", UsbUtil.toHexString(device.getVendorId()))
                .withData("productId", UsbUtil.toHexString(device.getProductId()))
                .withData("actionType", action);
    }


    @Override
    public void close() {
        HidManager.getHidServices().removeUsbServicesListener(this);
    }

}
