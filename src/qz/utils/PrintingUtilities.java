package qz.utils;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.PrintProcessor;
import qz.printer.action.ProcessorFactory;
import qz.ws.PrintSocketClient;

import javax.print.PrintService;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.PrinterResolution;
import java.awt.print.PrinterAbortException;
import java.util.*;

public class PrintingUtilities {

    private static final Logger log = LoggerFactory.getLogger(PrintingUtilities.class);

    private static HashMap<String,String> CUPS_DESC; //cups name -> service.getName()
    private static HashMap<String,String> CUPS_PPD; //cups name -> PPD file path
    private static HashMap<String,PrinterResolution> CUPS_DPI; //cups name -> default dpi


    private static GenericKeyedObjectPool<Format,PrintProcessor> processorPool;


    private PrintingUtilities() {}

    public enum Type {
        PIXEL, RAW
    }

    public enum Format {
        COMMAND, DIRECT, HTML, IMAGE, PDF
    }

    public enum Flavor {
        BASE64, FILE, HEX, PLAIN, XML
    }


    public static Format getPrintFormat(JSONArray printData) throws JSONException {
        convertVersion(printData);

        //grab first data object to determine type for entire set
        JSONObject data = printData.optJSONObject(0);

        Format format;
        if (data == null) {
            format = Format.COMMAND;
        } else {
            format = Format.valueOf(data.optString("format", "COMMAND").toUpperCase(Locale.ENGLISH));
        }

        return format;
    }

    public synchronized static PrintProcessor getPrintProcessor(Format format) {
        try {
            if (processorPool == null) {
                processorPool = new GenericKeyedObjectPool<>(new ProcessorFactory());

                long memory = Runtime.getRuntime().maxMemory() / 1000000;
                if (memory < Constants.MEMORY_PER_PRINT) {
                    log.warn("Memory available is less than minimum required ({}/{} MB)", memory, Constants.MEMORY_PER_PRINT);
                }
                if (memory < Long.MAX_VALUE) {
                    int maxInst = Math.max(1, (int)(memory / Constants.MEMORY_PER_PRINT));
                    log.debug("Allowing {} simultaneous processors based on memory available ({} MB)", maxInst, memory);
                    processorPool.setMaxTotal(maxInst);
                    processorPool.setMaxTotalPerKey(maxInst);
                }
            }

            log.trace("Waiting for processor, {}/{} already in use", processorPool.getNumActive(), processorPool.getMaxTotal());
            return processorPool.borrowObject(format);
        }
        catch(Exception e) {
            throw new IllegalArgumentException(String.format("Unable to find processor for %s type", format.name()));
        }
    }

    /**
     * Version 2.1 introduced the flavor attribute to apply better control on raw data.
     * Essentially format became flavor, type become format, and type was rewritten.
     * Though a few exceptions exist due to the way additional raw options used to be handled.
     * <p>
     * This method will take the data object, and if it uses any old terminology it will update the value to the new set.
     *
     * @param dataArr JSONArray of printData, will update any data values by reference
     */
    private static void convertVersion(JSONArray dataArr) throws JSONException {
        for(int i = 0; i < dataArr.length(); i++) {
            JSONObject data = dataArr.optJSONObject(i);
            if (data == null) { data = new JSONObject(); }

            if (!data.isNull("flavor")) { return; } //flavor exists only in new version, no need to convert any data

            if (!data.isNull("format")) {
                String format = data.getString("format").toUpperCase();
                if (Arrays.asList("BASE64", "FILE", "HEX", "PLAIN", "XML").contains(format)) {
                    data.put("flavor", format);
                    data.remove("format");
                }
            }

            if (!data.isNull("type")) {
                String type = data.getString("type").toUpperCase();
                if (Arrays.asList("HTML", "IMAGE", "PDF").contains(type)) {
                    data.put("type", "PIXEL");
                    data.put("format", type);
                }
            }
        }
    }

    public static void releasePrintProcessor(PrintProcessor processor) {
        try {
            log.trace("Returning processor back to pool");
            processorPool.returnObject(processor.getFormat(), processor);
        }
        catch(Exception ignore) {}
    }

    /**
     * Gets the printerId for use with CUPS commands
     *
     * @return Id of the printer for use with CUPS commands
     */
    public static String getCupsPrinterId(PrintService service) {
        if (CUPS_DESC == null || !CUPS_DESC.containsValue(service.getName())) {
            cacheCupsInfo();
        }

        // On Mac, service.getName() is the Description field
        if (SystemUtilities.isMac()) {
            for(Map.Entry<String,String> entry : CUPS_DESC.entrySet()) {
                if (entry.getValue().equals(service.getName())) {
                    return entry.getKey();
                }
            }
            log.warn("Could not locate printerId matching {}", service.getName());
        }
        // On Linux, service.getName() is the CUPS ID instead of the description
        return service.getName();
    }


