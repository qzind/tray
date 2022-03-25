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

    public abstract NativePrinterMap putAll(PrintService... services);

    abstract void fillAttributes(NativePrinter printer);

    public static NativePrinterMap getInstance() {
        if (instance == null) {
            switch(SystemUtilities.getOsType()) {
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

    public NativePrinter get(PrintService service) {
        for (NativePrinter printer : values()) {
            if (printer.getPrintService().equals(service)) {
                return printer;
            }
        }
        return null;
    }
}
