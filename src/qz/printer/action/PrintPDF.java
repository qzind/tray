package qz.printer.action;

import org.apache.commons.ssl.Base64;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrintPDF extends PrintPixel implements PrintProcessor {

    private static final Logger log = LoggerFactory.getLogger(PrintPDF.class);

    private List<PDDocument> pdfs;


    public PrintPDF() {
        pdfs = new ArrayList<>();
    }

    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.PDF;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.getJSONObject(i);

            PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.valueOf(data.optString("flavor", "FILE").toUpperCase(Locale.ENGLISH));

            try {
                PDDocument doc;
                if (flavor == PrintingUtilities.Flavor.BASE64) {
                    doc = PDDocument.load(new ByteArrayInputStream(Base64.decodeBase64(data.getString("data"))));
                } else {
                    doc = PDDocument.load(new URL(data.getString("data")).openStream());
                }

                pdfs.add(doc);
            }
            catch(FileNotFoundException e) {
                throw new UnsupportedOperationException("PDF file specified could not be found.", e);
            }
            catch(IOException e) {
                throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as a PDF file: %s", flavor, data.getString("data"), e.getLocalizedMessage()), e);
            }
        }

        log.debug("Parsed {} files for printing", pdfs.size());
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrinterException {
        if (pdfs.isEmpty()) {
            log.warn("Nothing to print");
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(output.getPrintService());
        PageFormat page = job.getPageFormat(null);

        PrintOptions.Pixel pxlOpts = options.getPixelOptions();
        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, page, (Media[])output.getPrintService().getSupportedAttributeValues(Media.class, null, null));

        if (SystemUtilities.isMac()) {
            log.warn("OSX cannot use attributes with PDF prints, disabling");
            attributes.clear();
        }

        Scaling scale = (pxlOpts.isScaleContent()? Scaling.SCALE_TO_FIT:Scaling.ACTUAL_SIZE);
        double useDensity = pxlOpts.getDensity();

        if (!pxlOpts.isRasterize()) {
            if (pxlOpts.getDensity() > 0) {
                //rasterization is automatically performed upon supplying a density, warn user if they aren't expecting this
                log.warn("Supplying a print density for PDF printing rasterizes the document.");
            } else if (SystemUtilities.isMac()) {
                log.warn("OSX systems cannot print vector PDF's, forcing raster to prevent crash.");
                useDensity = options.getDefaultOptions().getDensity();
            }
        }

        PDDocument masterDoc = new PDDocument();
        PDFMergerUtility mu = new PDFMergerUtility();

        try {
            for(PDDocument doc : pdfs) {
                for(PDPage pd : doc.getPages()) {
                    if (pxlOpts.getRotation() % 360 != 0) {
                        rotatePage(doc, pd, pxlOpts.getRotation());
                    }

                    if (!attributes.containsKey(OrientationRequested.class) && pxlOpts.getOrientation() != null &&
                            pxlOpts.getOrientation() != PrintOptions.Orientation.PORTRAIT) {
                        //force orientation change at data level
                        pd.setRotation(pxlOpts.getOrientation().getDegreesRot());
                    }
                }

                mu.appendDocument(masterDoc, doc);
            }

            mu.mergeDocuments(null);
        }
        catch(IOException e) {
            throw new PrinterException(e.getLocalizedMessage());
        }

        job.setJobName(pxlOpts.getJobName(Constants.PDF_PRINT));
        job.setPrintable(new PDFPrintable(masterDoc, scale, false, (float)(useDensity * pxlOpts.getUnits().as1Inch()), false), page);

        printCopies(output, pxlOpts, job, attributes);

        try { masterDoc.close(); } catch(Exception ignore) {}
    }

    private void rotatePage(PDDocument doc, PDPage page, double rotation) {
        try {
            //copy page to object for manipulation
            PDFormXObject xobject = new PDFormXObject(doc);
            InputStream src = page.getContents();
            OutputStream dest = xobject.getStream().createOutputStream();

            try { IOUtils.copy(src, dest); }
            finally {
                IOUtils.closeQuietly(src);
                IOUtils.closeQuietly(dest);
            }

            xobject.setResources(page.getResources());
            xobject.setBBox(page.getBBox());

            //draw our object at a rotated angle
            AffineTransform transform = new AffineTransform();
            transform.rotate(Math.toRadians(360 - rotation), xobject.getBBox().getWidth() / 2.0, xobject.getBBox().getHeight() / 2.0);
            xobject.setMatrix(transform);

            PDPageContentStream stream = new PDPageContentStream(doc, page);
            stream.drawForm(xobject);
            stream.close();
        }
        catch(IOException e) {
            log.warn("Failed to rotate PDF page for printing");
        }
    }

    @Override
    public void cleanup() {
        for(PDDocument doc : pdfs) {
            try { doc.close(); } catch(IOException ignore) {}
        }

        pdfs.clear();
    }
}
