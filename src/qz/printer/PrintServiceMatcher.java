/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.printer;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.info.NativePrinter;
import qz.printer.info.NativePrinterMap;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import java.util.Locale;

public class PrintServiceMatcher {
    private static final Logger log = LoggerFactory.getLogger(PrintServiceMatcher.class);

    public static NativePrinterMap getNativePrinterList() {
        NativePrinterMap printers = NativePrinterMap.getInstance();
        printers.putAll(PrintServiceLookup.lookupPrintServices(null, null));
        log.debug("Found {} printers", printers.size());
        //Todo Remove this debugging log
        log.warn("jfx print service num {}", PrintServiceLookup.lookupPrintServices(null, null).length);
        return printers;
    }

    public static String findPrinterName(String query) throws JSONException {
        NativePrinter printer = PrintServiceMatcher.matchPrinter(query);

        if (printer != null) {
            return printer.getPrintService().value().getName();
        } else {
            return null;
        }
    }

    /**
     * Finds {@code PrintService} by looking at any matches to {@code printerSearch}.
     *
     * @param printerSearch Search query to compare against service names.
     */
    public static NativePrinter matchPrinter(String printerSearch) {
        NativePrinter exact = null;
        NativePrinter begins = null;
        NativePrinter partial = null;

        log.debug("Searching for PrintService matching {}", printerSearch);
        printerSearch = printerSearch.toLowerCase(Locale.ENGLISH);

        // Search services for matches
        for(NativePrinter printer : getNativePrinterList().values()) {
            if (printer.getName() == null) {
                continue;
            }
            String printerName = printer.getName().toLowerCase(Locale.ENGLISH);
            if (printerName.equals(printerSearch)) {
                exact = printer;
                break;
            }
            if (printerName.startsWith(printerSearch)) {
                begins = printer;
                continue;
            }
            if (printerName.contains(printerSearch)) {
                partial = printer;
                continue;
            }

            if (SystemUtilities.isMac()) {
                // 1.9 compat: fallback for old style names
                PrinterName name = printer.getLegacyName();
                if (name == null || name.getValue() == null) { continue; }
                printerName = name.getValue().toLowerCase(Locale.ENGLISH);
                if (printerName.equals(printerSearch)) {
                    exact = printer;
                    continue;
                }
                if (printerName.startsWith(printerSearch)) {
                    begins = printer;
                    continue;
                }
                if (printerName.contains(printerSearch)) {
                    partial = printer;
                }
            }
        }

        // Return closest match
        NativePrinter use = null;
        if (exact != null) {
            use = exact;
        } else if (begins != null) {
            use = begins;
        } else if (partial != null) {
            use = partial;
        }

        if (use != null) {
            log.debug("Found match: {}", use.getPrintService().value().getName());
        } else {
            log.warn("Printer not found: {}", printerSearch);
        }

        return use;
    }


    public static JSONArray getPrintersJSON() throws JSONException {
        JSONArray list = new JSONArray();

        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

        for(NativePrinter printer : getNativePrinterList().values()) {
            PrintService ps = printer.getPrintService().value();
            JSONObject jsonService = new JSONObject();
            jsonService.put("name", ps.getName());
            jsonService.put("driver", printer.getDriver().value());
            jsonService.put("connection", printer.getConnection());
            jsonService.put("default", ps == defaultService);

            for(Media m : (Media[])ps.getSupportedAttributeValues(Media.class, null, null)) {
                if (m.toString().contains("Tray")) { jsonService.accumulate("trays", m.toString()); }
            }

            PrinterResolution res = printer.getResolution().value();
            int density = -1; if (res != null) { density = res.getFeedResolution(ResolutionSyntax.DPI); }
            jsonService.put("density", density);

            list.put(jsonService);
        }

        return list;
    }

}
