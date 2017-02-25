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

    public void statusChanged (PrinterStatusMonitor.PrinterStatus status) {
        PrintSocketClient.sendStream(session, createStatusStream(status));
    }

    private StreamEvent createStatusStream(PrinterStatusMonitor.PrinterStatus status) {
        //TODO Put real data here
        return new StreamEvent(StreamEvent.Stream.PRINTER, StreamEvent.Type.ACTION)
                .withData("printerName", status.printerName)
                .withData("statusCode", status.statusCode)
                .withData("statusText", status.statusText)
                .withData("severity", status.severity.toString())
                .withData("message", status.toString());
    }
}
