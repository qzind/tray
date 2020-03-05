package qz.printer.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.WindowsUtilities;

import javax.print.PrintService;

import static com.sun.jna.platform.win32.WinReg.*;

public class WindowsPrinterMap extends NativePrinterMap {
    private static final Logger log = LoggerFactory.getLogger(WindowsPrinterMap.class);

    public synchronized NativePrinterMap putAll(PrintService[] services) {
        for (PrintService service : findMissing(services)) {
            String name = service.getName();
            if(name.equals("PageManager PDF Writer")) {
                log.warn("Printer \"{}\" is blacklisted, removing", name); // Per https://github.com/qzind/tray/issues/599
                continue;
            }
            NativePrinter printer = new NativePrinter(name);
            printer.setDescription(name);
            printer.setPrintService(service);
            put(printer.getPrinterId(), printer);
        }
        return this;
    }

    synchronized void fillAttributes(NativePrinter printer) {
        String keyName = printer.getPrinterId().replaceAll("\\\\", ",");
        String key = "SYSTEM\\CurrentControlSet\\Control\\Print\\Printers\\" + keyName;
        String driver = WindowsUtilities.getRegString(HKEY_LOCAL_MACHINE, key, "Printer Driver");
        if (driver == null) {
            key = "Printers\\Connections\\" + keyName;
            String guid = WindowsUtilities.getRegString(HKEY_CURRENT_USER, key, "GuidPrinter");
            if (guid != null) {
                String serverName = keyName.replaceAll(",,(.+),.+", "$1");
                key = "Software\\Microsoft\\Windows NT\\CurrentVersion\\Print\\Providers\\Client Side Rendering Print Provider\\Servers\\" + serverName + "\\Printers\\" + guid;
                driver = WindowsUtilities.getRegString(HKEY_LOCAL_MACHINE, key, "Printer Driver");
            }
        }
        printer.setDriver(driver);
    }
}
