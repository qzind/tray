package qz.printer.action;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.ssl.Base64;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
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
import qz.utils.ConnectionUtilities;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;

import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.*;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrintPDF extends PrintPixel implements PrintProcessor {

    private static final Logger log = LoggerFactory.getLogger(PrintPDF.class);

    private List<PDDocument> originals;
    private List<PDDocument> printables;
    private Splitter splitter = new Splitter();

    private double docWidth = 0;
    private double docHeight = 0;


    public PrintPDF() {
        originals = new ArrayList<>();
        printables = new ArrayList<>();
    }

    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.PDF;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.getJSONObject(i);

            if (!data.isNull("options")) {
                JSONObject dataOpt = data.getJSONObject("options");

                if (!dataOpt.isNull("pageWidth") && dataOpt.optDouble("pageWidth") > 0) {
                    docWidth = dataOpt.optDouble("pageWidth") * (72.0 / options.getPixelOptions().getUnits().as1Inch());
                }
                if (!dataOpt.isNull("pageHeight") && dataOpt.optDouble("pageHeight") > 0) {
                    docHeight = dataOpt.optDouble("pageHeight") * (72.0 / options.getPixelOptions().getUnits().as1Inch());
                }
            }

            PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.valueOf(data.optString("flavor", "FILE").toUpperCase(Locale.ENGLISH));

            try {
                PDDocument doc;
                if (flavor == PrintingUtilities.Flavor.BASE64) {
                    doc = PDDocument.load(new ByteArrayInputStream(Base64.decodeBase64(data.getString("data"))));
                } else {
                    doc = PDDocument.load(ConnectionUtilities.getInputStream(data.getString("data")));
                }

                originals.add(doc);
                printables.addAll(splitter.split(doc));
            }
            catch(FileNotFoundException e) {
                throw new UnsupportedOperationException("PDF file specified could not be found.", e);
            }
            catch(IOException e) {
                throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as a PDF file: %s", flavor, data.getString("data"), e.getLocalizedMessage()), e);
            }
        }

        log.debug("Parsed {} files for printing", printables.size());
    }

    @Override
    public PrintRequestAttributeSet applyDefaultSettings(PrintOptions.Pixel pxlOpts, PageFormat page, Media[] supported) {
        if (pxlOpts.getOrientation() != null) {
            //page orient does not set properly on pdfs with orientation requested attribute
            page.setOrientation(pxlOpts.getOrientation().getAsOrientFormat());
        }

        return super.applyDefaultSettings(pxlOpts, page, supported);
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrinterException {
        if (printables.isEmpty()) {
            log.warn("Nothing to print");
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(output.getPrintService());

        PrintOptions.Pixel pxlOpts = options.getPixelOptions();
        Scaling scale = (pxlOpts.isScaleContent()? Scaling.SCALE_TO_FIT:Scaling.ACTUAL_SIZE);

        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, job.getPageFormat(null), (Media[])output.getPrintService().getSupportedAttributeValues(Media.class, null, null));

        // Disable attributes per https://github.com/qzind/tray/issues/174
        if (SystemUtilities.isMac() && Constants.JAVA_VERSION.compareWithBuildsTo(Version.valueOf("1.8.0+202")) < 0) {
            log.warn("MacOS and Java < 1.8.0u202 cannot use attributes with PDF prints, disabling");
            attributes.clear();
        }

        RenderingHints hints = new RenderingHints(buildRenderingHints(pxlOpts.getDithering(), pxlOpts.getInterpolation()));
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

        BookBundle bundle = new BookBundle();

        for(PDDocument doc : printables) {
            PageFormat page = job.getPageFormat(null);
            applyDefaultSettings(pxlOpts, page, output.getSupportedMedia());

            //trick pdfbox into an alternate doc size if specified
            if (docWidth > 0 || docHeight > 0) {
                Paper paper = page.getPaper();

                if (docWidth <= 0) { docWidth = page.getImageableWidth(); }
                if (docHeight <= 0) { docHeight = page.getImageableHeight(); }

                paper.setImageableArea(paper.getImageableX(), paper.getImageableY(), docWidth, docHeight);
                page.setPaper(paper);

                scale = Scaling.SCALE_TO_FIT; //to get custom size we need to force scaling

                //pdf uses imageable area from Paper, so this can be safely removed
                attributes.remove(MediaPrintableArea.class);
            }

            for(PDPage pd : doc.getPages()) {
                if (pxlOpts.getRotation() % 360 != 0) {
                    rotatePage(doc, pd, pxlOpts.getRotation());
                }

                if (pxlOpts.getOrientation() == null) {
                    PDRectangle bounds = pd.getBBox();
                    if ((page.getImageableHeight() > page.getImageableWidth() && bounds.getWidth() > bounds.getHeight()) || (pd.getRotation() / 90) % 2 == 1) {
                        log.info("Adjusting orientation to print landscape PDF source");
                        page.setOrientation(PrintOptions.Orientation.LANDSCAPE.getAsOrientFormat());
                    }
                } else if (pxlOpts.getOrientation() != PrintOptions.Orientation.PORTRAIT) {
                    //flip imageable area dimensions when in landscape
                    Paper repap = page.getPaper();
                    repap.setImageableArea(repap.getImageableX(), repap.getImageableY(), repap.getImageableHeight(), repap.getImageableWidth());
                    page.setPaper(repap);

                    //reverse fix for OSX
                    if (SystemUtilities.isMac() && pxlOpts.getOrientation() == PrintOptions.Orientation.REVERSE_LANDSCAPE) {
                        pd.setRotation(pd.getRotation() + 180);
                    }
                }
            }

            bundle.append(new PDFWrapper(doc, scale, false, (float)(useDensity * pxlOpts.getUnits().as1Inch()), false, pxlOpts.getOrientation(), hints), page, doc.getNumberOfPages());
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
        for(PDDocument doc : printables) {
            try { doc.close(); } catch(IOException ignore) {}
        }
        for(PDDocument doc : originals) {
            try { doc.close(); } catch(IOException ignore) {}
        }

        originals.clear();
        printables.clear();

        docWidth = 0;
        docHeight = 0;
    }
}
