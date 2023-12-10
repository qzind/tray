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
import qz.printer.info.CachedPrintServiceLookup;
import qz.printer.info.NativePrinter;
import qz.printer.info.NativePrinterMap;
import qz.utils.SystemUtilities;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.*;
import java.util.*;

public class PrintServiceMatcher {
    private static final Logger log = LogManager.getLogger(PrintServiceMatcher.class);
    private static boolean mediaTrayMessageShown = false;
    private static boolean printerNamesShown = false;

    // PrintService is slow in CUPS, use a cache instead per JDK-7001133
    // TODO: Include JDK version test for caching when JDK-7001133 is fixed upstream
    private static final boolean useCache = SystemUtilities.isUnix();

    public static NativePrinterMap getNativePrinterList(boolean silent, boolean withAttributes) {
        NativePrinterMap printers = NativePrinterMap.getInstance();
        printers.putAll(true, lookupPrintServices());
        if (withAttributes) { printers.values().forEach(NativePrinter::getDriverAttributes); }
        if (!silent) { log.debug("Found {} printers", printers.size()); }
        return printers;
    }

    private static PrintService[] lookupPrintServices() {
        return useCache ? CachedPrintServiceLookup.lookupPrintServices() :
                PrintServiceLookup.lookupPrintServices(null, null);
    }

    private static PrintService lookupDefaultPrintService() {
        return useCache ? CachedPrintServiceLookup.lookupDefaultPrintService() :
                PrintServiceLookup.lookupDefaultPrintService();
    }

    public static NativePrinterMap getNativePrinterList(boolean silent) {
        return getNativePrinterList(silent, false);
    }

    public static NativePrinterMap getNativePrinterList() {
        return getNativePrinterList(false);
    }

    public static NativePrinter getDefaultPrinter() {
        PrintService defaultService = lookupDefaultPrintService();

        if(defaultService == null) {
            return null;
        }

        NativePrinterMap printers = NativePrinterMap.getInstance();
        if (!printers.contains(defaultService)) {
            printers.putAll(false, defaultService);
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

        // Fix for https://github.com/qzind/tray/issues/931
        // This is more than an optimization, removal will lead to a regression
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

        PrintService defaultService = lookupDefaultPrintService();

        for(NativePrinter printer : getNativePrinterList().values()) {
            PrintService ps = printer.getPrintService().value();
            JSONObject jsonService = new JSONObject();
            jsonService.put("name", ps.getName());

            if (includeDetails) {
                jsonService.put("driver", printer.getDriver().value());
                jsonService.put("connection", printer.getConnection());
                jsonService.put("default", ps == defaultService);

                if (!mediaTrayMessageShown) {
                    log.info("Gathering printer MediaTray information...");
                    mediaTrayMessageShown = true;
                }

                // Drivers have a tendency to crash while looping over details, echo the printer name for troubleshooting
                if(!printerNamesShown) {
                    log.info("Gathering MediaTray information for {}...", jsonService.get("name"));
                }

                HashSet<String> uniqueSizes = new HashSet<>(); // prevents duplicates
                JSONArray trays = new JSONArray();
                JSONArray sizes = new JSONArray();

                for(Media m : (Media[])ps.getSupportedAttributeValues(Media.class, null, null)) {
                    if (m instanceof MediaTray) { trays.put(m.toString()); }
                    if (m instanceof MediaSizeName) {
                        if(uniqueSizes.add(m.toString())) {
                            MediaSize mediaSize = MediaSize.getMediaSizeForName((MediaSizeName)m);
                            if(mediaSize == null) {
                                continue;
                            }

                            JSONObject size = new JSONObject();
                            size.put("name", m.toString());

                            JSONObject in = new JSONObject();
                            in.put("width", mediaSize.getX(MediaPrintableArea.INCH));
                            in.put("height", mediaSize.getY(MediaPrintableArea.INCH));
                            size.put("in", in);

                            JSONObject mm = new JSONObject();
                            mm.put("width", mediaSize.getX(MediaPrintableArea.MM));
                            mm.put("height", mediaSize.getY(MediaPrintableArea.MM));
                            size.put("mm", mm);

                            sizes.put(size);
                        }

                    }
                }

                if(trays.length() > 0) {
                    jsonService.put("trays", trays);
                }
                if(sizes.length() > 0) {
                    jsonService.put("sizes", sizes);
                }

                PrinterResolution res = printer.getResolution().value();
                int density = -1; if (res != null) { density = res.getFeedResolution(ResolutionSyntax.DPI); }
                jsonService.put("density", density);
            }

            list.put(jsonService);
        }

        if(includeDetails) {
            printerNamesShown = true;
        }

        return list;
    }

}
