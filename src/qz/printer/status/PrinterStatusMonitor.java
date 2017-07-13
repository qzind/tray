package qz.printer.status;

import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.platform.win32.WinspoolUtil;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import static qz.utils.SystemUtilities.isWindows;

/**
 * Created by Kyle on 2/23/2017.
 */

public class PrinterStatusMonitor {
    private static final Logger log = LoggerFactory.getLogger(PrinterStatusMonitor.class);

    private static final HashMap<String, Thread> notificationThreadCollection = new HashMap<String, Thread>();
    public static final List<String> printersListening = new ArrayList<>();

    private static PrinterListener statusListener;

    public static synchronized boolean launchNotificationThreads() {
        boolean printerFound = false;

        if (notificationThreadCollection.isEmpty()) {
            Winspool.PRINTER_INFO_1[] printers = WinspoolUtil.getPrinterInfo1();
            for(int n = 0; n < printers.length; n++) {
                if (printersListening.isEmpty() || printersListening.contains(printers[n].pName)) {
                    printerFound = true;
                    //TODO Remove this debugging log
                    log.warn("Listening for events on printer " + printers[n].pName);
                    Thread notificationThread = new PrinterStatusThread(printers[n].pName);
                    notificationThreadCollection.put(printers[n].pName, notificationThread);
                    notificationThread.start();
                }
            }
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

    public static synchronized boolean startListening (JSONArray printerNames) {
        stopListening();
        if (!printerNames.toString().equals("[null]")) {
            try {
                for(int i = 0; i < printerNames.length(); i++) {
                    printersListening.add(printerNames.getString(i));
                }
            }
            catch(Exception e) {
                log.warn("invalid printer names format for subscription");
            }
        }
        if (isWindows()) {
            return launchNotificationThreads();
        } else {
            CupsStatusServer.runServer();
            return true;
        }
    }

    public static synchronized void addStatusListener (PrinterListener listener) {
        statusListener = listener;
    }

    //todo remove maybe
    public static void removeStatusListener() {
        statusListener = null;
    }

    public static synchronized boolean hasStatusListener() {
        return statusListener != null;
    }

    public static synchronized void stopListening() {
        printersListening.clear();
        if (isWindows()) {
            closeNotificationThreads();
        } else {
            CupsStatusServer.stopServer();
        }
    }

    public static boolean isListeningTo (String PrinterName){
        return printersListening.contains(PrinterName) || printersListening.isEmpty();
    }

    public static void statusChanged (PrinterStatus[] statuses) {
        statusListener.statusChanged(statuses);
    }
}
