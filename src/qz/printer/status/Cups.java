package qz.printer.status;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Created by kyle on 3/14/17.
 */
public interface Cups extends Library {

    Cups INSTANCE = Native.loadLibrary("cups", Cups.class);

    /**
     * Static class to facilitate readability of values
     */
    class IPP {
        public static int PORT = INSTANCE.ippPort();
        public static int TAG_OPERATION = INSTANCE.ippTagValue("Operation");
        public static int TAG_URI = INSTANCE.ippTagValue("uri");
        public static int TAG_NAME = INSTANCE.ippTagValue("Name");
        public static int TAG_INTEGER = INSTANCE.ippTagValue("Integer");
        public static int TAG_KEYWORD = INSTANCE.ippTagValue("keyword");
        public static int TAG_SUBSCRIPTION = INSTANCE.ippTagValue("Subscription");
        public static int GET_PRINTERS = INSTANCE.ippOpValue("CUPS-Get-Printers");
        public static int GET_PRINTER_ATTRIBUTES = INSTANCE.ippOpValue("Get-Printer-Attributes");
        public static int GET_SUBSCRIPTIONS = INSTANCE.ippOpValue("Get-Subscriptions");
        public static int CREATE_PRINTER_SUBSCRIPTION = INSTANCE.ippOpValue("Create-Printer-Subscription");
        public static int CANCEL_SUBSCRIPTION = INSTANCE.ippOpValue("Cancel-Subscription");
        public static final int INT_ERROR = 0;
        public static final int INT_UNDEFINED = -1;
    }

    //See https://www.cups.org/doc/api-cups.html and https://www.cups.org/doc/api-httpipp.html for usage

    Pointer cupsEncryption();
    Pointer httpConnectEncrypt(String host, int port, Pointer encryption);
    Pointer cupsDoRequest(Pointer http, Pointer request, String resource);
    Pointer ippNewRequest(int op);
    Pointer ippGetString(Pointer attr, int element, Pointer dataLen);
    Pointer ippFirstAttribute(Pointer ipp);
    Pointer ippNextAttribute(Pointer ipp);
    Pointer ippFindAttribute(Pointer ipp, String name, int type);
    Pointer ippFindNextAttribute(Pointer ipp, String name, int type);

    String cupsServer();
    String ippTagString(int tag);
    String ippGetName(Pointer attr);
    String ippGetString(Pointer attr, int element, String language);
    String ippEnumString (String attrname, int enumvalue);

    int ippPort();
    int httpAssembleURI(int encoding, Memory uri, int urilen, String sceme, String username, String host, int port, String resourcef);
    int ippTagValue(String name);
    int ippEnumValue(String attrname, String enumstring);
    int ippOpValue(String name);
    int ippAddString(Pointer ipp, int group, int tag, String name, String charset, String value);
    int ippAddInteger (Pointer ipp, int group, int tag, String name, int value);
    int ippGetCount(Pointer attr);
    int ippGetValueTag(Pointer ipp);
    int ippGetInteger(Pointer attr, int element);

    void ippDelete(Pointer ipp);
    void httpClose(Pointer http);
}
