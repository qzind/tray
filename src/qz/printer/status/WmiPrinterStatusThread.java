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

import static com.sun.jna.platform.win32.Winspool.JOB_NOTIFY_FIELD_DOCUMENT;
import static com.sun.jna.platform.win32.Winspool.JOB_NOTIFY_FIELD_STATUS;
import static com.sun.jna.platform.win32.Winspool.PRINTER_NOTIFY_FIELD_STATUS;
import static org.apache.log4j.Level.*;

public class WmiPrinterStatusThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(StatusMonitor.class);

    private boolean closing = false;
    private final String printerName;
    private final Winspool spool = Winspool.INSTANCE;
    private int lastPrinterStatus = -1;

    private WinNT.HANDLE hChangeObject;
    private WinDef.DWORDByReference pdwChangeResult;

    private HashMap<Integer, String> docNames = new HashMap<>();
    private HashMap<Integer, ArrayList<Integer>> pendingJobStatuses = new HashMap<>();
    private HashMap<Integer, Integer> lastJobStatusCodes = new HashMap<>();

    // Printer status isn't very good about reporting recovered errors, we'll try to track them manually
    private static ArrayList<NativeStatus> notOk = new ArrayList<>(Arrays.asList(
            NativePrinterStatus.PAUSED,
            NativePrinterStatus.ERROR,
            NativePrinterStatus.PAPER_JAM,
            NativePrinterStatus.PAPER_OUT,
            NativePrinterStatus.MANUAL_FEED,
            NativePrinterStatus.PAPER_PROBLEM,
            NativePrinterStatus.OFFLINE,
            NativePrinterStatus.OUTPUT_BIN_FULL,
            NativePrinterStatus.NOT_AVAILABLE,
            NativePrinterStatus.NO_TONER,
            NativePrinterStatus.USER_INTERVENTION,
            NativePrinterStatus.OUT_OF_MEMORY,
            NativePrinterStatus.DOOR_OPEN,
            NativePrinterStatus.SERVER_UNKNOWN,
            NativePrinterStatus.UNMAPPED
    ));

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
        mem[0].setFields(new short[] { JOB_NOTIFY_FIELD_STATUS, JOB_NOTIFY_FIELD_DOCUMENT });
        mem[1].Type = Winspool.PRINTER_NOTIFY_TYPE;
        mem[1].setFields(new short[] { PRINTER_NOTIFY_FIELD_STATUS });
        listenOptions.pTypes = mem[0];

        statusOptions = new Winspool.PRINTER_NOTIFY_OPTIONS();
        statusOptions.Version = 2;
        //statusOptions.Flags = Winspool.PRINTER_NOTIFY_OPTIONS_REFRESH;
        statusOptions.Count = 2;

        mem = (Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference[])
                new Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference().toArray(2);
        mem[0].Type = Winspool.JOB_NOTIFY_TYPE;
        mem[0].setFields(new short[] { JOB_NOTIFY_FIELD_STATUS, JOB_NOTIFY_FIELD_DOCUMENT });
        mem[1].Type = Winspool.PRINTER_NOTIFY_TYPE;
        mem[1].setFields(new short[] { PRINTER_NOTIFY_FIELD_STATUS });
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
            if (d.Field == PRINTER_NOTIFY_FIELD_STATUS) {
                if (d.NotifyData.adwData[0] !=lastPrinterStatus) {
                    Status[] statuses = NativeStatus.fromWmiPrinterStatus(d.NotifyData.adwData[0], printerName);
                    for(Status status : statuses) {
                        if(notOk.contains(status)) {
                            log.info("{} reported {} and that it's not OK, we'll send a dedicated OK when it clears", printerName, status);
                            // TODO: actually implement what the above message says
                        }
                    }
                    StatusMonitor.statusChanged(statuses);
                    lastPrinterStatus = d.NotifyData.adwData[0];
                }
            } else {
                // todo delete this
                log.warn("Unknown event field {}", d.Field);
            }
        }
        // The element containing our Doc name is not always the first item
        if (d.Type == Winspool.JOB_NOTIFY_TYPE && d.Field == JOB_NOTIFY_FIELD_DOCUMENT) {
            docNames.put(d.Id, d.NotifyData.Data.pBuf.getWideString(0));
        } else if (d.Field == JOB_NOTIFY_FIELD_STATUS) {
            ArrayList<Integer> statusList = pendingJobStatuses.get(d.Id);

            int newStatusCode = d.NotifyData.adwData[0];
            int oldStatusCode = lastJobStatusCodes.getOrDefault(d.Id, 0);

            // This only sets status flags if they are not in oldStatusCode
            int statusToReport = newStatusCode & (~oldStatusCode);
            if (statusToReport != 0) {
                if  (statusList == null) {
                    statusList = new ArrayList<>();
                    pendingJobStatuses.put(d.Id, statusList);
                }
                statusList.add(statusToReport);
            }

            lastJobStatusCodes.put(d.Id, newStatusCode);
        }
    }

    private void sendPendingStatuses() {
        if (pendingJobStatuses.size() == 0) return;
        for (Iterator<Map.Entry<Integer, ArrayList<Integer>>> i = pendingJobStatuses.entrySet().iterator(); i.hasNext();) {
            Map.Entry<Integer, ArrayList<Integer>> jobCodesEntry = i.next();
            ArrayList<Integer> codes = jobCodesEntry.getValue();
            int jobId = jobCodesEntry.getKey();

            for (int code: codes) {
                StatusMonitor.statusChanged(NativeStatus.fromWmiJobStatus(code, printerName, jobId, docNames.get(jobId)));
            }
            i.remove();

            // If the job was deleted, remove it from our lists
            if ((codes.get(codes.size() - 1) & (int)WmiJobStatusMap.DELETED.getRawCode()) != 0) {
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
