package qz.printer.action.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.Winspool;
import com.sun.jna.ptr.IntByReference;

/**
 * Winspool clone that exposes the OpenPrinter2 API
 */
@SuppressWarnings("SpellCheckingInspection")
public interface Winspool2 extends Winspool {
    Winspool2 INSTANCE = Native.load("winspool.drv", Winspool2.class);

    int PRINTER_OPTION_NO_BATCHING = 0x00000001;

    @Structure.FieldOrder({"cbSize", "dwFlags"})
    class PRINTER_OPTIONS extends Structure {
        @SuppressWarnings("unused")
        public int cbSize = this.size();
        public int dwFlags;
    }

    @Structure.FieldOrder({"pDocName", "pOutputFile", "pDatatype"})
    class DOC_INFO_1 extends Structure {
        public String pDocName;
        public String pOutputFile;
        public String pDatatype;
    }

    // Direct DLL function mappings matching native Win32 headers exactly
    boolean OpenPrinter2W(String pPrinterName, HANDLE[] phPrinter, Pointer pDefault, PRINTER_OPTIONS pOptions);
    boolean StartDocPrinterW(HANDLE hPrinter, int Level, DOC_INFO_1 pDocInfo);
    boolean StartPagePrinter(HANDLE hPrinter);
    boolean WritePrinter(HANDLE hPrinter, byte[] pBuf, int cbBuf, IntByReference pcWritten);
    @SuppressWarnings("UnusedReturnValue")
    boolean EndPagePrinter(HANDLE hPrinter);
    @SuppressWarnings("UnusedReturnValue")
    boolean EndDocPrinter(HANDLE hPrinter);
    boolean ClosePrinter(HANDLE hPrinter);
}