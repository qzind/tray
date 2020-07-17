package qz.printer.status;

import org.eclipse.jetty.websocket.api.Session;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

public class StatusSession {
    private Session session;

    public StatusSession(Session session) {
        this.session = session;
    }

    public void statusChanged(PrinterStatus printerStatus) {
        PrintSocketClient.sendStream(session, createStatusStream(printerStatus));
    }

    private StreamEvent createStatusStream(PrinterStatus status) {
        return new StreamEvent(StreamEvent.Stream.PRINTER, StreamEvent.Type.ACTION)
                .withData("printerName", status.getIssuingPrinterName(SystemUtilities.isMac()))
                .withData("statusCode", status.type.getCode())
                .withData("statusText", status.type.getName())
                .withData("severity", status.type.getSeverity())
                .withData("cupsString", status.cupsString)
                .withData("message", status.toString());
    }
}
