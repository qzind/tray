package qz.printer.status;

import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.util.URLUtil;

import java.net.URI;
import java.util.ArrayList;

/**
 * Created by kyle on 5/17/17.
 */
public class CupsUtils {
    private static final Logger log = LoggerFactory.getLogger(CupsUtils.class);

    private static boolean httpInitialised = false;
    private static Pointer http;
    private static int cupsPort;
    private static int subscriptionID = -1;


    synchronized static void initCupsStuff() {
        if (!httpInitialised) {
            httpInitialised = true;
            cupsPort = Cups.INSTANCE.ippPort();
            http = Cups.INSTANCE.httpConnectEncrypt(
                    Cups.INSTANCE.cupsServer(),
                    Cups.INSTANCE.ippPort(),
                    Cups.INSTANCE.cupsEncryption());
        }
    }

    static Pointer listSubscriptions() {
        Pointer request = Cups.INSTANCE.ippNewRequest(Cups.INSTANCE.ippOpValue("Get-Subscriptions"));

        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("uri"),
                                   "printer-uri",
                                   "",
                                   "ipp://localhost:" + cupsPort + "/printers/");
        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("Name"),
                                   "requesting-user-name",
                                   "",
                                   System.getProperty("user.name"));

        Pointer response = Cups.INSTANCE.cupsDoRequest(http, request, "/");
        return response;
    }

    public static PrinterStatus[] getStatuses(String printerName) {
        Pointer request = Cups.INSTANCE.ippNewRequest(Cups.INSTANCE.ippOpValue("Get-Printer-Attributes"));

        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("uri"),
                                   "printer-uri",
                                   "",
                                   //todo does this need to be sanitized?
                                   "ipp://localhost:" + cupsPort + "/printers/" + printerName);
        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("Name"),
                                   "requesting-user-name",
                                   "",
                                   System.getProperty("user.name"));

        Pointer response = Cups.INSTANCE.cupsDoRequest(http, request, "/");
        Pointer attr = Cups.INSTANCE.ippFindAttribute(response, "printer-state-reasons",
                                                          Cups.INSTANCE.ippTagValue("keyword"));
        ArrayList<PrinterStatus> statuses = new ArrayList<>();

        if (attr != Pointer.NULL) {
            int attrCount = Cups.INSTANCE.ippGetCount(attr);
            for(int i = 0; i < attrCount; i++) {
                String data = Cups.INSTANCE.ippGetString(attr, i, "");
                PrinterStatus s = PrinterStatus.getFromCupsString(data, printerName);
                if (s != null) statuses.add(s);
            }
        } else {
            statuses.add(new PrinterStatus(PrinterStatusType.NOT_AVAILABLE, printerName, ""));
        }

        Cups.INSTANCE.ippDelete(response);

        return statuses.toArray(new PrinterStatus[statuses.size()]);
    }

    public static ArrayList<PrinterStatus> getAllStatuses() {
        ArrayList<PrinterStatus> statuses = new ArrayList<>();
        Pointer request = Cups.INSTANCE.ippNewRequest(Cups.INSTANCE.ippOpValue("CUPS-Get-Printers"));

        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("Name"),
                                   "requesting-user-name",
                                   "",
                                   System.getProperty("user.name"));
        Pointer response = Cups.INSTANCE.cupsDoRequest(http, request, "/");
        Pointer attr = Cups.INSTANCE.ippFindAttribute(response, "printer-state-reasons",
                                                      Cups.INSTANCE.ippTagValue("keyword"));

        while (attr != Pointer.NULL) {
            //save reasons until we have name, we need to go through the attrs in order
            String[] reasons = new String[Cups.INSTANCE.ippGetCount(attr)];
            for(int i = 0; i < reasons.length; i++) {
                reasons[i] = Cups.INSTANCE.ippGetString(attr, i, "");
            }

            attr = Cups.INSTANCE.ippFindNextAttribute(response, "printer-name",
                                                      Cups.INSTANCE.ippTagValue("Name"));
            String name = Cups.INSTANCE.ippGetString(attr, 0, "");

            for(String reason : reasons) {
                statuses.add(PrinterStatus.getFromCupsString(reason, name));
            }

            //for next loop iteration
            attr = Cups.INSTANCE.ippFindNextAttribute(response, "printer-state-reasons",
                                                      Cups.INSTANCE.ippTagValue("keyword"));
        }
        Cups.INSTANCE.ippDelete(response);
        return statuses;
    }

    public static boolean clearSubscriptions() {
        Pointer response = listSubscriptions();
        Pointer attr = Cups.INSTANCE.ippFindAttribute(response, "notify-recipient-uri",
                                                      Cups.INSTANCE.ippTagValue("uri"));
        while(true) {
            if (attr == Pointer.NULL) {
                break;
            }
            try {
                String data = Cups.INSTANCE.ippGetString(attr, 0, "");

                int port = new URI(data).getPort();
                if (CupsStatusServer.CUPS_RSS_PORTS.contains(port)) {
                    Pointer idAttr = Cups.INSTANCE.ippFindNextAttribute(response, "notify-subscription-id",
                                                                        Cups.INSTANCE.ippTagValue("Integer"));

                    int id = Cups.INSTANCE.ippGetInteger(idAttr, 0);
                    log.warn("Ending CUPS subscription #{}", id);
                    endSubscription(id);
                }
            } catch(Exception ignore) { }
            attr = Cups.INSTANCE.ippFindNextAttribute(response, "notify-recipient-uri",
                                                      Cups.INSTANCE.ippTagValue("uri"));
        }
        Cups.INSTANCE.ippDelete(response);
        return false;
    }

    static void startSubscription(int rssPort) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> endSubscription(subscriptionID)));

        Pointer request = Cups.INSTANCE.ippNewRequest(Cups.INSTANCE.ippOpValue("Create-Printer-Subscription"));

        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("uri"),
                                   "printer-uri",
                                   "",
                                   "ipp://localhost:" + cupsPort + "/printers");
        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("Name"),
                                   "requesting-user-name",
                                   "",
                                   System.getProperty("user.name"));
        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Subscription"),
                                   Cups.INSTANCE.ippTagValue("uri"),
                                   "notify-recipient-uri",
                                   "",
                                   "rss://localhost:" + rssPort);
        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Subscription"),
                                   Cups.INSTANCE.ippTagValue("Keyword"),
                                   "notify-events",
                                   "",
                                   "printer-state-changed");
        Cups.INSTANCE.ippAddInteger(request,
                                    Cups.INSTANCE.ippTagValue("Subscription"),
                                    Cups.INSTANCE.ippTagValue("Integer"),
                                    "notify-lease-duration",
                                    0);
        Pointer response = Cups.INSTANCE.cupsDoRequest(http, request, "/");

        Pointer attr = Cups.INSTANCE.ippFindAttribute(response, "notify-subscription-id",
                                                      Cups.INSTANCE.ippTagValue("integer"));
        if (attr != Pointer.NULL) subscriptionID = Cups.INSTANCE.ippGetInteger(attr, 0);

        Cups.INSTANCE.ippDelete(response);
    }

    static void endSubscription(int id) {
        Pointer request = Cups.INSTANCE.ippNewRequest(Cups.INSTANCE.ippOpValue("Cancel-Subscription"));

        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("uri"),
                                   "printer-uri",
                                   "",
                                   "ipp://localhost:" + cupsPort);
        Cups.INSTANCE.ippAddInteger(request,
                                    Cups.INSTANCE.ippTagValue("Operation"),
                                    Cups.INSTANCE.ippTagValue("Integer"),
                                    "notify-subscription-id",
                                    id);
        Pointer response = Cups.INSTANCE.cupsDoRequest(http, request, "/");
        Cups.INSTANCE.ippDelete(response);
    }

    public synchronized static void freeIppObjs() {
        if (httpInitialised) {
            httpInitialised = false;
            Cups.INSTANCE.httpClose(http);
        }
    }
}
