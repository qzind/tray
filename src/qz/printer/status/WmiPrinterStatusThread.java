package qz.printer.status;

import com.sun.jna.platform.win32.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.status.printer.WmiPrinterStatusMap;

import java.util.ArrayList;
import java.util.Arrays;

public class WmiPrinterStatusThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(StatusMonitor.class);

    private boolean closing = false;
    private final String printerName;
    private final Winspool spool = Winspool.INSTANCE;
    private int lastStatus = -1;

    private WinNT.HANDLE hChangeObject;
    private WinDef.DWORDByReference pdwChangeResult;

    public WmiPrinterStatusThread(String name) {
        super("Printer Status Monitor " + name);
        printerName = name;
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
        hChangeObject = spool.FindFirstPrinterChangeNotification(phPrinterObject.getValue(), Winspool.PRINTER_CHANGE_ALL, 0, null);
    }

    private void waitOnChange() {
        Kernel32.INSTANCE.WaitForSingleObject(hChangeObject, WinBase.INFINITE);
    }

    private void ingestChange() {
        if (spool.FindNextPrinterChangeNotification(hChangeObject, pdwChangeResult, null, null)) {
            // Handle printer status changes
            //Requesting an info object every time is required
            int statusCode = WinspoolUtil.getPrinterInfo2(printerName).Status;
            if (lastStatus != statusCode) {
                lastStatus = statusCode;
                Status[] statuses = NativeStatus.fromWmi(statusCode, printerName, NativeStatus.NativeType.PRINTER);
                StatusMonitor.statusChanged(statuses);
            }

            // Handle job status changes
            WinNT.HANDLEByReference phPrinter = new WinNT.HANDLEByReference();
            Winspool.INSTANCE.OpenPrinter(printerName, phPrinter, null);
            for(Winspool.JOB_INFO_1 info : WinspoolUtil.getJobInfo1(phPrinter)) {
                Status[] statuses = NativeStatus.fromWmi(info.Status, printerName, NativeStatus.NativeType.JOB, info.JobId, info.pDocument);
                StatusMonitor.statusChanged(statuses);
            }
        } else {
            issueError();
        }
    }

    private void issueError() {
        int errorCode = Kernel32.INSTANCE.GetLastError();
        log.error("WMI Error number: {}, This should be reported", errorCode);
        Status[] unknownError = {new Status(WmiPrinterStatusMap.UNKNOWN_STATUS, printerName)};
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
            for(Winspool.JOB_INFO_1 jobInfo : WinspoolUtil.getJobInfo1(phPrinter)) {
                statuses.addAll(Arrays.asList(NativeStatus.fromWmi(WinspoolUtil.getPrinterInfo2(printerInfo.pPrinterName).Status, printerInfo.pPrinterName, NativeStatus.NativeType.JOB)));
            }
            statuses.addAll(Arrays.asList(NativeStatus.fromWmi(printerInfo.Status, printerInfo.pPrinterName, NativeStatus.NativeType.PRINTER)));
        }
        return statuses;
    }
}
