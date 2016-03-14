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
import java.awt.print.PrinterJob;
import java.io.IOException;

public class PrintHTML extends PrintImage implements PrintProcessor, Printable {

    private static final Logger log = LoggerFactory.getLogger(PrintHTML.class);


    public PrintHTML() {
        super();
    }


    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        try {
            WebApp.initialize();

            for(int i = 0; i < printData.length(); i++) {
                JSONObject data = printData.getJSONObject(i);
                String source = data.getString("data");

                PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", "FILE").toUpperCase());

                double pageWidth = PrinterJob.getPrinterJob().getPageFormat(null).getWidth();
                if (!data.isNull("options")) {
                    pageWidth = data.optJSONObject("options").optDouble("pageWidth", pageWidth);
                }

                double pageZoom = options.getPixelOptions().getDensity() / 72.0;
                if (pageZoom <= 0) { pageZoom = 1; }

                try {
                    images.add(WebApp.capture(source, (format == PrintingUtilities.Format.FILE), pageWidth, pageZoom));
                }
                catch(IOException e) {
                    //JavaFX image loader becomes null if webView is too large, throwing an IllegalArgumentException on screen capture attempt
                    if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
                        throw new UnsupportedOperationException("Image or Density is too large for HTML printing", e);
                    }

                    throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as HTML", format, source), e);
                }
            }

            log.debug("Parsed {} html records", images.size());
        }
        catch(NoClassDefFoundError e) {
            throw new UnsupportedOperationException("JavaFX libraries not found", e);
        }
    }

}
