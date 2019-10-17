package qz.printer.info;

import com.sun.jna.platform.win32.Advapi32Util;
import qz.utils.WindowsUtilities;

import javax.print.PrintService;

import static com.sun.jna.platform.win32.WinReg.*;

public class WindowsPrinterMap extends NativePrinterMap {
    public synchronized NativePrinterMap putAll(PrintService[] services) {
        for (PrintService service : findMissing(services)) {
            String name = service.getName();
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
