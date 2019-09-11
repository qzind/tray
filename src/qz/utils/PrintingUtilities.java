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

import java.awt.print.PrinterAbortException;
import java.util.*;

public class PrintingUtilities {

    private static final Logger log = LoggerFactory.getLogger(PrintingUtilities.class);

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
