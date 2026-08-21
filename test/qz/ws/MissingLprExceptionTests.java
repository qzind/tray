package qz.ws;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.print.PrintException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.io.IOException;

public class MissingLprExceptionTests {

    @Test
    public void wrapsMissingLprTests() {
        PrinterException ex = javaPrintFailure();

        Exception wrapped = MissingLprException.wrap(ex, true, false);

        Assert.assertTrue(wrapped instanceof MissingLprException);
        Assert.assertEquals(wrapped.getMessage(), "The 'lpr' command appears to be missing. Please install the 'cups-bsd' package and then try again.");
        Assert.assertSame(wrapped.getCause(), ex);
    }

    @Test
    public void ignoresAvailableLprTests() {
        PrinterException ex = javaPrintFailure();

        Assert.assertSame(MissingLprException.wrap(ex, true, true), ex);
    }

    @Test
    public void ignoresWrongCauseTests() {
        PrinterException ex = new PrinterException("Printer is offline");
        ex.initCause(new IOException("spool failed"));

        Assert.assertFalse(MissingLprException.isLprSignature(ex, true, false));
        Assert.assertSame(MissingLprException.wrap(ex, true, false), ex);
    }

    @Test
    public void ignoresNonLinuxTests() {
        PrinterException ex = javaPrintFailure();

        Assert.assertFalse(MissingLprException.isLprSignature(ex, false, false));
        Assert.assertSame(MissingLprException.wrap(ex, false, false), ex);
    }

    @Test
    public void wrapsIoCommandTests() {
        IOException ex = new IOException("error=2 running: '/usr/bin/lpr'");

        Exception wrapped = MissingLprException.wrap(ex, true, true);

        Assert.assertTrue(wrapped instanceof MissingLprException);
        Assert.assertSame(wrapped.getCause(), ex);
    }

    @Test
    public void ignoresOtherIoTests() {
        IOException ex = new IOException("Cannot run program \"/usr/bin/filter\": error=2, No such file or directory");

        Assert.assertFalse(MissingLprException.isLprSignature(ex, true, false));
        Assert.assertSame(MissingLprException.wrap(ex, true, false), ex);
    }

    private static PrinterException javaPrintFailure() {
        PrinterException ex = new PrinterException("Failed to print");
        ex.initCause(new PrintException(new PrinterIOException(new IOException("spool failed"))));
        return ex;
    }
}
