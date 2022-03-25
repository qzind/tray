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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.printer.info.NativePrinter;
import qz.printer.info.NativePrinterMap;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import java.lang.reflect.Method;
import java.util.Locale;

public class PrintServiceMatcher {
    private static final Logger log = LogManager.getLogger(PrintServiceMatcher.class);

    public static NativePrinterMap getNativePrinterList(boolean silent, boolean withAttributes) {
        NativePrinterMap printers = NativePrinterMap.getInstance();
        printers.putAll(PrintServiceLookup.lookupPrintServices(null, null));
        if (withAttributes) { printers.values().forEach(NativePrinter::getDriverAttributes); }
        if (!silent) { log.debug("Found {} printers", printers.size()); }
        return printers;
    }

    public static NativePrinterMap getNativePrinterList(boolean silent) {
        return getNativePrinterList(silent, false);
    }

    public static NativePrinterMap getNativePrinterList() {
        return getNativePrinterList(false);
    }

    public static NativePrinter getDefaultPrinter() {
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

        NativePrinterMap printers = NativePrinterMap.getInstance();
        if (!printers.contains(defaultService)) {
            printers.putAll(defaultService);
        }

        return printers.get(defaultService);
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
    public static NativePrinter matchPrinter(String printerSearch, boolean silent) {
        NativePrinter exact = null;
        NativePrinter begins = null;
        NativePrinter partial = null;

        if (!silent) { log.debug("Searching for PrintService matching {}", printerSearch); }

        NativePrinter defaultPrinter = getDefaultPrinter();
        if (defaultPrinter != null && printerSearch.equals(defaultPrinter.getName())) {
            if (!silent) { log.debug("Matched default printer, skipping further search"); }
            return defaultPrinter;
        }

        printerSearch = printerSearch.toLowerCase(Locale.ENGLISH);

        // Search services for matches
        for(NativePrinter printer : getNativePrinterList(silent).values()) {
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
            if(!silent) log.debug("Found match: {}", use.getPrintService().value().getName());
        } else {
            log.warn("Printer not found: {}", printerSearch);
        }

        return use;
    }

    public static NativePrinter matchPrinter(String printerSearch) {
        return matchPrinter(printerSearch, false);
    }

    public static JSONArray getPrintersJSON(boolean includeDetails) throws JSONException {
        JSONArray list = new JSONArray();

        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();

        boolean mediaTrayCrawled = false;

        for(NativePrinter printer : getNativePrinterList().values()) {
            PrintService ps = printer.getPrintService().value();
            JSONObject jsonService = new JSONObject();
            jsonService.put("name", ps.getName());

            if (includeDetails) {
                jsonService.put("driver", printer.getDriver().value());
                jsonService.put("connection", printer.getConnection());
                jsonService.put("default", ps == defaultService);

                if (!mediaTrayCrawled) {
                    log.info("Gathering printer MediaTray information...");
                    mediaTrayCrawled = true;
                }

                // HP and Ricoh have buggy drivers, remove bad trays
                boolean togglePcl6Quirks = false;
                if(SystemUtilities.isWindows()) {
                    if (driverStartsWithWord(printer,"HP", "Ricoh") && driverMatchesWord(printer,"PCL6", "PCL 6")) {
                        togglePcl6Quirks = true;
                    }
                }
                for(Media m : (Media[])ps.getSupportedAttributeValues(Media.class, null, null)) {
                    if (m instanceof MediaTray) { jsonService.accumulate("trays", m.toString()); }
                    if (m instanceof MediaTray) {
                        if(togglePcl6Quirks && isGoodPcl6Tray(printer, (MediaTray)m)) {
                            jsonService.accumulate("trays", m.toString());
                        }
                    }
                }

                PrinterResolution res = printer.getResolution().value();
                int density = -1; if (res != null) { density = res.getFeedResolution(ResolutionSyntax.DPI); }
                jsonService.put("density", density);
            }

            list.put(jsonService);
        }

        return list;
    }

    /**
     * Returns true if the underlying tray id is less than 1000
     * This should only be used in combination with known-buggy PCL6 drivers, such as HP, Ricoh
     */
    private static boolean isGoodPcl6Tray(NativePrinter printer, MediaTray mediaTray) {
        PrintService ps = printer.getPrintService().value();
        try {
            Method m = ps.getClass().getMethod("findTrayID", MediaTray.class);
            Object o = m.invoke(ps, new Object[]{mediaTray});
            if (o instanceof Integer && (Integer)o < 1000) {
                return true;
            }
            log.debug("Found suspected bad PCL6 printer tray \"{}\" from printer \"{}\"', ignoring", mediaTray, ps.getName());
            return false;
        }
        catch(ReflectiveOperationException ignore) {
            return true;
        }
    }

    /**
     * Simple pattern match to try to identify a driver starts with a particular word, usually the vendor name
     */
    private static boolean driverStartsWithWord(NativePrinter printer, String ... words) {
        String driver = printer.getDriver().value();
        for(String word : words) {
            if (driver != null && word != null) {
                if (driver.toLowerCase(Locale.ENGLISH).matches("^" + word.toLowerCase() + "\\b.*")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Simple pattern match to try to identify a driver has a particular word in it, e.g. "PCL6"
     */
    private static boolean driverMatchesWord(NativePrinter printer, String ... words) {
        String driver = printer.getDriver().value();
        for(String word : words) {
            if (driver != null && word != null) {
                if(driver.toLowerCase(Locale.ENGLISH).matches(".*\\b" + word.toLowerCase(Locale.ENGLISH) + "\\b.*")) {
                    return true;
                }
            }
        }
        return false;
    }

}
