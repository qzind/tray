package qz.printer.status;

import com.sun.jna.Pointer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.status.job.NativeJobStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kyle on 4/27/17.
 */
public class CupsStatusHandler extends AbstractHandler {
    private static final Logger log = LoggerFactory.getLogger(CupsStatusHandler.class);

    private static Cups cups = Cups.INSTANCE;
    private int lastEventNumber = 0;
    //todo could perhaps be the same list
    private HashMap<String, ArrayList<Status>> lastPrinterStatusMap = new HashMap<>();
    private HashMap<String, ArrayList<Status>> lastJobStatusMap = new HashMap<>();

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

                // todo: this should be job url maybe
                ArrayList<Status> oldStatuses = lastJobStatusMap.getOrDefault(printer + jobId, new ArrayList<>());
                ArrayList<Status> newStatuses = new ArrayList<>();

                boolean completed = false;
                int attrCount = cups.ippGetCount(jobStateReasonsAttr);
                for (int i = 0;  i < attrCount; i++) {
                    String reason = cups.ippGetString(jobStateReasonsAttr, i, "");
                    Status status = NativeStatus.fromCupsJobStatus(reason, jobState, printer, jobId, jobName);
                    if (!oldStatuses.contains(status)) statuses.add(status);
                    if (status.getCode() == NativeJobStatus.COMPLETE) completed = true;
                }
                if (completed) {
                    lastJobStatusMap.remove(printer + jobId);
                } else {
                    lastJobStatusMap.put(printer + jobId, newStatuses);
                }
            } else if (eventType.startsWith("printer")) {
                Pointer PrinterStateAttr = cups.ippFindNextAttribute(response, "printer-state", Cups.IPP.TAG_ENUM);
                Pointer PrinterStateReasonsAttr = cups.ippFindNextAttribute(response, "printer-state-reasons", Cups.IPP.TAG_KEYWORD);
                String state = Cups.INSTANCE.ippEnumString("printer-state", Cups.INSTANCE.ippGetInteger(PrinterStateAttr, 0));
                // Statuses come in blocks eg. {printing, toner_low} We only want to display a status if it didn't exist in the last block
                // Get the list of statuses from the last block associated with this printer
                ArrayList<Status> oldStatuses = lastPrinterStatusMap.getOrDefault(printer, new ArrayList<>());
                ArrayList<Status> newStatuses = new ArrayList<>();

                int attrCount = cups.ippGetCount(PrinterStateReasonsAttr);
                for (int i = 0;  i < attrCount; i++) {
                    String reason = cups.ippGetString(PrinterStateReasonsAttr, i, "");
                    Status pending = NativeStatus.fromCupsPrinterStatus(reason, state, printer);
                    // If this status was one we didn't see last block, send it
                    if (!oldStatuses.contains(pending)) statuses.add(pending);
                    // regardless, remember the status for the next block
                    newStatuses.add(pending);
                }
                // Replace the old list with the new one
                lastPrinterStatusMap.put(printer, newStatuses);
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
