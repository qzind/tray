package qz.printer.status;

import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.platform.win32.WinspoolUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.util.MultiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.printer.PrintServiceMatcher;
import qz.printer.info.NativePrinterMap;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;
import qz.ws.SocketConnection;

import java.util.*;

import static qz.utils.SystemUtilities.isWindows;

/**
 * Created by Kyle on 2/23/2017.
 */
public class StatusMonitor {
    private static final Logger log = LogManager.getLogger(StatusMonitor.class);

    public static final String ALL_PRINTERS = "";

    private static Thread printerConnectionsThread;
    private static final HashMap<String,Thread> notificationThreadCollection = new HashMap<>();
    private static final MultiMap<SocketConnection> clientPrinterConnections = new MultiMap<>();

    public synchronized static boolean launchNotificationThreads() {
        ArrayList<String> printerNameList = new ArrayList<>();

        Winspool.PRINTER_INFO_2[] printers = WinspoolUtil.getPrinterInfo2();
        for (Winspool.PRINTER_INFO_2 printer : printers) {
            printerNameList.add(printer.pPrinterName);
            if (!notificationThreadCollection.containsKey(printer.pPrinterName)) {
                Thread notificationThread = new WmiPrinterStatusThread(printer);
                notificationThreadCollection.put(printer.pPrinterName, notificationThread);
                notificationThread.start();
            }
        }
        //interrupt threads that don't have associated printers
        for (Map.Entry<String,Thread> e : notificationThreadCollection.entrySet()) {
            if (!printerNameList.contains(e.getKey())) {
                e.getValue().interrupt();
                notificationThreadCollection.remove(e.getKey());
            }
        }

        if (printerConnectionsThread == null) {
            printerConnectionsThread = new WmiPrinterConnectionsThread();
            printerConnectionsThread.start();
        }

        return true;
    }

    public synchronized static void relaunchThreads() {
        launchNotificationThreads();
    }

    public synchronized static void closeNotificationThreads() {
        for (Thread t : notificationThreadCollection.values()) {
            t.interrupt();
        }
        notificationThreadCollection.clear();

        if (printerConnectionsThread != null) {
            printerConnectionsThread.interrupt();
            printerConnectionsThread = null;
        }
    }

    public synchronized static boolean startListening(SocketConnection connection, JSONObject params) throws JSONException {
        JSONArray printerNames = params.getJSONArray("printerNames");
        boolean jobData = params.optBoolean("jobData", false);
        int maxJobData = params.optInt("maxJobData", -1);
        PrintingUtilities.Flavor dataFlavor = PrintingUtilities.Flavor.parse(params, PrintingUtilities.Flavor.PLAIN);

        if (printerNames.isNull(0)) {  //listen to all printers
            if (jobData) {
                connection.getStatusListener().enableJobDataOnPrinter(ALL_PRINTERS, maxJobData, dataFlavor);
            }
            if (!clientPrinterConnections.containsKey(ALL_PRINTERS)) {
                clientPrinterConnections.add(ALL_PRINTERS, connection);
            } else if (!clientPrinterConnections.getValues(ALL_PRINTERS).contains(connection)) {
                clientPrinterConnections.add(ALL_PRINTERS, connection);
            }
        } else {  // listen to specific printer(s)
            for (int i = 0; i < printerNames.length(); i++) {
                String printerName = printerNames.getString(i);
                if (SystemUtilities.isMac()) {
                    // Since 2.0: Mac printers use descriptions as printer names; Find CUPS ID by Description
                    printerName = NativePrinterMap.getInstance().lookupPrinterId(printerName);
                    // Handle edge-case where printer was recently renamed/added
                    if (printerName == null) {
                        // Call PrintServiceLookup.lookupPrintServices again
                        PrintServiceMatcher.getNativePrinterList(true);
                        printerName = NativePrinterMap.getInstance().lookupPrinterId(printerNames.getString(i));
                    }
                }
                if (printerName == null || printerName.equals("")) {
                    throw new IllegalArgumentException();
                }
                if(jobData) {
                    connection.getStatusListener().enableJobDataOnPrinter(printerName, maxJobData, dataFlavor);
                }
                if (!clientPrinterConnections.containsKey(printerName)) {
                    clientPrinterConnections.add(printerName, connection);
                } else if (!clientPrinterConnections.getValues(printerName).contains(connection)) {
                    clientPrinterConnections.add(printerName, connection);
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
        ArrayList<Status> statuses = isWindows() ? WmiPrinterStatusThread.getAllStatuses(): CupsUtils.getAllStatuses();

        List<SocketConnection> connections = clientPrinterConnections.get(ALL_PRINTERS);
        if (connections != null) {
            sendForAllPrinters = connections.contains(connection);
        }

        for (Status status : statuses) {
            if (sendForAllPrinters) {
                connection.getStatusListener().statusChanged(status);
            } else {
                connections = clientPrinterConnections.get(status.getPrinter());
                if ((connections != null) && connections.contains(connection)) {
                    connection.getStatusListener().statusChanged(status);
                }
            }
        }
    }

    public synchronized static void closeListener(SocketConnection connection) {
        for (Iterator<Map.Entry<String, List<SocketConnection>>> i = clientPrinterConnections.entrySet().iterator(); i.hasNext();) {
            if (i.next().getValue().contains(connection)) {
                i.remove();
            }
        }
        if (clientPrinterConnections.isEmpty()) {
            if (isWindows()) {
                closeNotificationThreads();
            } else {
                CupsStatusServer.stopServer();
            }
        }
    }

    public synchronized static void statusChanged(Status[] statuses) {
        HashSet<SocketConnection> connections = new HashSet<>();
        for (Status status : statuses) {
            if (clientPrinterConnections.containsKey(status.getPrinter())) {
                connections.addAll(clientPrinterConnections.get(status.getPrinter()));
            }
            if (clientPrinterConnections.containsKey(ALL_PRINTERS)) {
                connections.addAll(clientPrinterConnections.get(ALL_PRINTERS));
            }
            for (SocketConnection connection : connections) {
                connection.getStatusListener().statusChanged(status);
            }
        }
    }
}
