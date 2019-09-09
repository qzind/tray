package qz.printer.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NativePrinterMap extends ConcurrentHashMap<String, NativePrinter> {
    private static final Logger log = LoggerFactory.getLogger(NativePrinterMap.class);

    private static NativePrinterMap instance;

    public abstract NativePrinterMap putAll(PrintService[] services);
    abstract void fillAttributes(NativePrinter printer);

    public static NativePrinterMap getInstance() {
        if (instance == null) {
            if (SystemUtilities.isWindows()) {
                instance = new WindowsPrinterMap();
            } else {
                instance = new CupsPrinterMap();
            }
        }
        return instance;
    }

    public String getPrinterId(String description) {
        for(Map.Entry<String,NativePrinter> entry : entrySet()) {
            NativePrinter info = entry.getValue();
            if (info.getDescription().equals(description)) {
                return entry.getKey();
            }
        }
        log.warn("Could not find printerId for " + description);
        return description;
    }

    public ArrayList<PrintService> findMissing(PrintService[] services) {
        ArrayList<PrintService> serviceList = new ArrayList<>(Arrays.asList(services)); // shrinking list drastically improves performance
        for(NativePrinter printer : values()) {
            if (serviceList.contains(printer.getPrintService())) {
                serviceList.remove(printer.getPrintService()); // existing match
            } else {
                printer.setOutdated(true); // no matches, mark to be removed
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
}
