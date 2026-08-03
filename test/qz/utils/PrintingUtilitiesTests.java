package qz.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.io.IOException;

public class PrintingUtilitiesTests {

    @Test
    public void historicalLprCommandFailureClassifierTests() {
        PrinterException ex = new PrinterException(
                // Classifier regression coverage for
                // the error shape reported in issue #642
                // This does not exercise the JVM print path
                // it protects QZ's known-error detection
                "Error: java.io.IOException: error=1 running: '/usr/bin/lpr' '-PPDF' " +
                        "'-J QZ Tray Pixel Print' '-h' '-o media=Letter' '/tmp/javaprint123' " +
                        "usage: lpr [-cdfghlmnpqrstv]"
        );

        Assert.assertTrue(PrintingUtilities.hasLprSignature(ex));
        Assert.assertTrue(PrintingUtilities.isLikelyCupsBsdMissing(ex, true));
    }

    @Test
    public void historicalMissingLprClassifierTests() {
        // Classifier regression coverage for
        // an alternate error surface reported in issue #1341
        // This keeps the historical missing-lpr
        // message shape recognized by the helper
        PrinterException ex = new PrinterException("lpr: command not found");

        Assert.assertTrue(PrintingUtilities.hasLprSignature(ex));
        Assert.assertTrue(PrintingUtilities.isLikelyCupsBsdMissing(ex, false));
    }

    @Test
    public void historicalNestedMissingLprClassifierTests() {
        PrinterException ex = new PrinterException("Failed to print");
        // Classifier regression coverage for the nested missing-lpr shape from the
        // Raspberry Pi thread linked by issue #1341
        ex.initCause(new IOException("Cannot run program \"/usr/bin/lpr\": error=2, No such file or directory"));

        Assert.assertTrue(PrintingUtilities.hasLprSignature(ex));
        Assert.assertTrue(PrintingUtilities.isLikelyCupsBsdMissing(ex, true));
    }

    @Test
    public void printerIoWrapperClassifierTests() {
        // Classifier regression coverage for the wrapped Java print failure shape
        // documented in issue #1341
        PrinterException ex = new PrinterException("javax.print.PrintException: java.awt.print.PrinterIOException");
        ex.initCause(new PrinterIOException(new IOException("spool failed")));

        Assert.assertTrue(PrintingUtilities.hasPrinterIoWrapper(ex));
        Assert.assertTrue(PrintingUtilities.isLikelyCupsBsdMissing(ex, false));
        Assert.assertFalse(PrintingUtilities.isLikelyCupsBsdMissing(ex, true));
    }

    @Test
    public void cupsBsdHintWrapsClassifiedHistoricalFailureTests() {
        // Verifies QZ's hint wrapping for a classified historical error shape
        PrinterException ex = new PrinterException("lpr: command not found");

        PrinterException hinted = PrintingUtilities.exceptionWithCupsBsdHint(ex, true, false);

        Assert.assertNotSame(hinted, ex);
        Assert.assertEquals(hinted.getMessage(), PrintingUtilities.getLinuxLprMessage());
        Assert.assertSame(hinted.getCause(), ex);
    }

    @Test
    public void unrelatedPrintErrorIsNotClassifiedAsCupsBsdTests() {
        PrinterException ex = new PrinterException("Printer is offline");

        Assert.assertFalse(PrintingUtilities.hasLprSignature(ex));
        Assert.assertFalse(PrintingUtilities.hasPrinterIoWrapper(ex));
        Assert.assertFalse(PrintingUtilities.isLikelyCupsBsdMissing(ex, false));

        PrinterException actualException = PrintingUtilities.exceptionWithCupsBsdHint(ex, true, false);

        Assert.assertSame(actualException, ex);
    }

    @Test
    public void unrelatedMissingExecutableIsNotClassifiedAsCupsBsdTests() {
        PrinterException ex = new PrinterException("Cannot run program \"/usr/bin/filter\": error=2, No such file or directory");

        Assert.assertFalse(PrintingUtilities.hasLprSignature(ex));
        Assert.assertFalse(PrintingUtilities.isLikelyCupsBsdMissing(ex, false));

        PrinterException actualException = PrintingUtilities.exceptionWithCupsBsdHint(ex, true, false);

        Assert.assertSame(actualException, ex);
    }

    @Test
    public void nonLinuxHistoricalLprErrorIsNotWrappedTests() {
        // Verifies that historical lpr-shaped failures
        // are only rewritten by the Linux-specific path
        PrinterException nonLinux = new PrinterException("lpr: command not found");

        PrinterException actualException = PrintingUtilities.exceptionWithCupsBsdHint(nonLinux, false, false);

        Assert.assertSame(actualException, nonLinux);
    }
}