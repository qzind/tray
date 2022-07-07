package qz.printer.status;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.PointerByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.printer.status.job.WmiJobStatusMap;
import qz.printer.status.printer.NativePrinterStatus;
import qz.printer.status.printer.WmiPrinterStatusMap;

import java.util.*;

import static qz.printer.status.printer.WmiPrinterStatusMap.*;

public class WmiPrinterStatusThread extends Thread {

    private static final Logger log = LogManager.getLogger(StatusMonitor.class);
    private final Winspool spool = Winspool.INSTANCE;
    private final String printerName;
    private final HashMap<Integer, String> docNames = new HashMap<>();
    private final HashMap<Integer, ArrayList<Integer>> pendingJobStatuses = new HashMap<>();
    private final HashMap<Integer, Integer> lastJobStatusCodes = new HashMap<>();

    private boolean holdsJobs;
    private int statusField;
    private int attributeField;
    // Last "combined" printer status, see also combineStatus()
    private int lastPrinterStatus;

    private boolean wasOk = false;
    private boolean closing = false;

    private WinNT.HANDLE hChangeObject;
    private WinDef.DWORDByReference pdwChangeResult;

    Winspool.PRINTER_NOTIFY_OPTIONS listenOptions;
    Winspool.PRINTER_NOTIFY_OPTIONS statusOptions;

    // Honor translated strings, if available
    private static final ArrayList<String> invalidNames = new ArrayList<>();
    static {
        try {
            invalidNames.add(User32Util.loadString("%SystemRoot%\\system32\\localspl.dll,108"));
            invalidNames.add(User32Util.loadString("%SystemRoot%\\system32\\localspl.dll,107"));
        } catch(Exception e) {
            log.warn("Unable to obtain strings, defaulting to en-US values.", e);
            invalidNames.add("Local Downlevel Document");
            invalidNames.add("Remote Downlevel Document");
        }
    }

