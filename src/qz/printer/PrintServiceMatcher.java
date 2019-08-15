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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.standard.PrinterName;

public class PrintServiceMatcher {

    private static final Logger log = LoggerFactory.getLogger(PrintServiceMatcher.class);

    public static PrintService[] getPrintServices() {
        PrintService[] printers = PrintServiceLookup.lookupPrintServices(null, null);
        log.debug("Found {} printers", printers.length);

        return printers;
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
                break;
            }
            if (printerName.startsWith(printerSearch)) {
                begins = ps;
                continue;
            }
            if (printerName.contains(printerSearch)) {
                partial = ps;
                continue;
            }

            if (SystemUtilities.isMac()) {
                // 1.9 style printer names
                PrinterName name = ps.getAttribute(PrinterName.class);
                if (name == null) continue;
                printerName = name.getValue().toLowerCase();
                if (printerName.equals(printerSearch)) {
                    exact = ps;
                    continue;
                }
                if (printerName.startsWith(printerSearch)) {
                    begins = ps;
                    continue;
                }
                if (printerName.contains(printerSearch)) {
                    partial = ps;
                }
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

        PrintService[] printers = getPrintServices();
        for(PrintService ps : printers) {
            list.put(ps.getName());
        }

        return list;
    }

    public static String getPrinterJSON(String query) throws JSONException {
        PrintService service = PrintServiceMatcher.matchService(query);

        if (service != null) {
            return service.getName();
        } else {
            return null;
        }
    }

}
