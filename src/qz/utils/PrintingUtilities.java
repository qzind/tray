package qz.utils;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.printer.action.PrintProcessor;
import qz.printer.action.ProcessorFactory;

import javax.print.PrintService;
import javax.print.attribute.standard.PrinterResolution;
import java.util.HashMap;

public class PrintingUtilities {

    private static final Logger log = LoggerFactory.getLogger(PrintingUtilities.class);

    private static HashMap<String,String> CUPS_DESC; //name -> description
    private static HashMap<String,PrinterResolution> CUPS_DPI; //description -> default dpi

    private static GenericKeyedObjectPool<Type,PrintProcessor> processorPool;


    private PrintingUtilities() {}

    public enum Type {
        HTML, IMAGE, PDF, RAW
    }

    public enum Format {
        BASE64, FILE, IMAGE, PLAIN, HEX, XML
    }


    public synchronized static PrintProcessor getPrintProcessor(JSONArray printData) throws JSONException {
        JSONObject data = printData.optJSONObject(0);

        Type type;
        if (data == null) {
            type = Type.RAW;
        } else {
            type = Type.valueOf(data.optString("type", "RAW").toUpperCase());
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

            return processorPool.borrowObject(type);
        }
        catch(Exception e) {
            throw new IllegalArgumentException(String.format("Unable to find processor for %s type", type.name()));
        }
    }

    public static void releasePrintProcessor(PrintProcessor processor) {
        try {
            processorPool.returnObject(processor.getType(), processor);
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

}
