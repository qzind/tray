package qz.printer.info;

import qz.utils.SystemUtilities;

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
        T value;
        boolean set;

        public PrinterProperty() {
            this.set = false;
        }

        @Override
        public String toString() {
            if (value == null) {
                return null;
            } else if (value instanceof String) {
                return (String)value;
            } return value.toString();
        }

        public void set() {
            this.set = true;
        }

        public void set(T content) {
            this.value = content;
            this.set = true;
        }

        public T value() {
            return value;
        }

        public boolean isSet() {
            return set;
        }

        public boolean isNull() {
            return value == null;
        }

        @Override
        public boolean equals(Object o) {
            // PrintService.equals(...) is very slow in CUPS; use the pointer
            if (SystemUtilities.isUnix() && value instanceof PrintService) {
                return o == value;
            }
            if (value != null) {
                return value.equals(o);
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
        if (!driver.isSet()) {
            getDriverAttributes(this);
        }
        return driver;
    }

    public String getName() {
        if (printService != null && printService.value() != null) {
            return printService.value().getName();
        }
        return null;
    }

    public PrinterName getLegacyName() {
        if (printService != null && printService.value() != null) {
            return printService.value().getAttribute(PrinterName.class);
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
        if (!resolution.isSet()) {
            getDriverAttributes(this);
        }
        return resolution;
    }

    public List<Integer> getResolutions() {
        // TODO: Test/Implement supported resolutions for CUPS
        List<Integer> densities = new ArrayList<>();

        PrintService ps = getPrintService().value();
        PrinterResolution[] resSupport = (PrinterResolution[])ps.getSupportedAttributeValues(PrinterResolution.class, ps.getSupportedDocFlavors()[0], null);
        if (resSupport == null || resSupport.length == 0) {
            resSupport = new PrinterResolution[]{ getResolution().value() };
        }
        for(PrinterResolution res : resSupport) {
            densities.add(res.getFeedResolution(ResolutionSyntax.DPI));
        }

        return densities;
    }

    public static void getDriverAttributes(NativePrinter printer) {
        printer.driver.set();
        printer.resolution.set();
        NativePrinterMap.getInstance().fillAttributes(printer);
    }

    public boolean isOutdated() {
        return outdated;
    }

    public void setOutdated(boolean outdated) {
        this.outdated = outdated;
    }
}
