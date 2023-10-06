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
    private static final CachedObject<PrintService> cachedDefault = new CachedObject<>(CachedPrintServiceLookup::wrapDefaultPrintService);
    private static final CachedObject<PrintService[]> cachedPrintServices = new CachedObject<>(CachedPrintServiceLookup::wrapPrintServices);
    private static CachedPrintService[] oldPrintServices = {};

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

    private static PrintService wrapDefaultPrintService() {
        PrintService javaxPrintService = PrintServiceLookup.lookupDefaultPrintService();
        // If this CachedPrintService already exists, reuse it rather than wrapping a new one
        CachedPrintService oldCachedPrintService = getMatch(oldPrintServices, javaxPrintService);
        if (oldCachedPrintService != null) return oldCachedPrintService;
        return new CachedPrintService(PrintServiceLookup.lookupDefaultPrintService());
    }

    private static CachedPrintService[] wrapPrintServices() {
        PrintService[] javaxPrintServices = PrintServiceLookup.lookupPrintServices(null, null);
        CachedPrintService[] cachedPrintServices = new CachedPrintService[javaxPrintServices.length];
        for (int i = 0; i < javaxPrintServices.length; i++) {
            // If this CachedPrintService already exists, reuse it rather than wrapping a new one
            cachedPrintServices[i] = getMatch(oldPrintServices, javaxPrintServices[i]);
            if (cachedPrintServices[i] == null) {
                cachedPrintServices[i] = new CachedPrintService(javaxPrintServices[i]);
            }
        }
        oldPrintServices = cachedPrintServices;
        return cachedPrintServices;
    }

    private static CachedPrintService getMatch(CachedPrintService[] array, PrintService javaxPrintService) {
        for (CachedPrintService cps : array) {
            if (cps.getJavaxPrintService() == javaxPrintService) return cps;
        }
        return null;
    }
}
