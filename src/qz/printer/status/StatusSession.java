package qz.printer.status;

import org.eclipse.jetty.websocket.api.Session;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

public class StatusSession {
    private Session session;

    public StatusSession(Session session) {
        this.session = session;
    }

    public void statusChanged(Status status) {
        PrintSocketClient.sendStream(session, createStatusStream(status));
    }

    private StreamEvent createStatusStream(Status status) {
        StreamEvent streamEvent = new StreamEvent(StreamEvent.Stream.PRINTER, StreamEvent.Type.ACTION)
                .withData("printerName", status.sanitizePrinterName())
                .withData("eventType", status.getEventType())
                .withData("statusText", status.getCode().name())
                .withData("severity", status.getCode().getLevel())
                .withData("statusCode", status.getRawCode())
                .withData("message", status.toString());
        if(status.getJobId() > 0) {
            streamEvent.withData("jobId", status.getJobId());
        }
        if(status.getJobName() != null) {
            streamEvent.withData("jobName", status.getJobName());
        }
        return streamEvent;
    }
}
