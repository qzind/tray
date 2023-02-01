package qz.utils;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.ssl.Base64;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.PrintProcessor;
import qz.printer.action.ProcessorFactory;
import qz.ws.PrintSocketClient;

import java.awt.print.PrinterAbortException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public class PrintingUtilities {

    private static final Logger log = LogManager.getLogger(PrintingUtilities.class);

    private static GenericKeyedObjectPool<Format,PrintProcessor> processorPool;


    private PrintingUtilities() {}

    public enum Type {
        PIXEL, RAW
    }

    public enum Format {
        COMMAND, DIRECT, HTML, IMAGE, PDF
    }

    /**
     * TODO: Move this to a dedicated class
     */
    public enum Flavor {
        BASE64, FILE, HEX, PLAIN, XML;

        // TODO: Refactor DeviceUtilities to use optString("flavor") instead of optString("type")
        @Deprecated
        public static Flavor parse(String value, Flavor fallbackIfEmpty) {
            if(value != null && !value.isEmpty()) {
                return Flavor.valueOf(value.toUpperCase(Locale.ENGLISH));
            }
            return fallbackIfEmpty;
        }

        public static Flavor parse(JSONObject json, Flavor fallbackIfEmpty) {
            return parse(json.optString("flavor", ""), fallbackIfEmpty);
        }

        public String toString(byte[] bytes) {
            return ByteUtilities.toString(this, bytes);
        }

        public byte[] read(String data) throws IOException {
            return read(data, null);
        }

        public byte[] read(String data, String xmlTag) throws IOException {
            try {
                switch(this) {
                    case BASE64:
                        return Base64.decodeBase64(data);
                    case FILE:
                        return FileUtilities.readRawFile(data);
                    case HEX:
                        return ByteUtilities.hexStringToByteArray(data.trim());
                    case XML:
                            // Assume base64 encoded string inside the specified XML tag
                            return Base64.decodeBase64(FileUtilities.readXMLFile(data, xmlTag).getBytes(StandardCharsets.UTF_8));
                    case PLAIN:
                    default:
                        // Reading "plain" data is only supported through JSON/websocket, so we can safely assume it's always UTF8
                        return data.getBytes(StandardCharsets.UTF_8);
                }
            } catch(Exception e) {
                log.warn("An error occurred parsing data from " + this.name(), e);
                throw new IOException("Error parsing data from " + this.name());
            }
        }
    }


    public static Format getPrintFormat(JSONArray printData) throws JSONException {
        convertVersion(printData);

        //grab first data object to determine type for entire set
        JSONObject data = printData.optJSONObject(0);

        //Check for RAW type to coerce COMMAND format handling
        Type type;
        if (data == null) {
            type = Type.RAW;
        } else {
            type = Type.valueOf(data.optString("type", "RAW").toUpperCase(Locale.ENGLISH));
        }

        Format format;
        if (type == Type.RAW) {
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
                String format = data.getString("format").toUpperCase(Locale.ENGLISH);
                if (Arrays.asList("BASE64", "FILE", "HEX", "PLAIN", "XML").contains(format)) {
                    data.put("flavor", format);
                    data.remove("format");
                }
            }

            if (!data.isNull("type")) {
                String type = data.getString("type").toUpperCase(Locale.ENGLISH);
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
