package qz.printer.status;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.communication.DeviceListener;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import java.util.EventListener;

public class PrinterStatusListener implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(PrinterStatusListener.class);
    private Session session;

    public PrinterStatusListener(Session session) {
        this.session = session;
    }

    public void statusChanged (PrinterStatus printerStatus) {
        PrintSocketClient.sendStream(session, createStatusStream(printerStatus));
    }

    private StreamEvent createStatusStream(PrinterStatus status) {
        return new StreamEvent(StreamEvent.Stream.PRINTER, StreamEvent.Type.ACTION)
                .withData("printerName", status.issuingPrinterName)
                .withData("statusCode", status.type.getCode())
                .withData("statusText", status.type.getName())
                .withData("severity", status.type.getSeverity())
                .withData("cupsString", status.getCupsString())
                .withData("message", status.toString());
    }
}
