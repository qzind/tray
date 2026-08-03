package qz.utils;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import javax.print.PrintService;
import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

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

    @Test
    public void unixPrintJobLprTests() throws Exception {
        if (!SystemUtilities.isLinux()) {
            throw new SkipException("UnixPrintJob lpr command test is Linux-only");
        }

        // This intentionally reflects into JVM's Unix print command builder
        // QZ's cups-bsd hint depends on Java pixel printing using /usr/bin/lpr
        Class<?> unixPrintJobClass = Class.forName("sun.print.UnixPrintJob");
        Constructor<?> constructor = unixPrintJobClass.getDeclaredConstructor(PrintService.class);
        makeAccessible(constructor);

        Object unixPrintJob = constructor.newInstance(fakePrintService());

        Method printExecCmd = unixPrintJobClass.getDeclaredMethod(
                "printExecCmd",
                String.class,
                String.class,
                boolean.class,
                String.class,
                int.class,
                String.class
        );
        makeAccessible(printExecCmd);

        String[] command = (String[])printExecCmd.invoke(
                unixPrintJob,
                "Test_Printer",        // printer destination, produces -PTest_Printer
                "media=Letter",        // CUPS option, produces -o media=Letter
                true,                  // suppress job sheets, produces -h
                "QZ Tray Pixel Print", // job title, produces -J QZ Tray Pixel Print
                1,                     // one copy, omits -# copies
                "/tmp/javaprint-test"  // spool file path appended as the final argument
        );

        Assert.assertEquals(command[0], "/usr/bin/lpr", "JVM-generated print command: " + Arrays.toString(command));
    }

    private static PrintService fakePrintService() {
        return (PrintService)Proxy.newProxyInstance(
                PrintingUtilitiesTests.class.getClassLoader(),
                new Class[] { PrintService.class },
                (proxy, method, args) -> {
                    switch(method.getName()) {
                        case "getName":
                            return "PDF";
                        case "getDefaultAttributeValue":
                            return null;
                        case "isAttributeCategorySupported":
                            return false;
                        default:
                            return defaultValue(method.getReturnType());
                    }
                }
        );
    }

    private static void makeAccessible(AccessibleObject object) {
        try {
            object.setAccessible(true);
        } catch(InaccessibleObjectException | SecurityException e) {
            throw new SkipException("Accessing sun.print requires " + "--add-opens java.desktop/sun.print=ALL-UNNAMED", e);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == byte.class) {
            return (byte)0;
        }
        if (type == short.class) {
            return (short)0;
        }
        if (type == char.class) {
            return (char)0;
        }
        return null;
    }
}