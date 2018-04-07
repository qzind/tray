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

    private static HashMap<String,String> CUPS_DESC; //name -> description
    private static HashMap<String,PrinterResolution> CUPS_DPI; //description -> default dpi

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


    public synchronized static PrintProcessor getPrintProcessor(JSONArray printData) throws JSONException {
        JSONObject data = printData.optJSONObject(0);
        if (data == null) { data = new JSONObject(); }
        convertVersion(data);

        Type type = Type.valueOf(data.optString("type", "RAW").toUpperCase(Locale.ENGLISH));

        Format format;
        if (type == Type.RAW) {
            //avoids pulling a pixel print processor, the actual format will be used in impl
            format = Format.COMMAND;
        } else {
            format = Format.valueOf(data.optString("format", "IMAGE").toUpperCase(Locale.ENGLISH));
        }

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
            throw new IllegalArgumentException(String.format("Unable to find processor for %s type", type.name()));
        }
    }

    /**
     * Version 2.1 introduced the flavor attribute to apply better control on raw data.
     * Essentially format became flavor, type become format, and type was rewritten.
     * Though a few exceptions exist due to the way additional raw options used to be handled.
     * <p>
     * This method will take the data object, and if it uses any old terminology it will update the value to the new set.
     *
     * @param data JSONObject of printData, will update any values by reference
     */
    private static void convertVersion(JSONObject data) throws JSONException {
        if (!data.isNull("flavor")) { return; } //flavor exists only in new version, no need to convert

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
    public static String getPrinterId(String printerName) {
        if (CUPS_DESC == null || !CUPS_DESC.containsValue(printerName)) {
            CUPS_DESC = ShellUtilities.getCupsPrinters();
        }

        if (SystemUtilities.isMac()) {
            for(String name : CUPS_DESC.keySet()) {
                if (CUPS_DESC.get(name).equals(printerName)) {
                    return name;
                }
            }
            log.warn("Could not locate printerId matching {}", printerName);
        }
        return printerName;
    }

    public static PrinterResolution getNativeDensity(PrintService service) {
        if (service == null) { return null; }

        PrinterResolution pRes = (PrinterResolution)service.getDefaultAttributeValue(PrinterResolution.class);

        if (pRes == null && !SystemUtilities.isWindows()) {
            String printerId = getPrinterId(service.getName());

            if (CUPS_DPI == null || !CUPS_DPI.containsKey(printerId)) {
                CUPS_DPI = ShellUtilities.getCupsDensities(CUPS_DESC);
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
            driver = ShellUtilities.execute(new String[] {"lpstat", "-l", "-p", getPrinterId(service.getName())}, new String[] {"Interface:"});
            if (!driver.isEmpty()) {
                driver = ShellUtilities.execute(new String[] {"grep", "*PCFileName:", driver.substring(10).trim()}, new String[] {"*PCFileName:"});
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
        PrintProcessor processor = PrintingUtilities.getPrintProcessor(params.getJSONArray("data"));
        log.debug("Using {} to print", processor.getClass().getName());

        try {
            PrintOutput output = new PrintOutput(params.optJSONObject("printer"));
            PrintOptions options = new PrintOptions(params.optJSONObject("options"), output);

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
