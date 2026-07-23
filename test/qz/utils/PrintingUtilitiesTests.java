package qz.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.io.IOException;

public class PrintingUtilitiesTests {

    @Test
    public void lprSignatureTests() {
        PrinterException ex = new PrinterException(
                // This is the error shape that was reported in issue #642
                // See https://github.com/qzind/tray/issues/642
                "Error: java.io.IOException: error=1 running: '/usr/bin/lpr' '-PPDF' " +
                        "'-J QZ Tray Pixel Print' '-h' '-o media=Letter' '/tmp/javaprint123' " +
                        "usage: lpr [-cdfghlmnpqrstv]"
        );

        Assert.assertTrue(PrintingUtilities.hasLprSignature(ex));
        Assert.assertTrue(PrintingUtilities.isLikelyCupsBsdMissing(ex, true));
    }

    @Test
    public void missingLprSignatureTests() {
        // This was reported in issue #1341
        // See https://github.com/qzind/tray/issues/1341
        PrinterException ex = new PrinterException("lpr: command not found");

        Assert.assertTrue(PrintingUtilities.hasLprSignature(ex));
        Assert.assertTrue(PrintingUtilities.isLikelyCupsBsdMissing(ex, false));
    }

    @Test
    public void nestedLprSignatureTests() {
        PrinterException ex = new PrinterException("Failed to print");
        // Missing lpr shape from Raspberry Pi thread
        // linked by issue #1341
        ex.initCause(new IOException("Cannot run program \"/usr/bin/lpr\": error=2, No such file or directory"));

        Assert.assertTrue(PrintingUtilities.hasLprSignature(ex));
        Assert.assertTrue(PrintingUtilities.isLikelyCupsBsdMissing(ex, true));
    }

    @Test
    public void printerIoWrapperTests() {
        // Wrapped stack shape documented in issue #1341
        // see https://github.com/qzind/tray/issues/1341
        PrinterException ex = new PrinterException("javax.print.PrintException: java.awt.print.PrinterIOException");
        ex.initCause(new PrinterIOException(new IOException("spool failed")));

        Assert.assertTrue(PrintingUtilities.hasPrinterIoWrapper(ex));
        Assert.assertTrue(PrintingUtilities.isLikelyCupsBsdMissing(ex, false));
        Assert.assertFalse(PrintingUtilities.isLikelyCupsBsdMissing(ex, true));
    }

    @Test
    public void cupsBsdHintTests() {
        // Alternate error surface noted in issue #1341
        // see https://github.com/qzind/tray/issues/1341
        PrinterException ex = new PrinterException("lpr: command not found");

        PrinterException hinted = PrintingUtilities.exceptionWithCupsBsdHint(ex, true, false);

        Assert.assertNotSame(hinted, ex);
        Assert.assertEquals(hinted.getMessage(), PrintingUtilities.getLinuxLprMessage());
        Assert.assertSame(hinted.getCause(), ex);
    }

    @Test
    public void unrelatedPrintErrorTests() {
        PrinterException ex = new PrinterException("Printer is offline");

        Assert.assertFalse(PrintingUtilities.hasLprSignature(ex));
        Assert.assertFalse(PrintingUtilities.hasPrinterIoWrapper(ex));
        Assert.assertFalse(PrintingUtilities.isLikelyCupsBsdMissing(ex, false));

        PrinterException actualException = PrintingUtilities.exceptionWithCupsBsdHint(ex, true, false);

        Assert.assertSame(actualException, ex);
    }

    @Test
    public void nonLinuxPrintErrorTests() {
        // Alternate error surface noted in issue #1341
        // see https://github.com/qzind/tray/issues/1341
        PrinterException nonLinux = new PrinterException("lpr: command not found");

        PrinterException actualException = PrintingUtilities.exceptionWithCupsBsdHint(nonLinux, false, false);

        Assert.assertSame(actualException, nonLinux);
    }
}