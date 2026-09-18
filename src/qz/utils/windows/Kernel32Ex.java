package qz.utils.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32Ex extends StdCallLibrary {
    Kernel32Ex INSTANCE = Native.load("kernel32", Kernel32Ex.class, W32APIOptions.UNICODE_OPTIONS);

    int GetSystemDefaultLocaleName(char[] lpLocaleName, int cchLocaleName);
    int GetLocaleInfoEx(String lpLocaleName, int LCType, Pointer lpLCData, int cchData);
}