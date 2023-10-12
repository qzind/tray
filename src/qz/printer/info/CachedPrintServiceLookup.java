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
    private static final CachedObject<CachedPrintService> cachedDefault = new CachedObject<>(CachedPrintServiceLookup::wrapDefaultPrintService);
    private static final CachedObject<CachedPrintService[]> cachedPrintServices = new CachedObject<>(CachedPrintServiceLookup::wrapPrintServices);

    // Keep CachedPrintService object references between calls to supplier
    private static CachedPrintService[] cachedPrintServicesCopy = {};

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

    private static CachedPrintService wrapDefaultPrintService() {
        PrintService javaxPrintService = PrintServiceLookup.lookupDefaultPrintService();
        // CachedObject's supplier returns null
        if(javaxPrintService == null) {
            return null;
        }
        // If this CachedPrintService already exists, reuse it rather than wrapping a new one
        CachedPrintService cachedPrintService = getMatch(cachedPrintServicesCopy, javaxPrintService);
        if (cachedPrintService == null) {
            // Wrap a new one
            cachedPrintService = new CachedPrintService(javaxPrintService);
        }
        return cachedPrintService;
    }

    private static CachedPrintService[] wrapPrintServices() {
        PrintService[] javaxPrintServices = PrintServiceLookup.lookupPrintServices(null, null);
        CachedPrintService[] cachedPrintServices = new CachedPrintService[javaxPrintServices.length];
        for (int i = 0; i < javaxPrintServices.length; i++) {
            // If this CachedPrintService already exists, reuse it rather than wrapping a new one
            cachedPrintServices[i] = getMatch(cachedPrintServicesCopy, javaxPrintServices[i]);
            if (cachedPrintServices[i] == null) {
                cachedPrintServices[i] = new CachedPrintService(javaxPrintServices[i]);
            }
        }
        cachedPrintServicesCopy = cachedPrintServices;
        // Immediately invalidate the defaultPrinter cache
        cachedDefault.get(true);

        return cachedPrintServices;
    }

    private static CachedPrintService getMatch(CachedPrintService[] array, PrintService javaxPrintService) {
        if(array != null) {
            for(CachedPrintService cps : array) {
                // Note: Order of operations can cause the defaultService pointer to differ if lookupDefaultPrintService()
                // is called before lookupPrintServices(...) because the provider will invalidate on refreshServices() if
                // "sun.java2d.print.polling" is set to "false". We're OK with this because worst case, we just
                // call "lpstat" a second time.
                if (cps.getJavaxPrintService() == javaxPrintService) return cps;
            }
        }
        return null;
    }
}
