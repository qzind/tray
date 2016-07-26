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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.print.PrintException;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrintHTML implements PrintProcessor {

    private static final Logger log = LoggerFactory.getLogger(PrintHTML.class);

    private List<WebAppModel> models;

    public PrintHTML() {
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

            for(int i = 0; i < printData.length(); i++) {
                JSONObject data = printData.getJSONObject(i);
                String source = data.getString("data");

                PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", "FILE").toUpperCase(Locale.ENGLISH));

                double pageWidth = 0;
                double pageHeight = 0;

                if (options.getDefaultOptions().getPageSize() != null) {
                    pageWidth = options.getDefaultOptions().getPageSize().getWidth();
                }

                PrintOptions.Pixel pxlOpts = options.getPixelOptions();
                if (!data.isNull("options")) {
                    JSONObject dataOpt = data.getJSONObject("options");

                    if (!dataOpt.isNull("pageWidth")) {
                        pageWidth = data.optJSONObject("options").getDouble("pageWidth") * (72.0 / pxlOpts.getUnits().as1Inch());
                    }
                    if (!dataOpt.isNull("pageHeight")) {
                        pageHeight = data.optJSONObject("options").getDouble("pageHeight") * (72.0 / pxlOpts.getUnits().as1Inch());
                    }
                }

                models.add(new WebAppModel(source, (format != PrintingUtilities.Format.FILE), pageWidth, pageHeight, pxlOpts.isScaleContent()));
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
    public void print(PrintOutput output, PrintOptions options) throws PrintException, PrinterException {
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
        settings.setPrintQuality(PrintQuality.NORMAL);

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

        Paper paper = fxPrinter.getPrinterAttributes().getDefaultPaper();
        if (pxlOpts.getSize() != null) {
            double convert = 1;
            Units units = pxlOpts.getUnits().getAsUnits();
            if (units == null) {
                convert = 10; //need to adjust from cm to mm only for DPCM sizes
                units = Units.MM;
            }
            paper = PrintHelper.createPaper("Custom", pxlOpts.getSize().getWidth() * convert, pxlOpts.getSize().getHeight() * convert, units);
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

        log.trace("{}", settings.toString());


        for(WebAppModel model : models) {
            try {
                if (pxlOpts.getCopies() > fxPrinter.getPrinterAttributes().getMaxCopies()) {
                    for(int i = 0; i < pxlOpts.getCopies(); i++) {
                        WebApp.print(job, model);
                    }
                } else {
                    settings.setCopies(pxlOpts.getCopies());
                    WebApp.print(job, model);
                }
            }
            catch(Throwable t) {
                throw new PrintException(t.getMessage());
            }
        }

        //send pending prints
        job.endJob();
    }

    @Override
    public void cleanup() {
        models.clear();
    }
}