    public WmiPrinterStatusThread(Winspool.PRINTER_INFO_2 printerInfo2) {
        super("Printer Status Monitor " + printerInfo2.pPrinterName);
        printerName = printerInfo2.pPrinterName;
        holdsJobs = (printerInfo2.Attributes & Winspool.PRINTER_ATTRIBUTE_KEEPPRINTEDJOBS) > 0;
        statusField = printerInfo2.Status;
        attributeField = printerInfo2.Attributes;
        lastPrinterStatus = combineStatus(statusField, attributeField);

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
            // Many events fire with dataPointer == null, see also https://stackoverflow.com/questions/16283827
            if (dataPointer.getValue() != null) {
                Winspool.PRINTER_NOTIFY_INFO data = Structure.newInstance(Winspool.PRINTER_NOTIFY_INFO.class, dataPointer.getValue());
                data.read();

                for (Winspool.PRINTER_NOTIFY_INFO_DATA d: data.aData) {
                    decodeStatus(d);
                }
                sendPendingStatuses();
                Winspool.INSTANCE.FreePrinterNotifyInfo(data.getPointer());
            }
        } else {
            issueError();
        }
    }

    private void decodeStatus(Winspool.PRINTER_NOTIFY_INFO_DATA d) {
        if (d.Type == Winspool.PRINTER_NOTIFY_TYPE) {
            if (d.Field == Winspool.PRINTER_NOTIFY_FIELD_STATUS) { // Printer Status Changed
                statusField = d.NotifyData.adwData[0];
            } else if (d.Field == Winspool.PRINTER_NOTIFY_FIELD_ATTRIBUTES) { // Printer Attributes Changed
                attributeField = d.NotifyData.adwData[0];
                holdsJobs = (d.NotifyData.adwData[0] & Winspool.PRINTER_ATTRIBUTE_KEEPPRINTEDJOBS) != 0;
            } else {
                log.warn("Unknown event field {}", d.Field);
            }

            int combinedStatus = combineStatus(statusField, attributeField);
            if (combinedStatus != lastPrinterStatus) {
                Status[] statuses = NativeStatus.fromWmiPrinterStatus(combinedStatus, printerName);
                StatusMonitor.statusChanged(statuses);

                // If the printer was in an error state before and is not now, send an 'OK'
                boolean isOk = (combinedStatus & NOT_OK_MASK) == 0;
                if (isOk && !wasOk) {
                    // If the status is 0x00000000, fromWmiPrinterStatus returns 'OK'. We don't want to send a duplicate.
                    if (combinedStatus != 0) StatusMonitor.statusChanged(new Status[]{new Status(NativePrinterStatus.OK, printerName, 0)});
                }
                wasOk = isOk;

                lastPrinterStatus = combinedStatus;
            }
        } else if (d.Type == Winspool.JOB_NOTIFY_TYPE) {
            // Job Name Set or Changed
            if (d.Field == Winspool.JOB_NOTIFY_FIELD_DOCUMENT) {
                // The element containing our Doc name is not always the first item of the event
                // The Job name is only sent once, catalog it for later statuses
                docNames.put(d.Id, d.NotifyData.Data.pBuf.getWideString(0));
            // Job Status Changed
            } else if (d.Field == Winspool.JOB_NOTIFY_FIELD_STATUS) {
                //If there is no list for a given ID, create a new one and add it to the collection under said ID
                ArrayList<Integer> statusList = pendingJobStatuses.computeIfAbsent(d.Id, k -> new ArrayList<>());
                statusList.add(d.NotifyData.adwData[0]);
            }
        }
    }

    /**
     * Bitwise-safe combination of statusField and attributeField's PRINTER_ATTRIBUTE_WORK_OFFLINE.
     *
     * Due to PRINTER_ATTRIBUTE_WORK_OFFLINE's overlapping bitwise value, we must use a
     * non-overlapping value, ATTRIBUTE_WORK_OFFLINE.
     *
     * See also: https://stackoverflow.com/questions/41437023
     */
    private static int combineStatus(int statusField, int attributeField) {
        int workOfflineFlag = (attributeField & Winspool.PRINTER_ATTRIBUTE_WORK_OFFLINE) == 0 ? 0 : (int)WmiPrinterStatusMap.ATTRIBUTE_WORK_OFFLINE.getRawCode();
        return statusField | workOfflineFlag;
    }

    private void sendPendingStatuses() {
        if (pendingJobStatuses.size() == 0) return;
        for (Iterator<Map.Entry<Integer, ArrayList<Integer>>> i = pendingJobStatuses.entrySet().iterator(); i.hasNext();) {
            Map.Entry<Integer, ArrayList<Integer>> jobCodesEntry = i.next();
            ArrayList<Integer> codes = jobCodesEntry.getValue();
            int jobId = jobCodesEntry.getKey();

            // Wait until we have a real docName
            if (invalidNames.contains(docNames.get(jobId))) continue;

            // Workaround for double 'printed' statuses
            if (holdsJobs && docNames.get(jobId) == null && codes.size() == 1 && codes.get(0) == (int)WmiJobStatusMap.PRINTED.getRawCode()) {
                i.remove();
                lastJobStatusCodes.remove(jobId);
                continue;
            }

            for (int code: codes) {
                int oldStatusCode = lastJobStatusCodes.getOrDefault(jobId, 0);

                // This only sets status flags if they are not in oldStatusCode
                int statusToReport = code & (~oldStatusCode);
                if (statusToReport != 0) {
                    StatusMonitor.statusChanged(NativeStatus.fromWmiJobStatus(statusToReport, printerName, jobId, docNames.get(jobId)));
                }
                lastJobStatusCodes.put(jobId, code);
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
        for(Winspool.PRINTER_INFO_2 printerInfo2 : wmiPrinters) {
            WinNT.HANDLEByReference phPrinter = new WinNT.HANDLEByReference();
            Winspool.INSTANCE.OpenPrinter(printerInfo2.pPrinterName, phPrinter, null);
            for(Winspool.JOB_INFO_1 info : WinspoolUtil.getJobInfo1(phPrinter)) {
                Collections.addAll(statuses, NativeStatus.fromWmiJobStatus(info.Status, printerInfo2.pPrinterName, info.JobId, info.pDocument));
            }
            Collections.addAll(statuses, NativeStatus.fromWmiPrinterStatus(combineStatus(printerInfo2.Status, printerInfo2.Attributes), printerInfo2.pPrinterName));
        }
        return statuses;
    }
}
