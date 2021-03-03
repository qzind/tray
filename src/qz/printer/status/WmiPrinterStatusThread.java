package qz.printer.status;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.status.job.WmiJobStatusMap;
import qz.printer.status.printer.NativePrinterStatus;
import qz.printer.status.printer.WmiPrinterStatusMap;

import java.util.*;

public class WmiPrinterStatusThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(StatusMonitor.class);

    private boolean closing = false;
    private final String printerName;
    private final Winspool spool = Winspool.INSTANCE;
    private int lastPrinterStatus = -1;
    private boolean wasOk = false;
    private boolean holdsJobs = false;

    private WinNT.HANDLE hChangeObject;
    private WinDef.DWORDByReference pdwChangeResult;

    private HashMap<Integer, String> docNames = new HashMap<>();
    private HashMap<Integer, ArrayList<Integer>> pendingJobStatuses = new HashMap<>();
    private HashMap<Integer, Integer> lastJobStatusCodes = new HashMap<>();

    // Printer status isn't very good about reporting recovered errors, we'll try to track them manually
    private static final int notOkMask =
            (int)WmiPrinterStatusMap.PAUSED.getRawCode() |
            (int)WmiPrinterStatusMap.ERROR.getRawCode() |
            (int)WmiPrinterStatusMap.PAPER_JAM.getRawCode() |
            (int)WmiPrinterStatusMap.PAPER_OUT.getRawCode() |
            (int)WmiPrinterStatusMap.MANUAL_FEED.getRawCode() |
            (int)WmiPrinterStatusMap.PAPER_PROBLEM.getRawCode() |
            (int)WmiPrinterStatusMap.OFFLINE.getRawCode() |
            (int)WmiPrinterStatusMap.OUTPUT_BIN_FULL.getRawCode() |
            (int)WmiPrinterStatusMap.NOT_AVAILABLE.getRawCode() |
            (int)WmiPrinterStatusMap.NO_TONER.getRawCode() |
            (int)WmiPrinterStatusMap.USER_INTERVENTION.getRawCode() |
            (int)WmiPrinterStatusMap.OUT_OF_MEMORY.getRawCode() |
            (int)WmiPrinterStatusMap.DOOR_OPEN.getRawCode() |
            (int)WmiPrinterStatusMap.SERVER_UNKNOWN.getRawCode() |
            (int)WmiPrinterStatusMap.UNKNOWN_STATUS.getRawCode();

    Winspool.PRINTER_NOTIFY_OPTIONS listenOptions;
    Winspool.PRINTER_NOTIFY_OPTIONS statusOptions;

    public WmiPrinterStatusThread(String name) {
        super("Printer Status Monitor " + name);
        printerName = name;

        listenOptions = new Winspool.PRINTER_NOTIFY_OPTIONS();
        listenOptions.Version = 2;
        listenOptions.Flags = Winspool.PRINTER_NOTIFY_OPTIONS_REFRESH;
        listenOptions.Count = 2;

        Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference[] mem = (Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference[])
                new Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference().toArray(2);
        mem[0].Type = Winspool.JOB_NOTIFY_TYPE;
        mem[0].setFields(new short[] {Winspool.JOB_NOTIFY_FIELD_STATUS, Winspool.JOB_NOTIFY_FIELD_DOCUMENT });
        mem[1].Type = Winspool.PRINTER_NOTIFY_TYPE;
        mem[1].setFields(new short[] {Winspool.PRINTER_NOTIFY_FIELD_STATUS, Winspool.PRINTER_NOTIFY_FIELD_ATTRIBUTES });
        listenOptions.pTypes = mem[0];

        statusOptions = new Winspool.PRINTER_NOTIFY_OPTIONS();
        statusOptions.Version = 2;
        // Status option 'refresh' leads to a loss of data associated with our lock. I don't know why.
        // statusOptions.Flags = Winspool.PRINTER_NOTIFY_OPTIONS_REFRESH;
        statusOptions.Count = 2;

        mem = (Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference[])
                new Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference().toArray(2);
        mem[0].Type = Winspool.JOB_NOTIFY_TYPE;
        mem[0].setFields(new short[] { Winspool.JOB_NOTIFY_FIELD_STATUS, Winspool.JOB_NOTIFY_FIELD_DOCUMENT });
        mem[1].Type = Winspool.PRINTER_NOTIFY_TYPE;
        mem[1].setFields(new short[] { Winspool.PRINTER_NOTIFY_FIELD_STATUS, Winspool.PRINTER_NOTIFY_FIELD_ATTRIBUTES });
        statusOptions.pTypes = mem[0];
    }

    @Override
    public void run() {

        attachToSystem();

        if (hChangeObject != null) {
            while(!closing) {
                waitOnChange();
                if (closing) { break; }
                ingestChange();
            }
        }
    }

    private void attachToSystem() {
        WinNT.HANDLEByReference phPrinterObject = new WinNT.HANDLEByReference();
        spool.OpenPrinter(printerName, phPrinterObject, null);

        // Determine if the printer deletes jobs after printing
        // todo this should probably be done once, instead of once for every printer thread
        for (Winspool.PRINTER_INFO_2 printerInfo : WinspoolUtil.getPrinterInfo2()) {
            if (printerInfo.pPrinterName.equals(printerName)) {
                holdsJobs = (printerInfo.Attributes & Winspool.PRINTER_ATTRIBUTE_KEEPPRINTEDJOBS) > 0;
            }
        }

        pdwChangeResult = new WinDef.DWORDByReference();
        //The second param determines what kind of event releases our lock
        //See https://msdn.microsoft.com/en-us/library/windows/desktop/dd162722(v=vs.85).aspx
        hChangeObject = spool.FindFirstPrinterChangeNotification(phPrinterObject.getValue(), Winspool.PRINTER_CHANGE_JOB, 0, listenOptions);
    }

    private void waitOnChange() {
        Kernel32.INSTANCE.WaitForSingleObject(hChangeObject, WinBase.INFINITE);
    }

    private void ingestChange() {
        PointerByReference dataPointer = new PointerByReference();
        if (spool.FindNextPrinterChangeNotification(hChangeObject, pdwChangeResult, statusOptions, dataPointer)) {
            if (dataPointer.getValue() != null) {
                Winspool.PRINTER_NOTIFY_INFO data = Structure.newInstance(Winspool.PRINTER_NOTIFY_INFO.class, dataPointer.getValue());
                data.read();

                for (Winspool.PRINTER_NOTIFY_INFO_DATA d: data.aData) {
                    decodeJobStatus(d);
                }
                sendPendingStatuses();
                Winspool.INSTANCE.FreePrinterNotifyInfo(data.getPointer());
            } else {
                //Todo why do we end up here so often, what causes dataPointer to be null?
            }
        } else {
            issueError();
        }
    }

    private void decodeJobStatus(Winspool.PRINTER_NOTIFY_INFO_DATA d) {
        if (d.Type == Winspool.PRINTER_NOTIFY_TYPE) {
            // Printer Status Changed
            if (d.Field == Winspool.PRINTER_NOTIFY_FIELD_STATUS) {
                if (d.NotifyData.adwData[0] != lastPrinterStatus) {
                    Status[] statuses = NativeStatus.fromWmiPrinterStatus(d.NotifyData.adwData[0], printerName);
                    StatusMonitor.statusChanged(statuses);

                    // If the printer was in an error state before and is not now, send an 'OK'
                    boolean isOk = (d.NotifyData.adwData[0] & notOkMask) == 0;
                    if (isOk && !wasOk) {
                        // If the status is 0x00000000, fromWmiPrinterStatus returns 'OK'. We don't want to send a duplicate.
                        if (d.NotifyData.adwData[0] != 0) StatusMonitor.statusChanged(new Status[]{new Status(NativePrinterStatus.OK, printerName, 0)});
                    }
                    wasOk = isOk;

                    lastPrinterStatus = d.NotifyData.adwData[0];
                }
            // Printer Attributes Changed
            } else if (d.Field == Winspool.PRINTER_NOTIFY_FIELD_ATTRIBUTES) {
                // Check to see if "hold jobs" changed
                holdsJobs = (d.NotifyData.adwData[0] & Winspool.PRINTER_ATTRIBUTE_KEEPPRINTEDJOBS) > 0;
            } else {
                // todo delete this
                log.warn("Unknown event field {}", d.Field);
            }
        } else if (d.Type == Winspool.JOB_NOTIFY_TYPE) {
            // Job Name Set or Changed
            if (d.Field == Winspool.JOB_NOTIFY_FIELD_DOCUMENT) {
                // The element containing our Doc name is not always the first item of the event
                // The Job name is only sent once, catalog it for later statuses
                docNames.put(d.Id, d.NotifyData.Data.pBuf.getWideString(0));
            // Job Status Changed
            } else if (d.Field == Winspool.JOB_NOTIFY_FIELD_STATUS) {
                ArrayList<Integer> statusList = pendingJobStatuses.get(d.Id);
                if (statusList == null) {
                    statusList = new ArrayList<>();
                    pendingJobStatuses.put(d.Id, statusList);
                }
                //Todo Remove this debugging log
                log.warn("code {}", d.NotifyData.adwData[0]);

                statusList.add(d.NotifyData.adwData[0]);
            }
        }
    }

    private void sendPendingStatuses() {
        if (pendingJobStatuses.size() == 0) return;
        for (Iterator<Map.Entry<Integer, ArrayList<Integer>>> i = pendingJobStatuses.entrySet().iterator(); i.hasNext();) {
            Map.Entry<Integer, ArrayList<Integer>> jobCodesEntry = i.next();
            ArrayList<Integer> codes = jobCodesEntry.getValue();
            int jobId = jobCodesEntry.getKey();

            for (int code: codes) {
                int newStatusCode = code;
                int oldStatusCode = lastJobStatusCodes.getOrDefault(jobId, 0);

                // This only sets status flags if they are not in oldStatusCode
                int statusToReport = newStatusCode & (~oldStatusCode);
                if (statusToReport != 0) {
                    StatusMonitor.statusChanged(NativeStatus.fromWmiJobStatus(statusToReport, printerName, jobId, docNames.get(jobId)));
                }
                lastJobStatusCodes.put(jobId, newStatusCode);
            }
            i.remove();


            int code = codes.get(codes.size() - 1);
            boolean isFinalCode = (code & (int)WmiJobStatusMap.DELETED.getRawCode()) > 0;

            // If the printer holds jobs, the last event we will see is 'printed' or 'deleted' and not 'printing', otherwise it will be just 'deleted'.
            if (holdsJobs) {
                isFinalCode |= (code & (int)WmiJobStatusMap.PRINTED.getRawCode()) > 0;
                isFinalCode &= (code & (int)WmiJobStatusMap.PRINTING.getRawCode()) == 0;
            }
            // If that was the last status we will see from a job, remove it from our lists.
            if (isFinalCode) {
                docNames.remove(jobId);
                lastJobStatusCodes.remove(jobId);
            }
            //Todo Remove this debugging log
            log.warn("We remember {} jobs", docNames.size());
        }
    }

    private void issueError() {
        int errorCode = Kernel32.INSTANCE.GetLastError();
        log.error("WMI Error number: {}, This should be reported", errorCode);
        Status[] unknownError = { new Status(NativePrinterStatus.UNMAPPED, printerName, WmiPrinterStatusMap.UNKNOWN_STATUS.getRawCode()) };
        StatusMonitor.statusChanged(unknownError);
        try {
            //if the error repeats, we don't want to lock up the cpu
            Thread.sleep(1000);
        }
        catch(Exception ignore) {}
    }

    @Override
    public void interrupt() {
        closing = true;
        spool.FindClosePrinterChangeNotification(hChangeObject);
        super.interrupt();
    }

    public static ArrayList<Status> getAllStatuses() {
        ArrayList<Status> statuses = new ArrayList<>();
        Winspool.PRINTER_INFO_2[] wmiPrinters = WinspoolUtil.getPrinterInfo2();
        for(Winspool.PRINTER_INFO_2 printerInfo : wmiPrinters) {
            WinNT.HANDLEByReference phPrinter = new WinNT.HANDLEByReference();
            Winspool.INSTANCE.OpenPrinter(printerInfo.pPrinterName, phPrinter, null);
            for(Winspool.JOB_INFO_1 info : WinspoolUtil.getJobInfo1(phPrinter)) {
                statuses.addAll(Arrays.asList(NativeStatus.fromWmiJobStatus(info.Status, printerInfo.pPrinterName, info.JobId, info.pDocument)));
            }
            statuses.addAll(Arrays.asList(NativeStatus.fromWmiPrinterStatus(printerInfo.Status, printerInfo.pPrinterName)));
        }
        return statuses;
    }
}
