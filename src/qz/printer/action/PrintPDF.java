package qz.printer.action;

import net.sourceforge.iharder.Base64;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.printing.Scaling;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.printer.BookBundle;
import qz.printer.PDFWrapper;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;

import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
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
    public PrintingUtilities.Type getType() {
        return PrintingUtilities.Type.PDF;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.getJSONObject(i);

            PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", "FILE").toUpperCase(Locale.ENGLISH));

            try {
                PDDocument doc;
                if (format == PrintingUtilities.Format.BASE64) {
                    doc = PDDocument.load(new ByteArrayInputStream(Base64.decode(data.getString("data"))));
                } else {
                    doc = PDDocument.load(new URL(data.getString("data")).openStream());
                }

                pdfs.add(doc);
            }
            catch(FileNotFoundException e) {
                throw new UnsupportedOperationException("PDF file specified could not be found.", e);
            }
            catch(IOException e) {
                throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as a PDF file", format, data.getString("data")), e);
            }
        }

        log.debug("Parsed {} files for printing", pdfs.size());
    }

    @Override
    public PrintRequestAttributeSet applyDefaultSettings(PrintOptions.Pixel pxlOpts, PageFormat page) {
        //page orient does not set properly on pdfs with orientation requested attribute
        page.setOrientation(pxlOpts.getOrientation().getAsFormat());
        return super.applyDefaultSettings(pxlOpts, page);
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
        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, page);

        if (SystemUtilities.isMac()) {
            log.warn("OSX cannot use attributes with PDF prints, disabling");
            attributes.clear();
        }

        Scaling scale = (pxlOpts.isScaleContent()? Scaling.SCALE_TO_FIT:Scaling.ACTUAL_SIZE);

        BookBundle bundle = new BookBundle();

        for(PDDocument doc : pdfs) {
            for(PDPage pd : doc.getPages()) {
                if (pxlOpts.getRotation() % 360 != 0) {
                    rotatePage(doc, pd, pxlOpts.getRotation());
                }

                if (pxlOpts.getOrientation() != null && pxlOpts.getOrientation() != PrintOptions.Orientation.PORTRAIT) {
                    //flip imageable area dimensions when in landscape
                    Paper repap = page.getPaper();
                    repap.setImageableArea(repap.getImageableX(), repap.getImageableY(), repap.getImageableHeight(), repap.getImageableWidth());
                    page.setPaper(repap);
                }
            }

            bundle.append(new PDFWrapper(doc, scale, false, (float)(pxlOpts.getDensity() * pxlOpts.getUnits().as1Inch()), false), page, doc.getNumberOfPages());
        }

        job.setJobName(pxlOpts.getJobName(Constants.PDF_PRINT));
        job.setPageable(bundle.wrapAndPresent());

        printCopies(output, pxlOpts, job, attributes);
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
