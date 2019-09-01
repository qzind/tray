package qz.printer.info;

import javax.print.PrintService;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import java.util.ArrayList;
import java.util.List;

public class NativePrinter {
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
    private boolean outdated;
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
        this.outdated = false;
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

    public String getName() {
        if (printService != null && printService.get() != null) {
            return printService.get().getName();
        }
        return null;
    }

    public PrinterName getLegacyName() {
        if (printService != null && printService.get() != null) {
            return printService.get().getAttribute(PrinterName.class);
        }
        return null;

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

    public List<Integer> getResolutions() {
        // TODO: Test/Implement supported resolutions for CUPS
        List<Integer> densities = new ArrayList<>();

        PrintService ps = getPrintService().get();
        PrinterResolution[] resSupport = (PrinterResolution[])ps.getSupportedAttributeValues(PrinterResolution.class, ps.getSupportedDocFlavors()[0], null);
        if (resSupport == null || resSupport.length == 0) {
            resSupport = new PrinterResolution[]{ getResolution().get() };
            }
        if (resSupport != null) {
            for(PrinterResolution res : resSupport) {
                densities.add(res.getFeedResolution(ResolutionSyntax.DPI));
            }
        }

        return densities;
    }

    public synchronized static void getDriverAttributes(NativePrinter printer) {
        printer.driver.init();
        printer.resolution.init();
        NativePrinterList.getInstance().fillAttributes(printer);
    }

    public boolean isOutdated() {
        return outdated;
    }

    public void setOutdated(boolean outdated) {
        this.outdated = outdated;
    }
}
