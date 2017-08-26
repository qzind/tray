package qz.printer.status;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Created by kyle on 3/14/17.
 */
public interface Cups extends Library {
    Cups INSTANCE = (Cups) Native.loadLibrary("cups", Cups.class);

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
