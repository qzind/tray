package qz.printer.status;

import com.sun.jna.Pointer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by kyle on 4/27/17.
 */
public class CupsStatusHandler extends AbstractHandler {
    private static final Logger log = LoggerFactory.getLogger(CupsStatusHandler.class);

    private static Cups cups = Cups.INSTANCE;
    private int lastEventNumber = 0;

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        baseRequest.setHandled(true);
        if (request.getReader().readLine() != null) {
            getNotifications();
        }
    }

    private void getNotifications() {
        Pointer response = CupsUtils.getStatuses(lastEventNumber + 1);

        Pointer eventNumberAttr = cups.ippFindAttribute(response, "notify-sequence-number", Cups.IPP.TAG_INTEGER);
        Pointer eventTypeAttr = cups.ippFindAttribute(response, "notify-subscribed-event", Cups.IPP.TAG_KEYWORD);
        ArrayList<Status> statuses = new ArrayList<>();

        while (eventNumberAttr != Pointer.NULL) {
            lastEventNumber = cups.ippGetInteger(eventNumberAttr, 0);
            Pointer printerNameAttr = cups.ippFindNextAttribute(response, "printer-name", Cups.IPP.TAG_NAME);

            String printer = cups.ippGetString(printerNameAttr, 0, "");
            String eventType = cups.ippGetString(eventTypeAttr, 0, "");
            if (eventType.startsWith("job")) {
                Pointer JobIdAttr = cups.ippFindNextAttribute(response, "notify-job-id", Cups.IPP.TAG_INTEGER);
                Pointer jobStateAttr = cups.ippFindNextAttribute(response, "job-state", Cups.IPP.TAG_ENUM);
                Pointer jobNameAttr = cups.ippFindNextAttribute(response, "job-name", Cups.IPP.TAG_NAME);
                Pointer jobStateReasonsAttr = cups.ippFindNextAttribute(response, "job-state-reasons", Cups.IPP.TAG_KEYWORD);
                int jobId = cups.ippGetInteger(JobIdAttr, 0);
                String jobState = Cups.INSTANCE.ippEnumString("job-state", Cups.INSTANCE.ippGetInteger(jobStateAttr, 0));
                String jobName = cups.ippGetString(jobNameAttr, 0, "");

                int attrCount = cups.ippGetCount(jobStateReasonsAttr);
                for (int i = 0;  i < attrCount; i++) {
                    String reason = cups.ippGetString(jobStateReasonsAttr, i, "");
                    statuses.add(NativeStatus.fromCupsJobStatus(reason, jobState, printer, jobId, jobName));
                }
            } else if (eventType.startsWith("printer")) {
                Pointer PrinterStateAttr = cups.ippFindNextAttribute(response, "printer-state", Cups.IPP.TAG_ENUM);
                Pointer PrinterStateReasonsAttr = cups.ippFindNextAttribute(response, "printer-state-reasons", Cups.IPP.TAG_KEYWORD);
                String state = Cups.INSTANCE.ippEnumString("printer-state", Cups.INSTANCE.ippGetInteger(PrinterStateAttr, 0));

                int attrCount = cups.ippGetCount(PrinterStateReasonsAttr);
                for (int i = 0;  i < attrCount; i++) {
                    String reason = cups.ippGetString(PrinterStateReasonsAttr, i, "");
                    statuses.add(NativeStatus.fromCupsPrinterStatus(reason, state, printer));
                }
            } else {
                log.debug("Unknown CUPS event type {}.", eventType);
            }
            eventNumberAttr = cups.ippFindNextAttribute(response, "notify-sequence-number", Cups.IPP.TAG_INTEGER);
            eventTypeAttr = cups.ippFindNextAttribute(response, "notify-subscribed-event", Cups.IPP.TAG_KEYWORD);
        }

        cups.ippDelete(response);
        StatusMonitor.statusChanged(statuses.toArray(new Status[statuses.size()]));
    }
}
