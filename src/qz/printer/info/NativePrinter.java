package qz.printer.info;

import qz.utils.SystemUtilities;

import javax.print.PrintService;
import javax.print.attribute.standard.PrinterResolution;
import java.util.Date;

class NativePrinter {
    /**
     * Simple object wrapper allowing lazy fetching of values
     * @param <T>
     */
    public class PrinterProperty<T> {
        T content;
        boolean initialized;

        public PrinterProperty() {
            this.initialized = false;
        }

        @Override
        public String toString() {
            if (content == null) {
                return null;
            } else if (content instanceof String) {
                return (String)content;
            } return content.toString();
        }

        public void set(T content) {
            this.content = content;
            this.initialized = true;
        }

        public T get() {
            return content;
        }

        public boolean isNull() {
            return content == null;
        }

        public void init() {
            this.initialized = true;
        }

        public boolean isInit() {
            return initialized;
        }

        @Override
        public boolean equals(Object o) {
            if (content != null) {
                return content.equals(o);
            }
            return false;
        }
    }

    private final String printerId;
    private PrinterProperty<String> description;
    private PrinterProperty<PrintService> printService;
    private PrinterProperty<String> driver;
    private PrinterProperty<PrinterResolution> resolution;
    private PrinterProperty<String> driverFile;

    public NativePrinter(String printerId) {
        this.printerId = printerId;
        this.description = new PrinterProperty<>();
        this.printService = new PrinterProperty<>();
        this.driverFile = new PrinterProperty<>();
        this.driver = new PrinterProperty<>();
        this.resolution = new PrinterProperty<>();
    }

    public PrinterProperty<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public PrinterProperty<String> getDriverFile() {
        return driverFile;
    }

    public void setDriverFile(String driverFile) {
        this.driverFile.set(driverFile);
    }

    public PrinterProperty<String> getDriver() {
        if (!driver.isInit()) {
            getDriverAttributes(this);
        }
        return driver;
    }

    public void setDriver(String driver) {
        this.driver.set(driver);
    }

    public PrinterProperty<PrintService> getPrintService() {
        return printService;
    }

    public void setPrintService(PrintService printService) {
        // Fetch resolution, if available
        Object resolution = printService.getDefaultAttributeValue(PrinterResolution.class);
        if (resolution != null) {
            this.resolution.set((PrinterResolution)resolution);
        }
        this.printService.set(printService);
    }

    public String getPrinterId() {
        return printerId;
    }

    public void setResolution(PrinterResolution resolution) {
        this.resolution.set(resolution);
    }

    public PrinterProperty<PrinterResolution> getResolution() {
        if (!resolution.isInit()) {
            getDriverAttributes(this);
        }
        return resolution;
    }

    public synchronized static void getDriverAttributes(NativePrinter printer) {
        printer.driver.init();
        printer.resolution.init();
        Wrapper.getInstance().fillAttributes(printer);
    }

}
