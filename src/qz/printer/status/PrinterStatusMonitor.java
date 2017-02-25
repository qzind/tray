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
    static class PrinterStatus{
        String statusText;
        String printerName;
        Level severity;
        int statusCode;

        private PrinterStatus (String statusText, Level severity, int statusCode){
            this.statusText = statusText;
            this.severity = severity;
            this.statusCode = statusCode;
        }

        @Override
        public String toString(){
            return statusText + ": Level " + severity + ", StatusCode " + statusCode + ", From " + printerName;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(PrinterStatusMonitor.class);
    //TODO Replace Map with Enum
    public static final SortedMap<Integer, PrinterStatus> statusLookupTable;

    static {
        SortedMap<Integer, PrinterStatus> tempMap = new TreeMap<Integer, PrinterStatus>();
        tempMap.put(0x00000000, new PrinterStatus("OK", Level.INFO, 0x00000000));
        tempMap.put(0x00000001, new PrinterStatus("PAUSED", Level.WARN, 0x00000001));
        tempMap.put(0x00000002, new PrinterStatus("ERROR", Level.FATAL, 0x00000002));
        tempMap.put(0x00000004, new PrinterStatus("PENDING_DELETION", Level.WARN, 0x00000004));
        tempMap.put(0x00000008, new PrinterStatus("PAPER_JAM", Level.FATAL, 0x00000008));
        tempMap.put(0x00000010, new PrinterStatus("PAPER_OUT", Level.FATAL, 0x00000010));
        tempMap.put(0x00000020, new PrinterStatus("MANUAL_FEED", Level.INFO, 0x00000020));
        tempMap.put(0x00000040, new PrinterStatus("PAPER_PROBLEM", Level.FATAL, 0x00000040));
        tempMap.put(0x00000080, new PrinterStatus("OFFLINE", Level.FATAL, 0x00000080));
        tempMap.put(0x00000100, new PrinterStatus("IO_ACTIVE", Level.INFO, 0x00000100));
        tempMap.put(0x00000200, new PrinterStatus("BUSY", Level.INFO, 0x00000200));
        tempMap.put(0x00000400, new PrinterStatus("PRINTING", Level.INFO, 0x00000400));
        tempMap.put(0x00000800, new PrinterStatus("OUTPUT_BIN_FULL", Level.FATAL, 0x00000800));
        tempMap.put(0x00001000, new PrinterStatus("NOT_AVAILABLE", Level.FATAL, 0x00001000));
        tempMap.put(0x00002000, new PrinterStatus("WAITING", Level.INFO, 0x00002000));
        tempMap.put(0x00004000, new PrinterStatus("PROCESSING", Level.INFO, 0x00004000));
        tempMap.put(0x00008000, new PrinterStatus("INITIALIZING", Level.INFO, 0x00008000));
        tempMap.put(0x00010000, new PrinterStatus("WARMING_UP", Level.INFO, 0x00010000));
        tempMap.put(0x00020000, new PrinterStatus("TONER_LOW", Level.WARN, 0x00020000));
        tempMap.put(0x00040000, new PrinterStatus("NO_TONER", Level.FATAL, 0x00040000));
        tempMap.put(0x00080000, new PrinterStatus("PAGE_PUNT", Level.FATAL, 0x00080000));
        tempMap.put(0x00100000, new PrinterStatus("USER_INTERVENTION", Level.WARN, 0x00100000));
        tempMap.put(0x00200000, new PrinterStatus("OUT_OF_MEMORY", Level.FATAL, 0x00200000));
        tempMap.put(0x00400000, new PrinterStatus("DOOR_OPEN", Level.WARN, 0x00400000));
        tempMap.put(0x00800000, new PrinterStatus("SERVER_UNKNOWN", Level.WARN, 0x00800000));
        tempMap.put(0x01000000, new PrinterStatus("POWER_SAVE", Level.INFO, 0x01000000));
        tempMap.put(0x02000000, new PrinterStatus("UNKNOWN_STATUS", Level.FATAL, 0x02000000));
        statusLookupTable = Collections.unmodifiableSortedMap(tempMap);
    }

    private static PrinterStatus unknownStatus = statusLookupTable.get(0x02000000);
    private static final HashMap<String, Thread> notificationThreadCollection = new HashMap<String, Thread>();
    private static final List<PrinterListener> statusListeners = new ArrayList<PrinterListener>();

    public static synchronized boolean launchNotificationThreads(JSONArray printerNames) {
        boolean printerFound = false;
        boolean allPrinters = printerNames.isNull(0);
        //Unescaping isn't built into json.toString()
        String nameString = StringEscapeUtils.unescapeJson(printerNames.toString());

        if (notificationThreadCollection.isEmpty()) {
            Winspool.PRINTER_INFO_1[] printers = WinspoolUtil.getPrinterInfo1();
            for(int n = 0; n < printers.length; n++) {
                if (allPrinters || nameString.contains("\"" + printers[n].pName + "\"")) {
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

    public static void statusChanged (PrinterStatus status) {
        for (PrinterListener sl: statusListeners) {
            sl.statusChanged(status);
        }
    }

    public static PrinterStatus statusLookup(int code) {
        PrinterStatus resultStatus = statusLookupTable.get(code);
        if (resultStatus == null){
            resultStatus = unknownStatus;
            unknownStatus.statusCode = code;
        }
        return resultStatus;
    }
}
