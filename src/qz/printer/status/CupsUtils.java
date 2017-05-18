package qz.printer.status;

import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Created by kyle on 5/17/17.
 */
public class CupsUtils {
    private static final Logger log = LoggerFactory.getLogger(CupsUtils.class);

    private static Pointer http;
    private static int cupsPort;


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
                                   "ipp://localhost:631/printers/");
        Cups.INSTANCE.ippAddString(request,
                                   Cups.INSTANCE.ippTagValue("Operation"),
                                   Cups.INSTANCE.ippTagValue("Name"),
                                   "requesting-user-name",
                                   "",
                                   System.getProperty("user.name"));

        Pointer response = Cups.INSTANCE.cupsDoRequest(http, request, "/");
        //parseResponse(response);
        return response;
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
                int port = (new URI(data)).getPort();
                if (CupsStatusServer.CUPS_RSS_PORTS.contains(port)) {
                    Pointer idAttr = Cups.INSTANCE.ippFindNextAttribute(response, "notify-subscription-id",
                                                                        Cups.INSTANCE.ippTagValue("Integer"));

                    int id = Cups.INSTANCE.ippGetInteger(idAttr, 0);
                    endSubscription(id);
                    //Todo Remove this debugging log
                    log.warn("ending {}", id);
                }
            } catch(Exception e) {
                log.warn("Error getting subscription data");
            }
            attr = Cups.INSTANCE.ippFindNextAttribute(response, "notify-recipient-uri",
                                                      Cups.INSTANCE.ippTagValue("uri"));
        }
        Cups.INSTANCE.ippDelete(response);
        return false;
    }

    static void startSubscription(int rssPort) {
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
        parseResponse(response);
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

    static void parseResponse(Pointer response) {
        Pointer attr = Cups.INSTANCE.ippFirstAttribute(response);
        while (true) {
            if (attr == Pointer.NULL) {
                break;
            }
            int valueTag = Cups.INSTANCE.ippGetValueTag(attr);
            String data = Cups.INSTANCE.ippTagString(valueTag);
            String attrName = Cups.INSTANCE.ippGetName(attr);
            if (valueTag == Cups.INSTANCE.ippTagValue("Integer")) {
                data = "" + Cups.INSTANCE.ippGetInteger(attr, 0);
            } else {
                data = Cups.INSTANCE.ippGetString(attr, 0, "");
            }
            if (attrName == null){
                System.out.println("------------------------");
            } else {
                System.out.printf("%s: %s\n", attrName, data);
            }
            attr = Cups.INSTANCE.ippNextAttribute(response);
        }
        System.out.println("------------------------");
    }

    public static void freeIppObjs() {
        Cups.INSTANCE.httpClose(http);
    }
}
