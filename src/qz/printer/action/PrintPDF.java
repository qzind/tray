package qz.printer.action;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
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
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.pdf.BookBundle;
import qz.printer.action.pdf.FuturePdf;
import qz.printer.action.pdf.PDFWrapper;
import qz.printer.action.pdf.PdfParams;
import qz.utils.ConnectionUtilities;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PrintPDF extends PrintPixel implements PrintProcessor {

    private static final Logger log = LogManager.getLogger(PrintPDF.class);

    private final List<PDDocument> originals;
    private final List<PDDocument> printables;
    private final Splitter splitter = new Splitter();

    private PdfParams pdfParams;


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
        PrintOptions.Pixel pxlOpts = options.getPixelOptions();
        RenderingHints renderingHints = new RenderingHints(buildRenderingHints(pxlOpts.getDithering(), pxlOpts.getInterpolation()));

        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.getJSONObject(i);

            pdfParams = new PdfParams(data.optJSONObject("options"), options, renderingHints);

            PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.parse(data, PrintingUtilities.Flavor.FILE);

            try {
                PDDocument doc;

                if (options.getPixelOptions().isStream()) {
                    doc = new FuturePdf(data.getString("data"));
                    printables.add(doc);
                    continue; //no further doc processing, as it doesn't exist yet
                }

                switch(flavor) {
                    case PLAIN:
                        // There's really no such thing as a 'PLAIN' PDF, assume it's a URL
                    case FILE:
                        doc = Loader.loadPDF(
                                new RandomAccessReadBuffer(
                                    ConnectionUtilities.protocolRestricted(data.getString("data")).openStream()
                                )
                        );
                        break;
                    default:
                        doc = Loader.loadPDF(
                                new RandomAccessReadBuffer(
                                        flavor.read(data.getString("data"))
                                )
                        );
                }

                if (pxlOpts.getBounds() != null) {
                    for(PDPage page : doc.getPages()) {
                        page.setMediaBox(pdfParams.calculateMediaBox(page));
                    }
                }

                pdfParams.setPageRange(doc);
                originals.add(doc);

                List<PDDocument> splitPages = splitter.split(doc);
                originals.addAll(splitPages); //ensures non-ranged page will still get closed

                for(int pg = 0; pg < splitPages.size(); pg++) {
                    if (pdfParams.getPageRange().contains(pg + 1)) { //ranges are 1-indexed
                        printables.add(splitPages.get(pg));
                    }
                }
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

        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, job.getPageFormat(null), (Media[])output.getPrintService().getSupportedAttributeValues(Media.class, null, null));

        // Disable attributes per https://github.com/qzind/tray/issues/174
        if (SystemUtilities.isMac() && Constants.JAVA_VERSION.compareWithBuildsTo(Version.valueOf("1.8.0+202")) < 0) {
            log.warn("MacOS and Java < 1.8.0u202 cannot use attributes with PDF prints, disabling");
            attributes.clear();
        }

        BookBundle bundle = new BookBundle();

        for(PDDocument doc : printables) {
            PageFormat page = job.getPageFormat(null);
            applyDefaultSettings(pxlOpts, page, output.getSupportedMedia());

            if (doc instanceof FuturePdf) {
                FuturePdf future = (FuturePdf)doc;
                future.buildFutureWrapper(pdfParams);

                bundle.flagForStreaming(true);
                //fixme - book bundle short-circuits based on total pages, how to bypass ?
                bundle.append(future.getFutureWrapper(), page, 5);

                continue; //no further doc processing, as it doesn't exist yet
            }

            //trick pdfbox into an alternate doc size if specified
            if (pdfParams.isCustomSize()) {
                Paper paper = page.getPaper();
                paper.setImageableArea(paper.getImageableX(),
                                       paper.getImageableY(),
                                       pdfParams.getDocWidth(page.getImageableWidth()),
                                       pdfParams.getDocHeight(page.getImageableHeight()));
                page.setPaper(paper);

                pdfParams.setScaling(Scaling.SCALE_TO_FIT); //to get custom size we need to force scaling

                //pdf uses imageable area from Paper, so this can be safely removed
                attributes.remove(MediaPrintableArea.class);
            }

            for(PDPage pd : doc.getPages()) {
                if (pxlOpts.getRotation() % 360 != 0) {
                    rotatePage(doc, pd, pxlOpts.getRotation());
                }

                if (pxlOpts.getOrientation() == null) {
                    PDRectangle bounds = pd.getBBox();
                    if ((page.getImageableHeight() > page.getImageableWidth() && bounds.getWidth() > bounds.getHeight()) ^ (pd.getRotation() / 90) % 2 == 1) {
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

            PDFWrapper wrapper = new PDFWrapper(doc, pdfParams);
            bundle.append(wrapper, page, doc.getNumberOfPages());
        }

        if (pxlOpts.getSpoolSize() > 0 && bundle.getNumberOfPages() > pxlOpts.getSpoolSize()) {
            int jobNum = 1;
            int offset = 0;
            while(offset < bundle.getNumberOfPages()) {
                job.setJobName(pxlOpts.getJobName(Constants.PDF_PRINT) + "-" + jobNum++);
                job.setPageable(bundle.wrapAndPresent(offset, pxlOpts.getSpoolSize()));

                printCopies(output, pxlOpts, job, attributes);

                offset += pxlOpts.getSpoolSize();
            }
        } else {
            job.setJobName(pxlOpts.getJobName(Constants.PDF_PRINT));
            job.setPageable(bundle.wrapAndPresent());

            printCopies(output, pxlOpts, job, attributes);
        }
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
        for(PDDocument doc : originals) {
            try { doc.close(); } catch(IOException ignore) {}
        }

        originals.clear();
        printables.clear();
    }
}
