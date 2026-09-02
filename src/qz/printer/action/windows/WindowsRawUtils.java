package qz.printer.action.windows;

import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.info.NativePrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class WindowsRawUtils {
    private static final Logger log = LogManager.getLogger(WindowsRawUtils.class);

    public static boolean sendRawFile(NativePrinter nativePrinter, File sourceFile, PrintOptions options) {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            log.error("Invalid source file specified.");
            return false;
        }

        byte[] rawBytes;
        try {
            rawBytes = Files.readAllBytes(sourceFile.toPath());
        } catch (IOException e) {
            log.error("Failed to read raw file bytes: {}", e.getMessage());
            return false;
        }

        HANDLE[] phPrinter = new HANDLE[1];
        Winspool2.PRINTER_OPTIONS printerOptions = new Winspool2.PRINTER_OPTIONS();
        printerOptions.dwFlags = Winspool2.PRINTER_OPTION_NO_BATCHING;
        boolean opened = Winspool2.INSTANCE.OpenPrinter2W(nativePrinter.getName(), phPrinter, null, printerOptions);
        HANDLE hPrinter = phPrinter[0];

        if (!opened || hPrinter == null) {
            log.error("Unable to establish a direct connection to: {}", nativePrinter);
            return false;
        }

        boolean success = false;
        try {
            Winspool2.DOC_INFO_1 docInfo = new Winspool2.DOC_INFO_1();
            docInfo.pDocName = options.getRawOptions().getJobName(Constants.RAW_PRINT);
            docInfo.pOutputFile = null;
            docInfo.pDatatype = "RAW";
            docInfo.write();

            if (Winspool2.INSTANCE.StartDocPrinterW(hPrinter, 1, docInfo)) {
                if (Winspool2.INSTANCE.StartPagePrinter(hPrinter)) {
                    IntByReference bytesWritten = new IntByReference(0);
                    boolean writeResult = Winspool2.INSTANCE.WritePrinter(hPrinter, rawBytes, rawBytes.length, bytesWritten);
                    if (writeResult && bytesWritten.getValue() == rawBytes.length) {
                        success = true;
                    } else {
                        log.error("Spooler rejected or truncated raw data.");
                    }
                    Winspool2.INSTANCE.EndPagePrinter(hPrinter);
                }
                Winspool2.INSTANCE.EndDocPrinter(hPrinter);
            }
        } finally {
            Winspool2.INSTANCE.ClosePrinter(hPrinter);
        }
        return success;
    }
}