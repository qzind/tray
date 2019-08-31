package qz.printer.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import javax.print.attribute.standard.PrinterResolution;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CupsWrapper extends Wrapper {
    private static final String DEFAULT_CUPS_DRIVER = "TEXTONLY.ppd";
    private static final Logger log = LoggerFactory.getLogger(CupsWrapper.class);

    public NativePrinterList wrapServices(PrintService[] services, NativePrinterList existing) {
        // Iterating PrintServices is slow in CUPS; ArrayList allows us to pop items as they're found
        ArrayList<PrintService> serviceList = new ArrayList<>(Arrays.asList(services));

        NativePrinterList printers = existing != null ? existing : new NativePrinterList();

        String output = "\n" + ShellUtilities.executeRaw(new String[] {"lpstat", "-l", "-p"});
        String[] devices = output.split("[\\r\\n]printer ");

        // TODO: Find way to NOT may system calls when a printer's already been added
        for (String device : devices) {
            if (device.trim().isEmpty()) {
                continue;
            }
            NativePrinter printer = null;
            String[] lines = device.split("\\r?\\n");
            for(String line : lines) {
                line = line.trim();
                if (printer == null) {
                    printer = new NativePrinter(line.split("\\s+")[0]);
                } else {
                    String match = "Description:";
                    if (!printer.getDescription().isInit() && line.startsWith(match)) {
                        printer.setDescription(line.substring(line.indexOf(match) + match.length()).trim());
                    }
                    match = "Interface:";
                    if (!printer.getDriverFile().isInit() && line.startsWith(match)) {
                        printer.setDriverFile(line.substring(line.indexOf(match) + match.length()).trim());
                    }
                    if (printer.getDescription().isInit() && printer.getDriverFile().isInit()) {
                        break;
                    }
                }
            }
            for (PrintService service : serviceList) {
                if (SystemUtilities.isMac()) {
                    if (printer.getDescription().equals(service.getName())) {
                        printer.setPrintService(service);
                        serviceList.remove(service);
                        break;
                    }
                } else {
                    if (printer.getPrinterId().equals(service.getName())) {
                        printer.setPrintService(service);
                        serviceList.remove(service);
                        break;
                    }
                }
            }

            if (printer.getPrintService().isInit()) {
                printer.getDescription().init();
                printer.getDriverFile().init();
                printers.put(printer.getPrinterId(), printer);
            }
        }
        return printers;
    }

    public void fillAttributes(NativePrinter printer) {
        if (!printer.getDriverFile().isNull()) {
            File ppdFile = new File(printer.getDriverFile().get());
            try {
                BufferedReader buffer = new BufferedReader(new FileReader(ppdFile));
                String line;

                while((line = buffer.readLine()) != null) {
                    if (line.contains("*DefaultResolution:")) {
                        // Parse printer resolution
                        try {
                            int density = Integer.parseInt(line.split("x")[0].replaceAll("\\D+", ""));
                            int type = line.toLowerCase().contains("dpi")? PrinterResolution.DPI:PrinterResolution.DPCM;
                            printer.setResolution(new PrinterResolution(density, density, type));
                        } catch(NumberFormatException e) {
                            log.warn("Could not parse density from \"{}\"", line);
                        }
                    } else if(line.contains("*PCFileName:")) {
                        // Parse driver name
                        String[] split = line.split("\\*PCFileName:");
                        printer.setDriver(split[split.length - 1].replace("\"", "").trim());
                    }
                }
            } catch(IOException e) {
                log.error("Something went wrong while reading " + printer.getDriverFile());
            }
        }
        if (printer.getDriver().isNull()) {
            printer.setDriver(DEFAULT_CUPS_DRIVER);
        }
    }
}
