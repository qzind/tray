package qz.printer.info;

import qz.utils.SystemUtilities;

import javax.print.PrintService;

public abstract class Wrapper {
    private static Wrapper instance;
    public abstract NativePrinterList wrapServices(PrintService[] services, NativePrinterList existing);
    public abstract void fillAttributes(NativePrinter printer);
    public static Wrapper getInstance() {
        if (instance == null) {
            if (SystemUtilities.isWindows()) {
                instance = new WinWrapper();
            } else {
                instance = new CupsWrapper();
            }
        }
        return instance;
    }
}
