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

        // Read raw file bytes directly to preserve exactly what is on disk
        byte[] rawBytes;
        try {
            rawBytes = Files.readAllBytes(sourceFile.toPath());
        } catch (IOException e) {
            log.error("Failed to read raw file bytes: {}", e.getMessage());
            return false;
        }

        HANDLE[] phPrinter = new HANDLE[1];

        // Request immediate unbatched data routing to bypass kernel filter hooks
        Winspool2W.PRINTER_OPTIONS printerOptions = new Winspool2W.PRINTER_OPTIONS();
        printerOptions.dwFlags = Winspool2W.PRINTER_OPTION_NO_BATCHING;

        // Strip the spooler's rendering layers using the advanced open call
        boolean opened = Winspool2W.INSTANCE.OpenPrinter2W(nativePrinter.getName(), phPrinter, null,  printerOptions);
        if (!opened || phPrinter[0] == null) {
            log.error("Unable to establish a direct connection to: {}", nativePrinter);
            return false;
        }

        HANDLE hPrinter = phPrinter[0];
        boolean transmissionSuccess = false;

        try {
            // Reuse standard JNA Platform structures
            Winspool2W.DOC_INFO_1 docInfo = new Winspool2W.DOC_INFO_1();
            docInfo.pDocName = options.getRawOptions().getJobName(Constants.RAW_PRINT);
            docInfo.pOutputFile = null;
            docInfo.pDatatype = "RAW"; // Asserts exact data layout preservation
            docInfo.write();

            // Native methods inherited directly from standard Winspool
            if (Winspool2W.INSTANCE.StartDocPrinterW(hPrinter, 1, docInfo)) {
                if (Winspool2W.INSTANCE.StartPagePrinter(hPrinter)) {

                    IntByReference bytesWritten = new IntByReference(0);

                    // Execute direct channel push of the raw byte array
                    boolean writeResult = Winspool2W.INSTANCE.WritePrinter(
                            hPrinter, rawBytes, rawBytes.length, bytesWritten
                    );

                    if (writeResult && bytesWritten.getValue() == rawBytes.length) {
                        transmissionSuccess = true;
                    } else {
                        log.error("Spooler pipeline rejected or truncated raw transmission.");
                    }

                    Winspool2W.INSTANCE.EndPagePrinter(hPrinter);
                }
                Winspool2W.INSTANCE.EndDocPrinter(hPrinter);
            }
        } finally {
            Winspool2W.INSTANCE.ClosePrinter(hPrinter);
        }

        return transmissionSuccess;
    }
}