package qz.printer.status;

import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.platform.win32.WinspoolUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jetty.util.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.info.NativePrinterMap;
import qz.utils.SystemUtilities;
import qz.ws.SocketConnection;

import java.util.*;

import static qz.utils.SystemUtilities.isWindows;

/**
 * Created by Kyle on 2/23/2017.
 */

public class StatusMonitor {
    private static final Logger log = LoggerFactory.getLogger(StatusMonitor.class);

    private static final String ALL_PRINTERS = "";

    private static Thread printerConnectionsThread;
    private static final HashMap<String,Thread> notificationThreadCollection = new HashMap<>();
    private static final MultiMap<SocketConnection> clientPrinterConnections = new MultiMap<>();

    public synchronized static boolean launchNotificationThreads() {
        ArrayList<String> printerNameList = new ArrayList<>();

        Winspool.PRINTER_INFO_2[] printers = WinspoolUtil.getPrinterInfo2();
        for(Winspool.PRINTER_INFO_2 printer : printers) {
            printerNameList.add(printer.pPrinterName);
            if (!notificationThreadCollection.containsKey(printer.pPrinterName)) {
                Thread notificationThread = new WMIPrinterStatusThread(printer.pPrinterName, printer.Status);
                notificationThreadCollection.put(printer.pPrinterName, notificationThread);
                notificationThread.start();
            }
        }
        //cull threads that don't have associated printers
        for(Map.Entry<String,Thread> e : notificationThreadCollection.entrySet()) {
            if (!printerNameList.contains(e.getKey())) {
                e.getValue().interrupt();
                notificationThreadCollection.remove(e.getKey());
            }
        }

        if (printerConnectionsThread == null) {
            printerConnectionsThread = new WMIPrinterConnectionsThread();
            printerConnectionsThread.start();
        }

        return true;
    }

    public synchronized static void relaunchThreads() {
        launchNotificationThreads();
    }

    public synchronized static void closeNotificationThreads() {
        for(Thread t : notificationThreadCollection.values()) {
            t.interrupt();
        }
        notificationThreadCollection.clear();

        if (printerConnectionsThread != null) {
            printerConnectionsThread.interrupt();
            printerConnectionsThread = null;
        }
    }

    public synchronized static boolean startListening(SocketConnection connection, JSONArray printerNames) throws JSONException {
        if (printerNames.isNull(0)) {  //listen to all printers
            if (!clientPrinterConnections.containsKey(ALL_PRINTERS)) {
                clientPrinterConnections.add(ALL_PRINTERS, connection);
            } else if (!clientPrinterConnections.getValues(ALL_PRINTERS).contains(connection)) {
                clientPrinterConnections.add(ALL_PRINTERS, connection);
            }
        } else {  //listen to specific printer(s)
            for(int i = 0; i < printerNames.length(); i++) {
                String printerName = printerNames.getString(i);
                if (SystemUtilities.isMac()) {
                    // Since 2.0: Mac printers use Description; Find by CUPS ID
                    printerName = NativePrinterMap.getInstance().getPrinterId(printerName);
                }
                if (printerName == null || "".equals(printerName)) {
                    throw new IllegalArgumentException();
                }

                if (!clientPrinterConnections.containsKey(printerNames.getString(i))) {
                    clientPrinterConnections.add(printerNames.getString(i), connection);
                } else if (!clientPrinterConnections.getValues(printerNames.getString(i)).contains(connection)) {
                    clientPrinterConnections.add(printerNames.getString(i), connection);
                }
            }
        }

        if (isWindows()) {
            return launchNotificationThreads();
        } else {
            if (!CupsStatusServer.isRunning()) { CupsStatusServer.runServer(); }
            return true;
        }
    }

    public synchronized static void sendStatuses(SocketConnection connection) {
        boolean sendForAllPrinters = false;
        ArrayList<PrinterStatus> printers;

        if (isWindows()) {
            printers = new ArrayList<>();
            Winspool.PRINTER_INFO_2[] wmiPrinters = WinspoolUtil.getPrinterInfo2();
            for(Winspool.PRINTER_INFO_2 p : wmiPrinters) {
                printers.addAll(Arrays.asList(PrinterStatus.getFromWMICode(p.Status, p.pPrinterName)));
            }
        } else {
            printers = CupsUtils.getAllStatuses();
        }

        List<SocketConnection> connections = clientPrinterConnections.get("");
        if (connections != null) {
            sendForAllPrinters = connections.contains(connection);
        }

        for(PrinterStatus ps : printers) {
            if (sendForAllPrinters) {
                connection.getStatusListener().statusChanged(ps);
            } else {
                connections = clientPrinterConnections.get(ps.issuingPrinterName);
                if ((connections != null) && connections.contains(connection)) {
                    connection.getStatusListener().statusChanged(ps);
                }
            }
        }
    }

    public synchronized static void closeListener(SocketConnection connection) {
        ArrayList<String> itemsToDelete = new ArrayList<>();
        for(Map.Entry<String,List<SocketConnection>> e : clientPrinterConnections.entrySet()) {
            if (e.getValue().contains(connection)) {
                itemsToDelete.add(e.getKey());
            }
        }
        //Don't move this into the earlier loop, it causes a ConcurrentModificationException
        for(String s : itemsToDelete) {
            clientPrinterConnections.removeValue(s, connection);
        }

        if (clientPrinterConnections.isEmpty()) {
            if (isWindows()) {
                closeNotificationThreads();
            } else {
                CupsStatusServer.stopServer();
            }
        }
    }

    public synchronized static boolean isListeningTo(String PrinterName) {
        return clientPrinterConnections.containsKey(PrinterName) || clientPrinterConnections.containsKey("");
    }

    public synchronized static void statusChanged(PrinterStatus[] statuses) {
        HashSet<SocketConnection> connections = new HashSet<>();
        for(PrinterStatus ps : statuses) {
            if (clientPrinterConnections.containsKey(ps.issuingPrinterName)) {
                connections.addAll(clientPrinterConnections.get(ps.issuingPrinterName));
            }
            if (clientPrinterConnections.containsKey("")) {
                connections.addAll(clientPrinterConnections.get(""));
            }
            for(SocketConnection sc : connections) {
                sc.getStatusListener().statusChanged(ps);
            }
        }
    }
}
