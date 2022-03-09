package qz.printer.info;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ShellUtilities;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import javax.print.attribute.standard.PrinterResolution;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CupsPrinterMap extends NativePrinterMap {
    private static final String DEFAULT_CUPS_DRIVER = "TEXTONLY.ppd";
    private static final Logger log = LogManager.getLogger(CupsPrinterMap.class);
    private Map<NativePrinter, List<PrinterResolution>> resolutionMap = new HashMap<>();

    public synchronized NativePrinterMap putAll(PrintService... services) {
        ArrayList<PrintService> missing = findMissing(services);
        if (missing.isEmpty()) { return this; }

        String output = "\n" + ShellUtilities.executeRaw(new String[] {"lpstat", "-l", "-p"});
        String[] devices = output.split("[\\r\\n]printer ");

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
                    printer.getDescription().set();
                    printer.getDriverFile().set();
                } else {
                    String match = "Description:";
                    if (printer.getDescription().isNull() && line.startsWith(match)) {
                        printer.setDescription(line.substring(line.indexOf(match) + match.length()).trim());
                    }
                    match = "Interface:";
                    if (printer.getDriverFile().isNull() && line.startsWith(match)) {
                        printer.setDriverFile(line.substring(line.indexOf(match) + match.length()).trim());
                    }
                    if (!printer.getDescription().isNull() && !printer.getDriverFile().isNull()) {
                        break;
                    }
                }
            }

            for (PrintService service : missing) {
                if ((SystemUtilities.isMac() && printer.getDescription().equals(service.getName()))
                        || (SystemUtilities.isLinux() && printer.getPrinterId().equals(service.getName()))) {
                    printer.setPrintService(service);
                    missing.remove(service);
                    break;
                }
            }

            if (!printer.getPrintService().isNull()) {
                put(printer.getPrinterId(), printer);
            }
        }
        return this;
    }

    synchronized void addResolution(NativePrinter printer, PrinterResolution resolution) {
        List<PrinterResolution> resolutions = resolutionMap.get(printer);
        if(resolutions == null) {
            resolutions = new ArrayList<>();
            resolutionMap.put(printer, resolutions);
        }
        if(!resolutions.contains(resolution)) {
            resolutions.add(resolution);
        }
    }

    synchronized List<PrinterResolution> getResolutions(NativePrinter printer) {
        if(resolutionMap.get(printer) == null) {
            fillAttributes(printer);
        }
        return resolutionMap.get(printer);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if(value instanceof NativePrinter) {
            resolutionMap.remove(value);
        }
        return super.remove(key, value);
    }

    /**
     * Parse "*DefaultResolution" line from CUPS .ppd file
     * @param line
     * @return
     */
    public static PrinterResolution parseDefaultResolution(String line) {
        try {
            String[] parts = line.split("x");
            int cross = Integer.parseInt(parts[0].replaceAll("\\D+", ""));
            int feed = parts.length > 1? Integer.parseInt(parts[1].replaceAll("\\D+", "")):cross;
            int type = line.toLowerCase(Locale.ENGLISH).contains("dpi")? PrinterResolution.DPI:PrinterResolution.DPCM;
            return new PrinterResolution(cross, feed, type);
        } catch(NumberFormatException nfe) {
            log.warn("Could not parse density from \"{}\"", line);
        }
        return null;
    }

    /**
     * Parse "/HWResolution[" line from CUPS .ppd file
     * @param line
     * @return
     */
    public static PrinterResolution parseAdditionalResolution(String line) {
        try {
            String[] parts = line.split("/HWResolution\\[")[1].split("\\D"); // split on non-digits
            int cross = Integer.parseInt(parts[0]);
            int feed = parts.length > 1? Integer.parseInt(parts[1]) : cross;
            return new PrinterResolution(cross, feed, PrinterResolution.DPI); // always dpi per https://www.cups.org/doc/spec-ppd.html
        } catch(NumberFormatException nfe) {
            log.warn("Could not parse density from \"{}\"", line, nfe);
        }
        return null;
    }

    synchronized void fillAttributes(NativePrinter printer) {
        String options = ShellUtilities.executeRaw("lpoptions", "-p", printer.getPrinterId());
        String connection = null;
        int start;
        int end;
        String section;
        if((start = options.indexOf("device-uri=")) != -1) {
            section = options.substring(start);
            if((end = section.indexOf(' ')) > 0) {
                connection = section.substring(section.indexOf("=") + 1, end);
            } else {
                connection = section.substring(section.indexOf("=") + 1);
            }
        }
        printer.setConnection(connection);

        if (!printer.getDriverFile().isNull()) {
            File ppdFile = new File(printer.getDriverFile().value());
            try {
                BufferedReader buffer = new BufferedReader(new FileReader(ppdFile));
                String line;

                while((line = buffer.readLine()) != null) {
                    if (line.contains("*DefaultResolution:")) {
                        // Parse default printer resolution
                        PrinterResolution defaultRes = parseDefaultResolution(line);
                        if(defaultRes != null) {
                            printer.setResolution(defaultRes);
                            addResolution(printer, defaultRes);
                        }
                    } else if(line.contains("/HWResolution[")) {
                        PrinterResolution additionalRes = parseAdditionalResolution(line);
                        if(additionalRes != null) {
                            addResolution(printer, additionalRes);
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
        if(resolutionMap.get(printer) == null) {
            addResolution(printer, null); // create empty list
        }
    }
}
