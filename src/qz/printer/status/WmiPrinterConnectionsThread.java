package qz.printer.status;

import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.platform.win32.WinspoolUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WmiPrinterConnectionsThread extends Thread {

    private static final Logger log = LogManager.getLogger(WmiPrinterConnectionsThread.class);

    private boolean running = true;

    public WmiPrinterConnectionsThread() {
        super("Printer Connection Monitor");
    }

    @Override
    public void run() {
        Winspool.PRINTER_INFO_1[] currentPrinterList = WinspoolUtil.getPrinterInfo1();

        while(running) {
            try { sleep(1000); } catch(Exception ignore) {}

            Winspool.PRINTER_INFO_1[] newPrinterList = WinspoolUtil.getPrinterInfo1();

            if (!arrayEquiv(currentPrinterList, newPrinterList)) {
                StatusMonitor.relaunchThreads();
            }

            currentPrinterList = newPrinterList;
        }
    }

    private boolean arrayEquiv(Winspool.PRINTER_INFO_1[] a, Winspool.PRINTER_INFO_1[] b) {
        if (a.length != b.length) { return false; }

        for(int i = 0; i < a.length; i++) {
            if (!a[i].pName.equals(b[i].pName)) {
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
