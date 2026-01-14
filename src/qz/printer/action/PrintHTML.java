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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.html.WebApp;
import qz.printer.action.html.WebAppModel;
import qz.printer.action.raw.LanguageType;
import qz.utils.ByteUtilities;
import qz.utils.PrintingUtilities;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Sides;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrintHTML extends PrintImage implements PrintProcessor {

    private static final Logger log = LogManager.getLogger(PrintHTML.class);

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
            PrintOptions.Pixel pxlOpts = options.getPixelOptions();
            if (!pxlOpts.isLegacy()) {
                WebApp.initialize();
            }

            for(int i = 0; i < printData.length(); i++) {
                JSONObject data = printData.getJSONObject(i);

                PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.parse(data, PrintingUtilities.Flavor.FILE);

                String source = loadHtml(data.getString("data"), flavor, null);

                double pageZoom = (pxlOpts.getDensity() * pxlOpts.getUnits().as1Inch()) / 72.0;
                if (pageZoom <= 1) { pageZoom = 1; }

                double pageWidth = 0;
                double pageHeight = 0;
                double convertFactor = (72.0 / pxlOpts.getUnits().as1Inch());

                boolean renderFromHeight = Arrays.asList(PrintOptions.Orientation.LANDSCAPE,
                                                         PrintOptions.Orientation.REVERSE_LANDSCAPE).contains(pxlOpts.getOrientation());

                if (pxlOpts.getSize() != null) {
                    if (!renderFromHeight) {
                        pageWidth = pxlOpts.getSize().getWidth() * convertFactor;
                    } else {
                        pageWidth = pxlOpts.getSize().getHeight() * convertFactor;
                    }
                } else if (options.getDefaultOptions().getPageSize() != null) {
                    if (!renderFromHeight) {
                        pageWidth = options.getDefaultOptions().getPageSize().getWidth();
                    } else {
                        pageWidth = options.getDefaultOptions().getPageSize().getHeight();
                    }
                }

                if (pxlOpts.getMargins() != null) {
                    PrintOptions.Margins margins = pxlOpts.getMargins();
                    if (!renderFromHeight || pxlOpts.isRasterize()) {
                        pageWidth -= (margins.left() + margins.right()) * convertFactor;
                    } else {
                        pageWidth -= (margins.top() + margins.bottom()) * convertFactor; //due to vector margin matching
                    }
                }

                if (!data.isNull("options")) {
                    JSONObject dataOpt = data.getJSONObject("options");

                    if (!dataOpt.isNull("pageWidth") && dataOpt.optDouble("pageWidth") > 0) {
                        pageWidth = dataOpt.optDouble("pageWidth") * convertFactor;
                    }
                    if (!dataOpt.isNull("pageHeight") && dataOpt.optDouble("pageHeight") > 0) {
                        pageHeight = dataOpt.optDouble("pageHeight") * convertFactor;
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

    /**
     * Loads the HTML data into one large String from various input formats except in the case of a URL (FILE)
     * which will be loaded directly by the HTML engine.
     */
    private String loadHtml(String data, PrintingUtilities.Flavor flavor, Charset srcEncoding) throws IOException {
        switch(flavor) {
            case FILE:
            case PLAIN:
                // We'll toggle between 'plain' and 'file' when we construct WebAppModel
                return data;
            default:
                // Note: srcEncoding is only available in raw
                return new String(ByteUtilities.seekConversion(flavor.read(data), srcEncoding, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
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
                    if (model.getZoom() > 1 && t instanceof IllegalArgumentException) {
                        //probably a unrecognized image loader error, try at default zoom
                        try {
                            log.warn("Capture failed with increased zoom, attempting with default value");
                            model.setZoom(1);
                            images.add(WebApp.raster(model));
                        }
                        catch(Throwable tt) {
                            throw new PrinterException(tt.getMessage());
                        }
                    } else {
                        throw new PrinterException(t.getMessage());
                    }
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

            // If colortype is default, leave printColor blank. The system's printer settings will be used instead.
            if (pxlOpts.getColorType() != PrintOptions.ColorType.DEFAULT) {
                settings.setPrintColor(getColor(pxlOpts));
            }
            if (pxlOpts.getDuplex() == Sides.DUPLEX || pxlOpts.getDuplex() == Sides.TWO_SIDED_LONG_EDGE) {
                settings.setPrintSides(PrintSides.DUPLEX);
            }
            if (pxlOpts.getDuplex() == Sides.TUMBLE || pxlOpts.getDuplex() == Sides.TWO_SIDED_SHORT_EDGE) {
                settings.setPrintSides(PrintSides.TUMBLE);
            }
            if (pxlOpts.getPrinterTray() != null) {
                PaperSource tray = findFXTray(fxPrinter.getPrinterAttributes().getSupportedPaperSources(), pxlOpts.getPrinterTray());
                if (tray != null) {
                    settings.setPaperSource(tray);
                }
            }

            if (pxlOpts.getDensity() > 0) {
                settings.setPrintResolution(PrintHelper.createPrintResolution((int)pxlOpts.getDensity(), (int)pxlOpts.getDensity()));
            }

            Paper paper;
            if (pxlOpts.getSize() != null && pxlOpts.getSize().getWidth() > 0 && pxlOpts.getSize().getHeight() > 0) {
                double convert = 1;
                Units units = getUnits(pxlOpts);
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
                orient = getOrientation(pxlOpts);
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
                    if (orient == PageOrientation.PORTRAIT || orient == PageOrientation.REVERSE_PORTRAIT) {
                        layout = plCon.newInstance(paper, orient, m.left() * asPnt, m.right() * asPnt, m.top() * asPnt, m.bottom() * asPnt);
                    } else {
                        //rotate margins to match raster prints
                        layout = plCon.newInstance(paper, orient, m.top() * asPnt, m.bottom() * asPnt, m.right() * asPnt, m.left() * asPnt);
                    }
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
                job.cancelJob();
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

            Graphics2D graphics2D = (Graphics2D)graphics;
            graphics2D.setRenderingHints(buildRenderingHints(dithering, interpolation));
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

    public static Units getUnits(PrintOptions.Pixel opts) {
        switch(opts.getUnits()) {
            case INCH:
                return Units.INCH;
            case MM:
                return Units.MM;
            default:
                return null;
        }
    }

    public static PageOrientation getOrientation(PrintOptions.Pixel opts) {
        switch(opts.getOrientation()) {
            case LANDSCAPE:
                return PageOrientation.LANDSCAPE;
            case REVERSE_LANDSCAPE:
                return PageOrientation.REVERSE_LANDSCAPE;
            case REVERSE_PORTRAIT:
                return PageOrientation.REVERSE_PORTRAIT;
            default:
                return PageOrientation.PORTRAIT;
        }
    }

    public static PrintColor getColor(PrintOptions.Pixel opts) {
        switch(opts.getColorType()) {
            case COLOR:
                return PrintColor.COLOR;
            default:
                return PrintColor.MONOCHROME;
        }
    }

    /**
     * Creates a raw-compatible BufferedImage
     */
    @Override
    public BufferedImage createBufferedImage(String data, JSONObject opt, PrintingUtilities.Flavor flavor, PrintOptions.Raw rawOpts, PrintOptions.Pixel pxlOpts) throws IOException {
        double density = (pxlOpts.getDensity() * pxlOpts.getUnits().as1Inch());
        if (density <= 1) {
            density = LanguageType.parse(opt.optString("language")).getDefaultDensity();
        }
        double pageZoom = density / 72.0;

        double pageWidth = opt.optInt("pageWidth") / density * 72;
        double pageHeight = opt.optInt("pageHeight") / density * 72;

        BufferedImage bi;
        data = loadHtml(data, flavor, rawOpts.getSrcEncoding());
        WebAppModel model = new WebAppModel(data, (flavor != PrintingUtilities.Flavor.FILE), pageWidth, pageHeight, false, pageZoom);

        try {
            WebApp.initialize(); //starts if not already started
            bi = WebApp.raster(model);

            // down scale back from web density
            double scaleFactor = opt.optDouble("pageWidth", 0) / bi.getWidth();
            BufferedImage scaled = new BufferedImage((int)(bi.getWidth() * scaleFactor), (int)(bi.getHeight() * scaleFactor), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.drawImage(bi, 0, 0, (int)(bi.getWidth() * scaleFactor), (int)(bi.getHeight() * scaleFactor), null);
            g2d.dispose();
            bi = scaled;
        }
        catch(Throwable t) {
            if (model.getZoom() > 1 && t instanceof IllegalArgumentException) {
                //probably a unrecognized image loader error, try at default zoom
                try {
                    log.warn("Capture failed with increased zoom, attempting with default value");
                    model.setZoom(1);
                    bi = WebApp.raster(model);
                }
                catch(Throwable tt) {
                    log.error("Failed to capture html raster");
                    throw new IOException(tt);
                }
            } else {
                log.error("Failed to capture html raster");
                throw new IOException(t);
            }
        }

        return bi;
    }
}
