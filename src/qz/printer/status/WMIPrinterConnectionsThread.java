package qz.printer.status;

import com.sun.jna.platform.win32.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WMIPrinterConnectionsThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(WMIPrinterConnectionsThread.class);

    private boolean running = true;
    private Winspool.PRINTER_INFO_2[] currentPrinterList;

    public WMIPrinterConnectionsThread() {
        super("Printer Connection Monitor");
    }

    @Override
    public void run() {
        currentPrinterList = WinspoolUtil.getAllPrinterInfo2();
        while (running) {
            try {sleep(1000);} catch (Exception ignore) {}
            Winspool.PRINTER_INFO_2[] newPrinterList = WinspoolUtil.getAllPrinterInfo2();

            if (newPrinterList.length != currentPrinterList.length){
                StatusMonitor.relaunchThreads();
            } else if (!arrayEquiv(currentPrinterList, newPrinterList)) {
                StatusMonitor.relaunchThreads();
            }
            currentPrinterList = newPrinterList;
        }
    }

    private boolean arrayEquiv (Winspool.PRINTER_INFO_2[] a, Winspool.PRINTER_INFO_2[] b) {
        for (int i = 0; i < a.length; i++) {
            if (!a[i].pPrinterName.equals(b[i].pPrinterName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void interrupt() {
        running = false;
        super.interrupt();
    }
}
