package qz.printer.status;

import org.eclipse.jetty.websocket.api.Session;
import qz.printer.status.job.JobStatus;
import qz.printer.status.printer.PrinterStatus;
import qz.utils.SystemUtilities;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

public class StatusSession {
    private Session session;

    public StatusSession(Session session) {
        this.session = session;
    }

    public void statusChanged(StatusContainer status) {
        PrintSocketClient.sendStream(session, createStatusStream(status));
    }

    private StreamEvent createStatusStream(StatusContainer status) {
        return new StreamEvent(StreamEvent.Stream.PRINTER, StreamEvent.Type.ACTION)
                .withData("printerName", status.sanitizePrinterName())
                .withData("statusText", status.getCode().name())
                .withData("severity", status.getCode().getLevel())
                .withData("message", status.toString());
    }
}
