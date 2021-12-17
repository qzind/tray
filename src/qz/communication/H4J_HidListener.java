package qz.communication;

import org.codehaus.jettison.json.JSONArray;
import org.eclipse.jetty.websocket.api.Session;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServicesListener;
import org.hid4java.event.HidServicesEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import javax.usb.util.UsbUtil;

public class H4J_HidListener implements DeviceListener, HidServicesListener {

    private static final Logger log = LogManager.getLogger(H4J_HidListener.class);

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
    public void hidDataReceived(HidServicesEvent hidServicesEvent) {
        log.debug("Data received: {}", hidServicesEvent.getDataReceived().length + " bytes");

        JSONArray hex = new JSONArray();
        for(byte b : hidServicesEvent.getDataReceived()) {
            hex.put(UsbUtil.toHexString(b));
        }

        PrintSocketClient.sendStream(session, createStreamAction(hidServicesEvent.getHidDevice(), "Data Received", hex));
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
        return createStreamAction(device, action, null);
    }

    private StreamEvent createStreamAction(HidDevice device, String action, JSONArray dataArr) {
        StreamEvent event = new StreamEvent(StreamEvent.Stream.HID, StreamEvent.Type.ACTION)
                .withData("vendorId", UsbUtil.toHexString(device.getVendorId()))
                .withData("productId", UsbUtil.toHexString(device.getProductId()))
                .withData("actionType", action);

        if (dataArr != null) {
            event.withData("data", dataArr);
        }

        return event;
    }


    @Override
    public void close() {
        HidManager.getHidServices().removeHidServicesListener(this);
    }

}
