package qz.printer.info;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NativePrinterMap extends ConcurrentHashMap<String, NativePrinter> {
    private static final Logger log = LogManager.getLogger(NativePrinterMap.class);

    private static NativePrinterMap instance;

    public abstract NativePrinterMap putAll(boolean exhaustive, PrintService... services);

    abstract void fillAttributes(NativePrinter printer);

    public static NativePrinterMap getInstance() {
        if (instance == null) {
            switch(SystemUtilities.getOs()) {
                case WINDOWS:
                    instance = new WindowsPrinterMap();
                    break;
                default:
                    instance = new CupsPrinterMap();
            }
        }
        return instance;
    }

    public String lookupPrinterId(String description) {
        for(Map.Entry<String,NativePrinter> entry : entrySet()) {
            NativePrinter info = entry.getValue();
            if (description.equals(info.getPrintService().value().getName())) {
                return entry.getKey();
            }
        }
        log.warn("Could not find printerId for " + description);
        return null;
    }

    /**
     * WARNING: Despite the function's name, if <code>exhaustive</code> is true, it will treat the listing as exhaustive and remove
     * any PrintServices that are not part of this HashMap.
     */
    public ArrayList<PrintService> findMissing(boolean exhaustive, PrintService[] services) {
        ArrayList<PrintService> serviceList = new ArrayList<>(Arrays.asList(services)); // shrinking list drastically improves performance

        for(NativePrinter printer : values()) {
            int index = serviceList.indexOf(printer.getPrintService());
            if (index >= 0) {
                // Java's `PrintService.equals(o)` method uses getName().equals(). This causes issues if a stale PrintService has been replaced
                // by a new PrintService of the same name. For that reason, we always refresh the PrintService reference in NativePrinter.
                // See: https://github.com/qzind/tray/issues/1259
                printer.setPrintService(serviceList.get(index));
                serviceList.remove(printer.getPrintService()); // existing match
            } else {
                if(exhaustive) {
                    printer.setOutdated(true); // no matches, mark to be removed
                }
            }
        }

        // remove outdated
        for (Map.Entry<String, NativePrinter> entry : entrySet()) {
            if(entry.getValue().isOutdated()) {
                remove(entry.getKey());
            }
        }

        // any remaining services are new/missing
        return serviceList;
    }

    public boolean contains(PrintService service) {
        for (NativePrinter printer : values()) {
            if (printer.getPrintService().equals(service)) {
                return true;
            }
        }
        return false;
    }

    public NativePrinter get(PrintService service) {
        for (NativePrinter printer : values()) {
            if (printer.getPrintService().equals(service)) {
                return printer;
            }
        }
        return null;
    }
}
