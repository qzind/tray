package qz.printer.status;

import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.platform.win32.WinspoolUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Level;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by Kyle on 2/23/2017.
 */

public class PrinterStatusMonitor {
    private static final Logger log = LoggerFactory.getLogger(PrinterStatusMonitor.class);

    private static final HashMap<String, Thread> notificationThreadCollection = new HashMap<String, Thread>();
    private static final List<PrinterListener> statusListeners = new ArrayList<PrinterListener>();

    public static synchronized boolean launchNotificationThreads(JSONArray printerNames) {
        boolean printerFound = false;
        boolean allPrinters = printerNames.isNull(0);
        //Unescaping isn't built into json.toString()
        String nameString = StringEscapeUtils.unescapeJson(printerNames.toString());

        if (notificationThreadCollection.isEmpty()) {
            Winspool.PRINTER_INFO_1[] printers = WinspoolUtil.getPrinterInfo1();
            /*for(int n = 0; n < printers.length; n++) {
                if (allPrinters || nameString.contains("\"" + printers[n].pName + "\"")) {
                    printerFound = true;
                    //TODO Remove this debugging log
                    log.warn("Listening for events on printer " + printers[n].pName);
                    Thread notificationThread = new PrinterStatusThread(printers[n].pName);
                    notificationThreadCollection.put(printers[n].pName, notificationThread);
                    notificationThread.start();
                }
            }*/
        } else {
            log.warn("Attempted to launch printer status notification threads twice, ignoring.");
        }
        return printerFound;
    }

    public static synchronized void closeNotificationThreads() {
        for(Map.Entry<String, Thread> entry : notificationThreadCollection.entrySet()) {
            entry.getValue().interrupt();
        }
        notificationThreadCollection.clear();
    }

    public static synchronized void addStatusListener (PrinterListener listener) {
        statusListeners.add(listener);
    }

    public static void removeStatusListener (PrinterListener listener) {
        statusListeners.remove(listener);
    }

    public static synchronized boolean isListening() {
        return !statusListeners.isEmpty();
    }

    public static synchronized void stopListening() {
        statusListeners.clear();
    }

    public static void statusChanged (PrinterStatus status, String printerName) {
        for (PrinterListener sl: statusListeners) {
            sl.statusChanged(status, printerName);
        }
    }
}
