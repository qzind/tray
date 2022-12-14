package qz.printer.status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import qz.App;
import qz.printer.status.job.WmiJobStatusMap;
import qz.utils.*;
import qz.ws.PrintSocketClient;
import qz.ws.StreamEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static qz.printer.status.StatusMonitor.ALL_PRINTERS;

public class StatusSession {
    private static final Logger log = LogManager.getLogger(StatusSession.class);
    private Session session;
    private HashMap<String, Spooler> printerSpoolerMap = new HashMap<>();

    private class Spooler implements Cloneable {
        public Path path;
        public int maxJobData;
        public PrintingUtilities.Flavor dataFlavor;

        public Spooler() {
            this(null, -1, PrintingUtilities.Flavor.PLAIN);
        }

        public Spooler(Path path, int maxJobData, PrintingUtilities.Flavor dataFlavor) {
            this.path = path;
            this.maxJobData = maxJobData;
            this.dataFlavor = dataFlavor;
        }

        @Override
        public Spooler clone() {
            return new Spooler(path, maxJobData, dataFlavor);
        }
    }

    public StatusSession(Session session) {
        this.session = session;
    }

    public void statusChanged(Status status) {
        PrintSocketClient.sendStream(session, createStatusStream(status));
        // If this statusSession has printers flagged to return jobData, issue a jobData event after any 'retained' job events
        if (status.getCode() == WmiJobStatusMap.RETAINED.getParent() && isDataPrinter(status.getPrinter())) {
            PrintSocketClient.sendStream(session, createJobDataStream(status));
        }
    }

    public void enableJobDataOnPrinter(String printer, int maxJobData, PrintingUtilities.Flavor dataFlavor) throws UnsupportedOperationException {
        if (!SystemUtilities.isWindows()) {
            throw new UnsupportedOperationException("Job data listeners are only supported on Windows");
        }
        String spoolFileMonitoring = PrefsSearch.get(App.getTrayProperties(), "printer.status.jobdata", "false", false );
        if (!Boolean.parseBoolean(spoolFileMonitoring)) {
            throw new UnsupportedOperationException("Job data listeners are currently disabled");
        }
        if (printerSpoolerMap.containsKey(printer)) {
            printerSpoolerMap.get(printer).maxJobData = maxJobData;
        } else {
            // Lookup spooler path lazily
            printerSpoolerMap.put(printer, new Spooler(null, maxJobData, dataFlavor));
        }
        if (printer.equals(ALL_PRINTERS)) {
            // If we have started job-data listening on all printer, the new parameters need to be added to all existing printers
            for(Map.Entry<String, Spooler> entry : printerSpoolerMap.entrySet()) {
                entry.getValue().maxJobData = maxJobData;
            }
        }
    }

    private StreamEvent createJobDataStream(Status status) {
        StreamEvent streamEvent = new StreamEvent(StreamEvent.Stream.PRINTER, StreamEvent.Type.ACTION)
                .withData("printerName", status.sanitizePrinterName())
                .withData("eventType", Status.EventType.JOB_DATA)
                .withData("jobID", status.getJobId())
                .withData("jobName", status.getJobName())
                .withData("data", getJobData(status.getJobId(), status.getPrinter()));
        return streamEvent;
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

    private String getJobData(int jobId, String printer) {
        String data = null;
        try {
            if (!printerSpoolerMap.containsKey(printer)) {
                // If not listening on this printer, assume we're listening on ALL_PRINTERS
                Spooler spooler;
                if(printerSpoolerMap.containsKey(ALL_PRINTERS)) {
                    spooler = printerSpoolerMap.get(ALL_PRINTERS).clone();
                } else {
                    // we should never get here
                    spooler = new Spooler();
                }
                printerSpoolerMap.put(printer, spooler);
            }
            Spooler spooler = printerSpoolerMap.get(printer);
            if (spooler.path == null) spooler.path = WindowsUtilities.getSpoolerLocation(printer);
            if (spooler.maxJobData != -1 && Files.size(spooler.path) > spooler.maxJobData) {
                throw new IOException("File too large, omitting result. Size:" + Files.size(spooler.path) + " MaxJobData:" + spooler.maxJobData);
            }
            data = spooler.dataFlavor.toString(Files.readAllBytes(spooler.path.resolve(String.format("%05d", jobId) + ".SPL")));
        }
        catch(IOException e) {
            log.error("Failed to retrieve job data from job #{}", jobId, e);
        }
        return data;
    }

    private boolean isDataPrinter(String printer) {
        return (printerSpoolerMap.containsKey(ALL_PRINTERS) || printerSpoolerMap.containsKey(printer));
    }
}
