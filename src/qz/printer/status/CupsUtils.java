package qz.printer.status;

import com.sun.jna.Pointer;
import org.codehaus.jettison.json.JSONArray;
import org.eclipse.jetty.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.status.Cups.IPP;
import qz.utils.PrintingUtilities;
import qz.utils.ShellUtilities;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kyle on 5/17/17.
 */
public class CupsUtils {
    private static final Logger log = LoggerFactory.getLogger(CupsUtils.class);

    public static String USER = System.getProperty("user.name");
    public static String CHARSET = "";

    private static Cups cups = Cups.INSTANCE;

    private static boolean httpInitialised = false;
    private static Pointer http;
    private static int subscriptionID = IPP.INT_UNDEFINED;


    synchronized static void initCupsHttp() {
        if (!httpInitialised) {
            httpInitialised = true;
            http = cups.httpConnectEncrypt(cups.cupsServer(), IPP.PORT, cups.cupsEncryption());
        }
    }

    static Pointer listSubscriptions() {
        Pointer request = cups.ippNewRequest(IPP.GET_SUBSCRIPTIONS);

        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_URI, "printer-uri", CHARSET,
                                   URIUtil.encodePath("ipp://localhost:" + IPP.PORT + "/printers/"));
        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_NAME, "requesting-user-name", CHARSET, USER);

        return cups.cupsDoRequest(http, request, "/");
    }

    public static PrinterStatus[] getStatuses(String printerName) {
        Pointer request = cups.ippNewRequest(IPP.GET_PRINTER_ATTRIBUTES);

        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_URI, "printer-uri", CHARSET,
                                   URIUtil.encodePath("ipp://localhost:" + IPP.PORT + "/printers/" + printerName));
        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_NAME, "requesting-user-name", CHARSET, USER);

        Pointer response = cups.cupsDoRequest(http, request, "/");
        Pointer attr = cups.ippFindAttribute(response, "printer-state-reasons", IPP.TAG_KEYWORD);
        ArrayList<PrinterStatus> statuses = new ArrayList<>();

        if (attr != Pointer.NULL) {
            int attrCount = cups.ippGetCount(attr);
            for(int i = 0; i < attrCount; i++) {
                String data = cups.ippGetString(attr, i, "");
                PrinterStatus status = PrinterStatus.getFromCupsString(data, printerName);
                if (status != null) { statuses.add(status); }
            }
        } else {
            statuses.add(new PrinterStatus(PrinterStatusType.NOT_AVAILABLE, printerName, ""));
        }

        cups.ippDelete(response);

        return statuses.toArray(new PrinterStatus[statuses.size()]);
    }

    public static ArrayList<PrinterStatus> getAllStatuses() {
        ArrayList<PrinterStatus> statuses = new ArrayList<>();
        Pointer request = cups.ippNewRequest(IPP.GET_PRINTERS);

        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_NAME, "requesting-user-name", CHARSET, USER);
        Pointer response = cups.cupsDoRequest(http, request, "/");
        Pointer attr = cups.ippFindAttribute(response, "printer-state-reasons", IPP.TAG_KEYWORD);

        while(attr != Pointer.NULL) {
            //save reasons until we have name, we need to go through the attrs in order
            String[] reasons = new String[cups.ippGetCount(attr)];
            for(int i = 0; i < reasons.length; i++) {
                reasons[i] = cups.ippGetString(attr, i, "");
            }

            attr = cups.ippFindNextAttribute(response, "printer-name", IPP.TAG_NAME);
            String name = cups.ippGetString(attr, 0, "");

            for(String reason : reasons) {
                statuses.add(PrinterStatus.getFromCupsString(reason, name));
            }

            //for next loop iteration
            attr = cups.ippFindNextAttribute(response, "printer-state-reasons", IPP.TAG_KEYWORD);
        }

        cups.ippDelete(response);
        return statuses;
    }

    public static boolean clearSubscriptions() {
        Pointer response = listSubscriptions();
        Pointer attr = cups.ippFindAttribute(response, "notify-recipient-uri", IPP.TAG_URI);

        while(true) {
            if (attr == Pointer.NULL) {
                break;
            }
            try {
                String data = cups.ippGetString(attr, 0, "");

                int port = new URI(data).getPort();
                if (CupsStatusServer.CUPS_RSS_PORTS.contains(port)) {
                    Pointer idAttr = cups.ippFindNextAttribute(response, "notify-subscription-id", IPP.TAG_INTEGER);

                    int id = cups.ippGetInteger(idAttr, 0);
                    log.warn("Ending CUPS subscription #{}", id);
                    endSubscription(id);
                }
            }
            catch(Exception ignore) { }

            attr = cups.ippFindNextAttribute(response, "notify-recipient-uri", IPP.TAG_URI);
        }

        cups.ippDelete(response);
        return false;
    }

    static void startSubscription(int rssPort) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> freeIppObjs()));

        Pointer request = cups.ippNewRequest(IPP.CREATE_PRINTER_SUBSCRIPTION);

        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_URI, "printer-uri", CHARSET,
                                   URIUtil.encodePath("ipp://localhost:" + IPP.PORT + "/printers"));
        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_NAME, "requesting-user-name", CHARSET, USER);
        cups.ippAddString(request, IPP.TAG_SUBSCRIPTION, IPP.TAG_URI, "notify-recipient-uri", CHARSET,
                                   URIUtil.encodePath("rss://localhost:" + rssPort));
        cups.ippAddString(request, IPP.TAG_SUBSCRIPTION, IPP.TAG_KEYWORD, "notify-events", CHARSET, "printer-state-changed");
        cups.ippAddInteger(request, IPP.TAG_SUBSCRIPTION, IPP.TAG_INTEGER, "notify-lease-duration", 0);

        Pointer response = cups.cupsDoRequest(http, request, "/");

        Pointer attr = cups.ippFindAttribute(response, "notify-subscription-id", IPP.TAG_INTEGER);
        if (attr != Pointer.NULL) { subscriptionID = cups.ippGetInteger(attr, 0); }

        cups.ippDelete(response);
    }

    static void endSubscription(int id) {
        switch (id) {
            case IPP.INT_ERROR:
            case IPP.INT_UNDEFINED:
                return; // no subscription to end
        }
        Pointer request = cups.ippNewRequest(IPP.CANCEL_SUBSCRIPTION);

        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_URI, "printer-uri", CHARSET,
                                   URIUtil.encodePath("ipp://localhost:" + IPP.PORT));
        cups.ippAddInteger(request, IPP.TAG_OPERATION, IPP.TAG_INTEGER, "notify-subscription-id", id);

        Pointer response = cups.cupsDoRequest(http, request, "/");
        cups.ippDelete(response);
    }

    public synchronized static void freeIppObjs() {
        if (httpInitialised) {
            httpInitialised = false;
            endSubscription(subscriptionID);
            subscriptionID = IPP.INT_UNDEFINED;
            cups.httpClose(http);
        }
    }
}
