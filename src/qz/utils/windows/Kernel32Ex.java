package qz.utils.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32Ex extends Kernel32 {
    Kernel32Ex INSTANCE = Native.load("kernel32", Kernel32Ex.class, W32APIOptions.UNICODE_OPTIONS);

    int GetSystemDefaultLocaleName(char[] lpLocaleName, int cchLocaleName);
    int GetLocaleInfoEx(String lpLocaleName, int LCType, Pointer lpLCData, int cchData);
    boolean SetFileInformationByHandle(WinNT.HANDLE hFile, int fileInformationClass, Structure lpFileInformation, int dwBufferSize);
}