    /**
     * Returns a <code>HashMap</code> of name value pairs of printer name and printer description
     * On Linux, the description field is intentionally mapped to the printer name to match the Linux Desktop/<code>.ppd</code> behavior
     * On Mac, the description field is fetched separately to match the Mac Desktop/<code>.ppd</code> behavior
     * @return <code>HashMap</code> of name value pairs of printer name and printer description
     */
    public static void cacheCupsInfo() {
        CUPS_DESC = new HashMap<>();
        CUPS_PPD = new HashMap<>();
        String devices = ShellUtilities.executeRaw(new String[] {"lpstat", "-a"});

        for (String line : devices.split("\\r?\\n")) {
            String device = line.split(" ")[0];

            // Fetch CUPS printer Description and Interface values
            String props = ShellUtilities.executeRaw(new String[] {"lpstat", "-l", "-p", device});
            if (!props.isEmpty()) {
                for(String prop : props.split("\\r?\\n")) {
                    if (prop.trim().startsWith("Description:")) {
                        String[] split = prop.split("Description:");
                        if (split.length > 0) {
                            // cache the description so we can map it to the actual printer name
                            CUPS_DESC.put(device, split[split.length - 1].trim());
                            log.info(split[split.length - 1].trim() + ": " + device);
                        }
                    }
                    if (prop.trim().startsWith("Interface:")) {
                        String[] split = prop.split("Interface:");
                        if (split.length > 0) {
                            // cache the interface so we can use it for detecting DPI later
                            CUPS_PPD.put(device, split[split.length - 1].trim());
                            log.info(split[split.length - 1].trim() + ": " + device);
                        }
                    }
                }
            }
            // Fallback to using the CUPS id
            if (!CUPS_DESC.containsKey(device)) {
                CUPS_DESC.put(device, device);
            }
        }
    }

    /**
     * Fetches a <code>HashMap</code> of name value pairs of printer name and default density for CUPS enabled systems
     * @return <code>HashMap</code> of name value pairs of printer name and default density
     */
    public static void cacheCupsDensities() {
        HashMap<String, PrinterResolution> densityMap = new HashMap<>();
        for (Map.Entry<String, String> entry : CUPS_DESC.entrySet()) {
            String out = ShellUtilities.execute(
                    new String[]{"lpoptions", "-p", entry.getKey(), "-l"},
                    new String[] {
                            "Resolution/",
                            "Printer Resolution:",
                            "Output Resolution:"
                    }
            );
            // Check against the PPD file
            if (out.isEmpty()) {
                String ppd = CUPS_PPD.get(entry.getKey());
                if (ppd != null && !ppd.isEmpty()) {
                    out = ShellUtilities.execute(new String[] {"grep", "*DefaultResolution:", ppd}, new String[] {"*DefaultResolution:"});
                    if (!out.isEmpty()) {
                        // Mimic lpoptions format
                        String[] parts = out.split("\\*DefaultResolution:");
                        out = "*" + parts[parts.length - 1].trim();
                    }
                }
            }
            if (!out.isEmpty()) {
                String[] parts = out.split("\\s+");
                for (String part : parts) {
                    // parse default, i.e. [200dpi *300x300dpi 600dpi]
                    if (part.startsWith("*")) {
                        int type = part.toLowerCase().contains("dpi")? PrinterResolution.DPI:PrinterResolution.DPCM;

                        try {
                            int density = Integer.parseInt(part.split("x")[0].replaceAll("\\D+", ""));
                            densityMap.put(entry.getKey(), new PrinterResolution(density, density, type));
                            log.debug("Parsed default density from CUPS {}: {}{}", entry.getKey(), density,
                                      type == PrinterResolution.DPI? "dpi":"dpcm");
                        } catch(NumberFormatException ignore) {}
                    }
                }
            }
            if (!densityMap.containsKey(entry.getKey())) {
                densityMap.put(entry.getKey(), null);
                log.warn("Error parsing default density from CUPS, either no response or invalid response {}: {}", entry.getKey(), out);
            }
        }
        CUPS_DPI = densityMap;
    }

    public static HashMap<String, String> getCupsPrinters() {
        return CUPS_DESC;
    }

