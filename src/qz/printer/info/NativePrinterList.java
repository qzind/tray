package qz.printer.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class NativePrinterList extends ConcurrentHashMap<String, NativePrinter> {
    private static NativePrinterList instance;

    public abstract NativePrinterList putAll(PrintService[] services);
    abstract void fillAttributes(NativePrinter printer);

    public static NativePrinterList getInstance() {
        if (instance == null) {
            if (SystemUtilities.isWindows()) {
                instance = new WindowsPrinterList();
            } else {
                instance = new CupsPrinterList();
            }
        }
        return instance;
    }
    private static final Logger log = LoggerFactory.getLogger(NativePrinterList.class);

    public PrintService[] getPrintServices() {
        PrintService[] services = new PrintService[this.size()];
        NativePrinter[] printers = this.values().toArray(new NativePrinter[this.size()]);

        for (int i = 0; i < this.size(); i++) {
            services[i] = printers[i].getPrintService().get();
        }
        return services;
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

    public PrintService[] findMissing(PrintService[] services) {
        ArrayList<PrintService> toAdd = new ArrayList<>();
        // Flag outdated
        for (NativePrinter printer : values()) {
            printer.setOutdated(true);
        }
        // Unflag
        Collection<NativePrinter> shrinkingList = new ArrayList<>(values()); // shrinking list drastically improves performance
        boolean found = false;
        for (PrintService service : services) {
            for (NativePrinter printer : shrinkingList) {
                if (printer.getPrintService().equals(service)) {
                    printer.setOutdated(false);
                    found = true;
                    shrinkingList.remove(printer);
                    break;
                }
            }
            if (!found) {
                toAdd.add(service);
            }
        }
        // Remove outdated
        for (Map.Entry<String, NativePrinter> entry : entrySet()) {
            if(entry.getValue().isOutdated()) {
                remove(entry.getKey());
            }
        }
        return toAdd.toArray(new PrintService[toAdd.size()]);
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
