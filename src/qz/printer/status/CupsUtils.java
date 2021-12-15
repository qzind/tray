package qz.printer.status;

import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import org.eclipse.jetty.util.URIUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import qz.printer.status.Cups.IPP;

import java.net.URI;
import java.util.ArrayList;

/**
 * Created by kyle on 5/17/17.
 */
public class CupsUtils {
    private static final Logger log = LogManager.getLogger(CupsUtils.class);

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

    /**
     * Gets all statuses relating to our subscriptionId with a sequence number greater than eventNumber
     */
    public static Pointer getStatuses(int eventNumber) {
        Pointer request = cups.ippNewRequest(IPP.GET_NOTIFICATIONS);

        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_URI, "printer-uri", CHARSET,
                                   URIUtil.encodePath("ipp://localhost:" + IPP.PORT + "/"));
        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_NAME, "requesting-user-name", CHARSET, USER);
        cups.ippAddInteger(request, IPP.TAG_OPERATION, IPP.TAG_INTEGER, "notify-subscription-ids", subscriptionID);
        cups.ippAddInteger(request, IPP.TAG_OPERATION, IPP.TAG_INTEGER, "notify-sequence-numbers", eventNumber);

        return cups.cupsDoRequest(http, request, "/");
    }

    public static ArrayList<Status> getAllStatuses() {
        ArrayList<Status> statuses = new ArrayList<>();
        Pointer request = cups.ippNewRequest(IPP.GET_PRINTERS);

        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_NAME, "requesting-user-name", CHARSET, USER);
        Pointer response = cups.cupsDoRequest(http, request, "/");
        Pointer stateAttr = cups.ippFindAttribute(response, "printer-state", IPP.TAG_ENUM);
        Pointer reasonAttr = cups.ippFindAttribute(response, "printer-state-reasons", IPP.TAG_KEYWORD);
        Pointer nameAttr = cups.ippFindAttribute(response, "printer-name", IPP.TAG_NAME);

        while(stateAttr != Pointer.NULL) {

            //save reasons until we have name, we need to go through the attrs in order
            String[] reasons = new String[cups.ippGetCount(reasonAttr)];
            for(int i = 0; i < reasons.length; i++) {
                reasons[i] = cups.ippGetString(reasonAttr, i, "");
            }
            String state = Cups.INSTANCE.ippEnumString("printer-state", Cups.INSTANCE.ippGetInteger(stateAttr, 0));
            String printer = cups.ippGetString(nameAttr, 0, "");

            for(String reason : reasons) {
                statuses.add(NativeStatus.fromCupsPrinterStatus(reason, state, printer));
            }

            //for next loop iteration
            stateAttr = cups.ippFindNextAttribute(response, "printer-state", IPP.TAG_ENUM);
            reasonAttr = cups.ippFindNextAttribute(response, "printer-state-reasons", IPP.TAG_KEYWORD);
            nameAttr = cups.ippFindNextAttribute(response, "printer-name", IPP.TAG_NAME);
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

        String[] subscriptions = {"job-state-changed", "printer-state-changed"};
        Pointer request = cups.ippNewRequest(IPP.CREATE_JOB_SUBSCRIPTION);

        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_URI, "printer-uri", CHARSET,
                          URIUtil.encodePath("ipp://localhost:" + IPP.PORT + "/printers"));
        cups.ippAddString(request, IPP.TAG_OPERATION, IPP.TAG_NAME, "requesting-user-name", CHARSET, USER);
        cups.ippAddString(request, IPP.TAG_SUBSCRIPTION, IPP.TAG_URI, "notify-recipient-uri", CHARSET,
                          URIUtil.encodePath("rss://localhost:" + rssPort));
        cups.ippAddStrings(request, IPP.TAG_SUBSCRIPTION, IPP.TAG_KEYWORD, "notify-events", subscriptions.length, CHARSET,
                          new StringArray(subscriptions));
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
