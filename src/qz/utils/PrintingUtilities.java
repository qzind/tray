package qz.utils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.*;
import qz.ws.PrintSocketClient;

import javax.print.PrintService;
import javax.print.attribute.standard.PrinterResolution;
import java.awt.print.PrinterAbortException;
import java.util.HashMap;

public class PrintingUtilities {

    private static final Logger log = LoggerFactory.getLogger(PrintingUtilities.class);

    private static HashMap<String,String> CUPS_DESC; //name -> description
    private static HashMap<String,PrinterResolution> CUPS_DPI; //description -> default dpi


    private PrintingUtilities() {}

    public enum Type {
        HTML, IMAGE, PDF, RAW
    }

    public enum Format {
        BASE64, FILE, IMAGE, PLAIN, HEX, XML
    }


    public static PrintProcessor getPrintProcessor(JSONArray printData) throws JSONException {
        JSONObject data = printData.optJSONObject(0);

        Type type;
        if (data == null) {
            type = Type.RAW;
        } else {
            type = Type.valueOf(data.optString("type", "RAW").toUpperCase());
        }

        switch(type) {
            case HTML:
                return new PrintHTML();
            case IMAGE: default:
                return new PrintImage();
            case PDF:
                return new PrintPDF();
            case RAW:
                return new PrintRaw();
        }
    }

    /**
     * Gets the printerId for use with CUPS commands
     * @param printerName
     * @return Id of the printer for use with CUPS commands
     */
    public static String getPrinterId(String printerName) {
        if (!SystemUtilities.isMac()) {
            return printerName;
        }

        if (CUPS_DESC == null || !CUPS_DESC.containsValue(printerName)) {
            CUPS_DESC = ShellUtilities.getCupsPrinters();
        }

        for(String name : CUPS_DESC.keySet()) {
            if (CUPS_DESC.get(name).equals(printerName)) {
                return name;
            }
        }

        log.warn("Could not locate printerId matching {}", printerName);
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


    /**
     * Determine print variables and send data to printer
     *
     * @param session WebSocket session
     * @param UID     ID of call from web API
     * @param params  Params of call from web API
     */
    public static void processPrintRequest(Session session, String UID, JSONObject params) {
        try {
            PrintOutput output = new PrintOutput(params.optJSONObject("printer"));
            PrintOptions options = new PrintOptions(params.optJSONObject("options"), output);

            PrintProcessor processor = PrintingUtilities.getPrintProcessor(params.getJSONArray("data"));
            log.debug("Using {} to print", processor.getClass().getName());

            processor.parseData(params.optJSONArray("data"), options);
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
    }

}
