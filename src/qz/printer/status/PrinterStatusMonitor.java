package qz.printer.status;

import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.platform.win32.WinspoolUtil;
import org.codehaus.jettison.json.JSONArray;
import org.eclipse.jetty.util.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.ws.SocketConnection;

import java.util.*;
import static qz.utils.SystemUtilities.isWindows;

/**
 * Created by Kyle on 2/23/2017.
 */

public class PrinterStatusMonitor {
    private static final Logger log = LoggerFactory.getLogger(PrinterStatusMonitor.class);

    private static final HashMap<String, Thread> notificationThreadCollection = new HashMap<>();
    private static final MultiMap<SocketConnection> clientPrinterConnections = new MultiMap<>();
    public static final List<String> printersListening = new ArrayList<>();

    public synchronized static boolean launchNotificationThreads() {
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

    public synchronized static void closeNotificationThreads() {
        for(Map.Entry<String, Thread> entry : notificationThreadCollection.entrySet()) {
            entry.getValue().interrupt();
        }
        notificationThreadCollection.clear();
    }

    public synchronized static boolean startListening (SocketConnection connection, JSONArray printerNames) {
        try {
            for(int i = 0; i < printerNames.length(); i++) {
                clientPrinterConnections.add(printerNames.getString(i), connection);
            }
        }
        catch(Exception e) {
            log.warn("invalid printer names format for subscription");
        }
        if (isWindows()) {
            return launchNotificationThreads();
        } else {
            if (!CupsStatusServer.isRunning()) CupsStatusServer.runServer();
            return true;
        }
    }

    public synchronized static void closeListener(SocketConnection connection) {
        for (Map.Entry<String, List<SocketConnection>> e: clientPrinterConnections.entrySet()) {
            if (e.getValue().contains(connection)) {
                clientPrinterConnections.removeValue(e.getKey(),connection);
            }
        }
    }

    public synchronized static void stopListening() {
        printersListening.clear();
        if (isWindows()) {
            closeNotificationThreads();
        } else {
            CupsStatusServer.stopServer();
        }
    }

    public synchronized static boolean isListeningTo (String PrinterName){
        return clientPrinterConnections.containsKey(PrinterName) || clientPrinterConnections.containsKey("null");
    }

    public synchronized static void statusChanged (PrinterStatus[] statuses) {
        HashSet<SocketConnection> connections = new HashSet<>();
        for (PrinterStatus ps : statuses) {
            if (clientPrinterConnections.containsKey(ps.issuingPrinterName)) {
                connections.addAll(clientPrinterConnections.get(ps.issuingPrinterName));
            }
            if (clientPrinterConnections.containsKey("null")) {
                connections.addAll(clientPrinterConnections.get("null"));
            }
            for(SocketConnection sc : connections) {
                sc.getStatusListener().statusChanged(ps);
            }
        }
    }
}
