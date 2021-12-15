package qz.printer.status;

import com.sun.jna.Pointer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final Logger log = LogManager.getLogger(CupsStatusHandler.class);

    private static Cups cups = Cups.INSTANCE;
    private int lastEventNumber = 0;
    private HashMap<String, ArrayList<Status>> lastPrinterStatusMap = new HashMap<>();
    private HashMap<String, ArrayList<Status>> lastJobStatusMap = new HashMap<>();

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        baseRequest.setHandled(true);
        if (request.getReader().readLine() != null) {
            getNotifications();
        }
    }

    private synchronized void getNotifications() {
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
                // Statuses come in blocks eg. {printing, toner_low} We only want to display a status if it didn't exist in the last block
                // Get the list of statuses from the last block associated with this printer
                // '/' Is a documented invalid character for CUPS printer names. We will use that as a separator
                String jobKey = printer + "/" + jobId;
                ArrayList<Status> oldStatuses = lastJobStatusMap.getOrDefault(jobKey, new ArrayList<>());
                ArrayList<Status> newStatuses = new ArrayList<>();

                boolean completed = false;
                int attrCount = cups.ippGetCount(jobStateReasonsAttr);
                for (int i = 0;  i < attrCount; i++) {
                    String reason = cups.ippGetString(jobStateReasonsAttr, i, "");
                    Status pending = NativeStatus.fromCupsJobStatus(reason, jobState, printer, jobId, jobName);
                    // If this status was one we didn't see last block, send it
                    if (!oldStatuses.contains(pending)) statuses.add(pending);
                    // If the job is complete, we need to remove it from our map
                    if ((pending.getCode() == NativeJobStatus.COMPLETE) ||
                            (pending.getCode() == NativeJobStatus.CANCELED)) {
                        completed = true;
                    }
                    // regardless, remember the status for the next block
                    newStatuses.add(pending);
                }
                if (completed) {
                    lastJobStatusMap.remove(jobKey);
                } else {
                    // Replace the old list with the new one
                    lastJobStatusMap.put(jobKey, newStatuses);
                }
            } else if (eventType.startsWith("printer")) {
                Pointer printerStateAttr = cups.ippFindNextAttribute(response, "printer-state", Cups.IPP.TAG_ENUM);
                Pointer printerStateReasonsAttr = cups.ippFindNextAttribute(response, "printer-state-reasons", Cups.IPP.TAG_KEYWORD);
                String state = Cups.INSTANCE.ippEnumString("printer-state", Cups.INSTANCE.ippGetInteger(printerStateAttr, 0));
                // Statuses come in blocks eg. {printing, toner_low} We only want to display a status if it didn't exist in the last block
                // Get the list of statuses from the last block associated with this printer
                ArrayList<Status> oldStatuses = lastPrinterStatusMap.getOrDefault(printer, new ArrayList<>());
                ArrayList<Status> newStatuses = new ArrayList<>();

                int attrCount = cups.ippGetCount(printerStateReasonsAttr);
                for (int i = 0;  i < attrCount; i++) {
                    String reason = cups.ippGetString(printerStateReasonsAttr, i, "");
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
