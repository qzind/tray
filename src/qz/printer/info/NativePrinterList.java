package qz.printer.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.PrintService;
import java.util.HashMap;
import java.util.Map;

public class NativePrinterList extends HashMap<String, NativePrinter> {
    private static final Logger log = LoggerFactory.getLogger(NativePrinterList.class);

    public PrintService[] getPrintServices() {
        PrintService[] services = new PrintService[this.size()];
        NativePrinter[] printers = this.values().toArray(new NativePrinter[this.size()]);

        for (int i = 0; i < this.size(); i++) {
            services[i] = printers[i].getPrintService().get();
        }
        return services;
    }

    public String getPrinterId(PrintService service) {
        for(Map.Entry<String,NativePrinter> entry : entrySet()) {
            NativePrinter info = entry.getValue();
            if (info.getPrintService().equals(service)) {
                return entry.getKey();
            } else {
                return getPrinterId(service);
            }
        }
        log.warn("Could not find printerId for " + service.getName());
        return service.getName();
    }

    public boolean contains(PrintService service) {
        NativePrinter[] printers = this.values().toArray(new NativePrinter[this.size()]);

        for (int i = 0; i < this.size(); i++) {
            if (printers[i].getPrintService().equals(service)) {
                return true;
            }
        }
        return false;
    }
}
