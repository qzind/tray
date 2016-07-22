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
import qz.utils.PrintingUtilities;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.PrinterResolution;

public class PrintServiceMatcher {

    private static final Logger log = LoggerFactory.getLogger(PrintServiceMatcher.class);

    public static PrintService[] getPrintServices() {
        PrintService[] printers = PrintServiceLookup.lookupPrintServices(null, null);
        log.debug("Found {} printers", printers.length);

        return printers;
    }

    public static String findPrinterName(String query) throws JSONException {
        PrintService service = PrintServiceMatcher.matchService(query);

        if (service != null) {
            return service.getName();
        } else {
            return null;
        }
    }

    /**
     * Finds {@code PrintService} by looking at any matches to {@code printerSearch}.
     *
     * @param printerSearch Search query to compare against service names.
     */
    public static PrintService matchService(String printerSearch) {
        PrintService exact = null;
        PrintService begins = null;
        PrintService partial = null;

        log.debug("Searching for PrintService matching {}", printerSearch);
        printerSearch = printerSearch.toLowerCase();

        // Search services for matches
        PrintService[] printers = getPrintServices();
        for(PrintService ps : printers) {
            String printerName = ps.getName().toLowerCase();

            if (printerName.equals(printerSearch)) {
                exact = ps;
            }
            if (printerName.startsWith(printerSearch)) {
                begins = ps;
            }
            if (printerName.contains(printerSearch)) {
                partial = ps;
            }
        }

        // Return closest match
        PrintService use = null;
        if (exact != null) {
            use = exact;
        } else if (begins != null) {
            use = begins;
        } else if (partial != null) {
            use = partial;
        }

        if (use != null) {
            log.debug("Found match: {}", use.getName());
        } else {
            log.warn("Printer not found: {}", printerSearch);
        }

        return use;
    }


    public static JSONArray getPrintersJSON() throws JSONException {
        JSONArray list = new JSONArray();

        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

        PrintService[] printers = getPrintServices();
        for(PrintService ps : printers) {
            JSONObject jsonService = new JSONObject();
            jsonService.put("name", ps.getName());
            jsonService.put("driver", PrintingUtilities.getDriver(ps));
            jsonService.put("default", ps == defaultService);

            int trays = 0;
            for(Media m : (Media[])ps.getSupportedAttributeValues(Media.class, null, null)) {
                if (m.toString().trim().startsWith("Tray")) { trays++; }
            }
            jsonService.put("trays", trays);

            PrinterResolution res = PrintingUtilities.getNativeDensity(ps);
            int density = -1; if (res != null) { density = res.getFeedResolution(ResolutionSyntax.DPI); }
            jsonService.put("density", density);

            list.put(jsonService);
        }

        return list;
    }

}
