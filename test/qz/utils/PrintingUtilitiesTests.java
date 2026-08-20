package qz.utils;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import javax.print.PrintService;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class PrintingUtilitiesTests {

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
