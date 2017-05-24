package qz.printer.status;

import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import java.util.EventListener;

public class PrinterListener implements EventListener{

    private static final Logger log = LoggerFactory.getLogger(PrinterListener.class);
    private Session session;

    public PrinterListener(Session session) {
        this.session = session;
    }

    public void statusChanged (PrinterStatus status, String printerName) {
        PrintSocketClient.sendStream(session, createStatusStream(status, printerName));
    }

    private StreamEvent createStatusStream(PrinterStatus status, String printerName) {
        //TODO Put real data here
        return new StreamEvent(StreamEvent.Stream.PRINTER, StreamEvent.Type.ACTION)
                .withData("printerName", printerName)
                .withData("statusCode", status.getCode())
                .withData("statusText", status.getName())
                .withData("severity", status.getSeverity())
                .withData("message", status.toString());
    }
}
