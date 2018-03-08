/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.printer.action;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.PrintOptions;
import qz.utils.PrintingUtilities;

import java.awt.print.Printable;
import java.io.IOException;
import java.util.Locale;

public class PrintHTML extends PrintImage implements PrintProcessor, Printable {

    private static final Logger log = LoggerFactory.getLogger(PrintHTML.class);


    public PrintHTML() {
        super();
    }

    @Override
    public PrintingUtilities.Type getType() {
        return PrintingUtilities.Type.HTML;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        try {
            WebApp.initialize();
            PrintOptions.Pixel pxlOpts = options.getPixelOptions();

            for(int i = 0; i < printData.length(); i++) {
                JSONObject data = printData.getJSONObject(i);
                if (data.optBoolean("processed")) { continue; } //in cases of failed captures on multi-pages
                String source = data.getString("data");

                PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", "FILE").toUpperCase(Locale.ENGLISH));

                double pageZoom = (pxlOpts.getDensity() * pxlOpts.getUnits().as1Inch()) / 72.0;
                if (pageZoom <= 1 || data.optBoolean("forceOriginal")) { pageZoom = 1; }

                double pageWidth = 0;
                double pageHeight = 0;

                // web dimension use 96dpi (or equivalent dp/metric)
                if (options.getDefaultOptions().getPageSize() != null) {
                    pageWidth = options.getDefaultOptions().getPageSize().getWidth() * (96.0 / 72.0);
                    //pageHeight = options.getDefaultOptions().getPageSize().getHeight() * (96.0 / 72.0);
                }
                if (!data.isNull("options")) {
                    JSONObject dataOpt = data.getJSONObject("options");
                    if (!dataOpt.isNull("pageWidth")) {
                        pageWidth = data.optJSONObject("options").getDouble("pageWidth") * (96.0 / pxlOpts.getUnits().as1Inch());
                    }
                    if (!dataOpt.isNull("pageHeight")) {
                        pageHeight = data.optJSONObject("options").getDouble("pageHeight") * (96.0 / pxlOpts.getUnits().as1Inch());
                    }
                }

                try {
                    images.add(WebApp.capture(source, (format == PrintingUtilities.Format.FILE), pageWidth, pageHeight, pageZoom));
                    data.put("processed", true);
                }
                catch(IOException e) {
                    //JavaFX image loader becomes null if webView is too large, throwing an IllegalArgumentException on screen capture attempt
                    if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                        if (data.optBoolean("forceOriginal")) {
                            throw new UnsupportedOperationException("Image or Density is too large for HTML printing", e);
                        } else {
                            log.warn("HTML capture failed due to size, attempting at default zoom");
                            data.put("forceOriginal", true);
                            parseData(printData, options);
                            return;
                        }
                    }

                    throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as HTML", format, source), e);
                }
                catch(Throwable t) {
                    throw new UnsupportedOperationException("Failed to capture HTML", t);
                }
            }

            log.debug("Parsed {} html records", images.size());
        }
        catch(IOException e) {
            throw new UnsupportedOperationException("Unable to start JavaFX service", e);
        }
        catch(NoClassDefFoundError e) {
            throw new UnsupportedOperationException("JavaFX libraries not found", e);
        }
    }

}
