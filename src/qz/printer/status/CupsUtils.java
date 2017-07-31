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

    private static Pointer http;
    private static int cupsPort;
    private static int subid = -1;


    static void initCupsStuff() {
        cupsPort = Cups.INSTANCE.ippPort();
        http = Cups.INSTANCE.httpConnectEncrypt(
                Cups.INSTANCE.cupsServer(),
                Cups.INSTANCE.ippPort(),
                Cups.INSTANCE.cupsEncryption());
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
        //todo remove this
        //parseResponse(response);
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
                    endSubscription(id);
                    //Todo Remove this debugging log
                    log.warn("Ending subscription #{}", id);
                }
            } catch(Exception ignore) { }
            attr = Cups.INSTANCE.ippFindNextAttribute(response, "notify-recipient-uri",
                                                      Cups.INSTANCE.ippTagValue("uri"));
        }
        Cups.INSTANCE.ippDelete(response);
        return false;
    }

    static void startSubscription(int rssPort) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> endSubscription(subid)));

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
        if (attr != Pointer.NULL) subid = Cups.INSTANCE.ippGetInteger(attr, 0);

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

    public static void freeIppObjs() {
        Cups.INSTANCE.httpClose(http);
    }


    //TODO Remove these functions
    static void parseResponse(Pointer response) {
        Pointer attr = Cups.INSTANCE.ippFirstAttribute(response);
        while (true) {
            if (attr == Pointer.NULL) {
                break;
            }
            System.out.println(parseAttr(attr));
            attr = Cups.INSTANCE.ippNextAttribute(response);
        }
        System.out.println("------------------------");
    }
    static String parseAttr(Pointer attr){
        int valueTag = Cups.INSTANCE.ippGetValueTag(attr);
        int attrCount = Cups.INSTANCE.ippGetCount(attr);
        String data = "";
        String attrName = Cups.INSTANCE.ippGetName(attr);
        for (int i = 0; i < attrCount; i++) {
            if (valueTag == Cups.INSTANCE.ippTagValue("Integer")) {
                data += Cups.INSTANCE.ippGetInteger(attr, i);
            } else if (valueTag == Cups.INSTANCE.ippTagValue("Boolean")) {
                data += (Cups.INSTANCE.ippGetInteger(attr, i) == 1);
            } else if (valueTag == Cups.INSTANCE.ippTagValue("Enum")) {
                data += Cups.INSTANCE.ippEnumString(attrName, Cups.INSTANCE.ippGetInteger(attr, i));
            } else {
                data += Cups.INSTANCE.ippGetString(attr, i, "");
            }
            if (i + 1 < attrCount) {
                data += ", ";
            }
        }

        if (attrName == null){
            return "------------------------";
        }
        return String.format("%s: %d %s {%s}", attrName, attrCount, Cups.INSTANCE.ippTagString(valueTag), data);
    }
}
