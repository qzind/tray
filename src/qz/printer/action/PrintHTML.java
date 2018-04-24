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

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrintHTML extends PrintImage implements PrintProcessor, Printable {

    private static final Logger log = LoggerFactory.getLogger(PrintHTML.class);

    private List<WebAppModel> models;

    private JLabel legacyLabel = null;


    public PrintHTML() {
        super();
        models = new ArrayList<>();
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
                String source = data.getString("data");

                PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", "FILE").toUpperCase(Locale.ENGLISH));

                double pageZoom = (pxlOpts.getDensity() * pxlOpts.getUnits().as1Inch()) / 72.0;
                if (pageZoom <= 1) { pageZoom = 1; }

                double pageWidth = 0;
                double pageHeight = 0;

                // web dimension use 96dpi (or equivalent dp/metric)
                if (options.getDefaultOptions().getPageSize() != null) {
                    pageWidth = options.getDefaultOptions().getPageSize().getWidth();
                    //pageHeight = options.getDefaultOptions().getPageSize().getHeight();
                }

                if (!data.isNull("options")) {
                    JSONObject dataOpt = data.getJSONObject("options");

                    if (!dataOpt.isNull("pageWidth") && dataOpt.optDouble("pageWidth") > 0) {
                        pageWidth = dataOpt.optDouble("pageWidth") * (72.0 / pxlOpts.getUnits().as1Inch());
                    }
                    if (!dataOpt.isNull("pageHeight") && dataOpt.optDouble("pageHeight") > 0) {
                        pageHeight = dataOpt.optDouble("pageHeight") * (72.0 / pxlOpts.getUnits().as1Inch());
                    }
                }

                models.add(new WebAppModel(source, (format != PrintingUtilities.Format.FILE), pageWidth, pageHeight, pxlOpts.isScaleContent(), pageZoom));
            }

            log.debug("Parsed {} html records", models.size());
        }
        catch(IOException e) {
            throw new UnsupportedOperationException("Unable to start JavaFX service", e);
        }
        catch(NoClassDefFoundError e) {
            throw new UnsupportedOperationException("JavaFX libraries not found", e);
        }
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrinterException {
        if (options.getPixelOptions().isLegacy()) {
            printLegacy(output, options);
        } else {
            for(WebAppModel model : models) {
                try {
                    images.add(WebApp.capture(model));
                }
                catch(IllegalArgumentException | IOException e) {
                    //JavaFX image loader becomes null if webView is too large, throwing an IllegalArgumentException on screen capture attempt
                    if (e instanceof IllegalArgumentException || (e.getCause() != null && e.getCause() instanceof IllegalArgumentException)) {
                        try {
                            log.warn("HTML capture failed due to size, attempting at default zoom");
                            model.setZoom(1.0);
                            images.add(WebApp.capture(model));
                        }
                        catch(Throwable re) {
                            throw new UnsupportedOperationException("Image or Density is too large for HTML printing", re);
                        }
                    } else {
                        throw new PrinterException(e.getMessage());
                    }
                }
                catch(Throwable t) {
                    throw new UnsupportedOperationException("Failed to capture HTML", t);
                }
            }

            super.print(output, options);
        }
    }

    private void printLegacy(PrintOutput output, PrintOptions options) throws PrinterException {
        PrintOptions.Pixel pxlOpts = options.getPixelOptions();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(output.getPrintService());
        PageFormat page = job.getPageFormat(null);

        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, page);

        //setup swing ui
        JFrame legacyFrame = new JFrame(pxlOpts.getJobName(Constants.HTML_PRINT));
        legacyFrame.setUndecorated(true);
        legacyFrame.setLayout(new FlowLayout());
        legacyFrame.setExtendedState(Frame.ICONIFIED);

        legacyLabel = new JLabel();
        legacyLabel.setOpaque(true);
        legacyLabel.setBackground(Color.WHITE);
        legacyLabel.setBorder(null);
        legacyLabel.setDoubleBuffered(false);

        legacyFrame.add(legacyLabel);

        try {
            for(WebAppModel model : models) {
                if (model.isPlainText()) {
                    legacyLabel.setText(model.getSource());
                } else {
                    try(InputStream fis = new URL(model.getSource()).openStream()) {
                        String webPage = IOUtils.toString(fis, "UTF-8").replaceAll("^[\\s\\S]+<(HTML|html)\\b.*?>", "<html>");
                        legacyLabel.setText(webPage);
                    }
                }

                legacyFrame.pack();
                legacyFrame.setVisible(true);

                job.setPrintable(this);
                printCopies(output, pxlOpts, job, attributes);
            }
        }
        catch(Exception e) {
            throw new PrinterException(e.getMessage());
        }
        finally {
            legacyFrame.dispose();
        }
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (legacyLabel == null) {
            return super.print(graphics, pageFormat, pageIndex);
        } else {
            if (graphics == null) { throw new PrinterException("No graphics specified"); }
            if (pageFormat == null) { throw new PrinterException("No page format specified"); }

            if (pageIndex + 1 > models.size()) {
                return NO_SUCH_PAGE;
            }
            log.trace("Requested page {} for printing", pageIndex);

            Graphics2D graphics2D = super.withRenderHints((Graphics2D)graphics, interpolation);
            graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            graphics2D.scale(pageFormat.getImageableWidth() / pageFormat.getWidth(), pageFormat.getImageableHeight() / pageFormat.getHeight());
            legacyLabel.paint(graphics2D);

            return PAGE_EXISTS;
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();

        models.clear();
        legacyLabel = null;
    }

}
