package qz.printer.info;

import qz.common.CachedObject;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

/**
 * PrintService[] cache to workaround JDK-7001133
 *
 * See also <code>CachedPrintService</code>
 */
public class CachedPrintServiceLookup {
    private static final CachedObject<PrintService> cachedDefault = new CachedObject<>(CachedPrintServiceLookup::innerLookupDefaultPrintService);
    private static final CachedObject<PrintService[]> cachedPrintServices = new CachedObject<>(CachedPrintServiceLookup::innerLookupPrintServices);

    static {
        setLifespan(CachedObject.DEFAULT_LIFESPAN);
    }

    public static PrintService lookupDefaultPrintService() {
        return cachedDefault.get();
    }

    public static void setLifespan(long lifespan) {
        cachedDefault.setLifespan(lifespan);
        cachedPrintServices.setLifespan(lifespan);
    }

    public static PrintService[] lookupPrintServices() {
        return cachedPrintServices.get();
    }

    private static PrintService innerLookupDefaultPrintService() {
        return new CachedPrintService(PrintServiceLookup.lookupDefaultPrintService());
    }

    private static PrintService[] innerLookupPrintServices() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        for (int i = 0; i < printServices.length; i++) {
            printServices[i] = new CachedPrintService(printServices[i]);
        }
        return printServices;
    }
}