    public static PrinterResolution getNativeDensity(PrintService service) {
        if (service == null) { return null; }

        PrinterResolution pRes = (PrinterResolution)service.getDefaultAttributeValue(PrinterResolution.class);

        if (pRes == null && !SystemUtilities.isWindows()) {
            String printerId = getCupsPrinterId(service);

            if (CUPS_DPI == null || !CUPS_DPI.containsKey(printerId)) {
                cacheCupsDensities();
            }

            return CUPS_DPI.get(printerId);
        }

        log.debug("Found Resolution: {}", pRes);
        return pRes;
    }

    public static List<Integer> getSupportedDensities(PrintService service) {
        List<Integer> densities = new ArrayList<>();

        PrinterResolution[] resSupport = (PrinterResolution[])service.getSupportedAttributeValues(PrinterResolution.class, service.getSupportedDocFlavors()[0], null);
        if (resSupport != null) {
            for(PrinterResolution res : resSupport) {
                densities.add(res.getFeedResolution(ResolutionSyntax.DPI));
            }
        }

        return densities;
    }

    public static String getDriver(PrintService service) {
        String driver;

        if (SystemUtilities.isWindows()) {
            String regName = service.getName().replaceAll("\\\\", ",");
            String keyPath = "HKLM\\SYSTEM\\CurrentControlSet\\Control\\Print\\Printers\\" + regName;

            driver = ShellUtilities.execute(new String[] {"reg", "query", keyPath, "/v", "Printer Driver"}, new String[] {"REG_SZ"});
            if (!driver.isEmpty()) {
                driver = driver.substring(driver.indexOf("REG_SZ") + 6).trim();
            } else {
                String serverName = regName.replaceAll(",,(.+),.+", "$1");

                keyPath = "HKCU\\Printers\\Connections\\" + regName;
                String guid = ShellUtilities.execute(new String[] {"reg", "query", keyPath, "/v", "GuidPrinter"}, new String[] {"REG_SZ"});
                if (!guid.isEmpty()) {
                    guid = guid.substring(guid.indexOf("REG_SZ") + 6).trim();

                    keyPath = "HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion\\Print\\Providers\\Client Side Rendering Print Provider\\Servers\\" + serverName + "\\Printers\\" + guid;
                    driver = ShellUtilities.execute(new String[] {"reg", "query", keyPath, "/v", "Printer Driver"}, new String[] {"REG_SZ"});
                    if (!driver.isEmpty()) {
                        driver = driver.substring(driver.indexOf("REG_SZ") + 6).trim();
                    }
                }
            }
        } else {
            if (CUPS_PPD == null) {
                cacheCupsInfo();
            }
            driver = CUPS_PPD.get(getCupsPrinterId(service));
            // ShellUtilities.execute(new String[] {"lpstat", "-l", "-p", getCupsPrinterId(service)}, new String[] {"Interface:"});
            if (driver != null && !driver.isEmpty()) {
                driver = ShellUtilities.execute(new String[] {"grep", "*PCFileName:", driver}, new String[] {"*PCFileName:"});
                if (!driver.isEmpty()) {
                    return driver.substring(12).replace("\"", "").trim();
                }
            }
            /// Assume CUPS, raw
            return "TEXTONLY.ppd";
        }

        return driver;
    }

    /**
     * Determine print variables and send data to printer
     *
     * @param session WebSocket session
     * @param UID     ID of call from web API
     * @param params  Params of call from web API
     */
    public static void processPrintRequest(Session session, String UID, JSONObject params) throws JSONException {
        Format format = getPrintFormat(params.getJSONArray("data"));
        PrintProcessor processor = PrintingUtilities.getPrintProcessor(format);
        log.debug("Using {} to print", processor.getClass().getName());

        try {
            PrintOutput output = new PrintOutput(params.optJSONObject("printer"));
            PrintOptions options = new PrintOptions(params.optJSONObject("options"), output, format);

            processor.parseData(params.getJSONArray("data"), options);
            processor.print(output, options);
            log.info("Printing complete");

            PrintSocketClient.sendResult(session, UID, null);
        }
        catch(PrinterAbortException e) {
            log.warn("Printing cancelled");
            PrintSocketClient.sendError(session, UID, "Printing cancelled");
        }
        catch(Exception e) {
            log.error("Failed to print", e);
            PrintSocketClient.sendError(session, UID, e);
        }
        finally {
            PrintingUtilities.releasePrintProcessor(processor);
        }
    }

}
