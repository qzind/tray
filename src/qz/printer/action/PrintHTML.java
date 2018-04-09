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

import com.sun.javafx.print.PrintHelper;
import com.sun.javafx.print.Units;
import javafx.print.*;
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
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrintHTML extends PrintImage implements PrintProcessor {

    private static final Logger log = LoggerFactory.getLogger(PrintHTML.class);

    private List<WebAppModel> models;

    private JLabel legacyLabel = null;


    public PrintHTML() {
        super();
        models = new ArrayList<>();
    }

    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.HTML;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        try {
            WebApp.initialize();
            PrintOptions.Pixel pxlOpts = options.getPixelOptions();

            for(int i = 0; i < printData.length(); i++) {
                JSONObject data = printData.getJSONObject(i);
                String source = data.getString("data");

                PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.valueOf(data.optString("flavor", "FILE").toUpperCase(Locale.ENGLISH));

                double pageZoom = (pxlOpts.getDensity() * pxlOpts.getUnits().as1Inch()) / 72.0;
                if (pageZoom <= 1 || data.optBoolean("forceOriginal")) { pageZoom = 1; }

                double pageWidth = 0;
                double pageHeight = 0;

                if (pxlOpts.getSize() != null) {
                    pageWidth = pxlOpts.getSize().getWidth() * (72.0 / pxlOpts.getUnits().as1Inch());
                } else if (options.getDefaultOptions().getPageSize() != null) {
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

                models.add(new WebAppModel(source, (flavor != PrintingUtilities.Flavor.FILE), pageWidth, pageHeight, pxlOpts.isScaleContent(), pageZoom));
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
        } else if (options.getPixelOptions().isRasterize()) {
            //grab a snapshot of the pages for PrintImage instead of printing directly
            for(WebAppModel model : models) {
                try { images.add(WebApp.raster(model)); }
                catch(Throwable t) {
                    throw new PrinterException("Failed to take raster of web page, image size is too large");
                }
            }

            super.print(output, options);
        } else {

            Printer fxPrinter = null;
            for(Printer p : Printer.getAllPrinters()) {
                if (p.getName().equals(output.getPrintService().getName())) {
                    fxPrinter = p;
                    break;
                }
            }
            if (fxPrinter == null) {
                throw new PrinterException("Cannot find printer under the JavaFX libraries");
            }

            PrinterJob job = PrinterJob.createPrinterJob(fxPrinter);


            // apply option settings
            PrintOptions.Pixel pxlOpts = options.getPixelOptions();
            JobSettings settings = job.getJobSettings();
            settings.setJobName(pxlOpts.getJobName(Constants.HTML_PRINT));
            settings.setPrintQuality(PrintQuality.HIGH);

            if (pxlOpts.getColorType() != null) {
                settings.setPrintColor(pxlOpts.getColorType().getAsPrintColor());
            }
            if (pxlOpts.isDuplex()) {
                settings.setPrintSides(PrintSides.DUPLEX);
            }
            if (pxlOpts.getPrinterTray() != null) {
                fxPrinter.getPrinterAttributes().getSupportedPaperSources().stream()
                        .filter(source -> pxlOpts.getPrinterTray().equals(source.getName())).forEach(settings::setPaperSource);
            }

            if (pxlOpts.getDensity() > 0) {
                settings.setPrintResolution(PrintHelper.createPrintResolution((int)pxlOpts.getDensity(), (int)pxlOpts.getDensity()));
            }

            Paper paper;
            if (pxlOpts.getSize() != null) {
                double convert = 1;
                Units units = pxlOpts.getUnits().getAsUnits();
                if (units == null) {
                    convert = 10; //need to adjust from cm to mm only for DPCM sizes
                    units = Units.MM;
                }
                paper = PrintHelper.createPaper("Custom", pxlOpts.getSize().getWidth() * convert, pxlOpts.getSize().getHeight() * convert, units);
            } else {
                PrintOptions.Size paperSize = options.getDefaultOptions().getPageSize();
                paper = PrintHelper.createPaper("Default", paperSize.getWidth(), paperSize.getHeight(), Units.POINT);
            }

            PageOrientation orient = fxPrinter.getPrinterAttributes().getDefaultPageOrientation();
            if (pxlOpts.getOrientation() != null) {
                orient = pxlOpts.getOrientation().getAsPageOrient();
            }

            try {
                PageLayout layout;
                PrintOptions.Margins m = pxlOpts.getMargins();
                if (m != null) {
                    //force access to the page layout constructor as the adjusted margins on small sizes are wildly inaccurate
                    Constructor<PageLayout> plCon = PageLayout.class.getDeclaredConstructor(Paper.class, PageOrientation.class, double.class, double.class, double.class, double.class);
                    plCon.setAccessible(true);

                    //margins defined as pnt (1/72nds)
                    double asPnt = pxlOpts.getUnits().toInches() * 72;
                    layout = plCon.newInstance(paper, orient, m.left() * asPnt, m.right() * asPnt, m.top() * asPnt, m.bottom() * asPnt);
                } else {
                    //if margins are not provided, use default paper margins
                    PageLayout valid = fxPrinter.getDefaultPageLayout();
                    layout = fxPrinter.createPageLayout(paper, orient, valid.getLeftMargin(), valid.getRightMargin(), valid.getTopMargin(), valid.getBottomMargin());
                }

                //force our layout as the default to avoid default-margin exceptions on small paper sizes
                Field field = fxPrinter.getClass().getDeclaredField("defPageLayout");
                field.setAccessible(true);
                field.set(fxPrinter, layout);

                settings.setPageLayout(layout);
            }
            catch(Exception e) {
                log.error("Failed to set custom layout", e);
            }

            settings.setCopies(pxlOpts.getCopies());
            log.trace("{}", settings.toString());

            //javaFX lies about this value, so pull from original print service
            CopiesSupported cSupport = (CopiesSupported)output.getPrintService()
                    .getSupportedAttributeValues(Copies.class, output.getPrintService().getSupportedDocFlavors()[0], null);

            try {
                if (cSupport != null && cSupport.contains(pxlOpts.getCopies())) {
                    for(WebAppModel model : models) {
                        WebApp.print(job, model);
                    }
                } else {
                    settings.setCopies(1); //manually handle copies if they are not supported
                    for(int i = 0; i < pxlOpts.getCopies(); i++) {
                        for(WebAppModel model : models) {
                            WebApp.print(job, model);
                        }
                    }
                }
            }
            catch(Throwable t) {
                throw new PrinterException(t.getMessage());
            }

            //send pending prints
            job.endJob();
        }
    }

    private void printLegacy(PrintOutput output, PrintOptions options) throws PrinterException {
        PrintOptions.Pixel pxlOpts = options.getPixelOptions();

        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setPrintService(output.getPrintService());
        PageFormat page = job.getPageFormat(null);

        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, page, output.getSupportedMedia());

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
                    legacyLabel.setText(cleanHtmlContent(model.getSource()));
                } else {
                    try(InputStream fis = new URL(model.getSource()).openStream()) {
                        String webPage = cleanHtmlContent(IOUtils.toString(fis, "UTF-8"));
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

    private String cleanHtmlContent(String html) {
        return html.replaceAll("^[\\s\\S]*<(HTML|html)\\b.*?>", "<html>");
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
