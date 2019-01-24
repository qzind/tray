package qz.printer.status;

import com.sun.jna.platform.win32.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WMIPrinterStatusThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(StatusMonitor.class);

    private boolean closing = false;
    private final String printerName;
    private final Winspool spool = Winspool.INSTANCE;
    private int lastStatus = -1;

    private WinNT.HANDLE hChangeObject;
    private WinDef.DWORDByReference pdwChangeResult;

    public WMIPrinterStatusThread(String name, int status) {
        super("Printer Status Monitor " + name);
        lastStatus = status;
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
        hChangeObject = spool.FindFirstPrinterChangeNotification(phPrinterObject.getValue(), Winspool.PRINTER_CHANGE_SET_PRINTER, 0, null);
    }

    private void waitOnChange() {
        Kernel32.INSTANCE.WaitForSingleObject(hChangeObject, WinBase.INFINITE);
    }

    private void ingestChange() {
        boolean returnResult;
        returnResult = spool.FindNextPrinterChangeNotification(hChangeObject, pdwChangeResult, null, null);
        if (returnResult) {
            //Requesting an info object every time is required
            int statusCode = WinspoolUtil.getPrinterInfo2(printerName).Status;
            if (lastStatus != statusCode) {
                lastStatus = statusCode;
                PrinterStatus[] statuses = PrinterStatus.getFromWMICode(statusCode, printerName);
                StatusMonitor.statusChanged(statuses);
            }
        } else {
            issueError();
        }
    }

    private void issueError() {
        int errorCode = Kernel32.INSTANCE.GetLastError();
        log.error("WMI Error number: {}, This should be reported", errorCode);
        PrinterStatus[] unknownError = {new PrinterStatus(PrinterStatusType.UNKNOWN_STATUS, printerName)};
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
}
