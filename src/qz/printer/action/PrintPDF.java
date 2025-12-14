package qz.printer.action;

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.printing.Scaling;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.action.pdf.BookBundle;
import qz.printer.action.pdf.PDFWrapper;
import qz.utils.ConnectionUtilities;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrintPDF extends PrintPixel implements PrintProcessor {

    private static final Logger log = LogManager.getLogger(PrintPDF.class);

    private final List<PDDocument> originals;
    private final List<PDDocument> printables;
    private final Splitter splitter;

    private double docWidth = 0;
    private double docHeight = 0;
    private boolean ignoreTransparency = false;
    private boolean altFontRendering = false;


    public PrintPDF() {
        originals = new ArrayList<>();
        printables = new ArrayList<>();
        splitter = new Splitter();
    }

    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.PDF;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        PrintOptions.Pixel pxlOpts = options.getPixelOptions();
        double convert = 72.0 / pxlOpts.getUnits().as1Inch();

        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.getJSONObject(i);
            HashSet<Integer> pagesToPrint = new HashSet<>();

            if (!data.isNull("options")) {
                JSONObject dataOpt = data.getJSONObject("options");

                if (!dataOpt.isNull("pageWidth") && dataOpt.optDouble("pageWidth") > 0) {
                    docWidth = dataOpt.optDouble("pageWidth") * convert;
                }
                if (!dataOpt.isNull("pageHeight") && dataOpt.optDouble("pageHeight") > 0) {
                    docHeight = dataOpt.optDouble("pageHeight") * convert;
                }

                ignoreTransparency = dataOpt.optBoolean("ignoreTransparency", false);
                altFontRendering = dataOpt.optBoolean("altFontRendering", false);

                if (!dataOpt.isNull("pageRanges")) {
                    String[] ranges = dataOpt.optString("pageRanges", "").split(",");
                    for(String range : ranges) {
                        range = range.trim();
                        if(range.isEmpty()) {
                            continue;
                        }
                        String[] period = range.split("-");

                        try {
                            int start = Integer.parseInt(period[0]);
                            pagesToPrint.add(start);

                            if (period.length > 1) {
                                int end = Integer.parseInt(period[period.length - 1]);
                                pagesToPrint.addAll(IntStream.rangeClosed(start, end).boxed().collect(Collectors.toSet()));
                            }
                        }
                        catch(NumberFormatException nfe) {
                            log.warn("Unable to parse page range {}.", range);
                        }
                    }
                }
            }

            PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.parse(data, PrintingUtilities.Flavor.FILE);

            try {
                PDDocument doc = loadPdf(data.getString("data"), flavor);

                if (pxlOpts.getBounds() != null) {
                    PrintOptions.Bounds bnd = pxlOpts.getBounds();

                    for(PDPage page : doc.getPages()) {
                        PDRectangle box = new PDRectangle(
                                (float)(bnd.getX() * convert),
                                page.getMediaBox().getUpperRightY() - (float)((bnd.getHeight() + bnd.getY()) * convert),
                                (float)(bnd.getWidth() * convert),
                                (float)(bnd.getHeight() * convert));
                        page.setMediaBox(box);
                    }
                }

                if (pagesToPrint.isEmpty()) {
                    pagesToPrint.addAll(IntStream.rangeClosed(1, doc.getNumberOfPages()).boxed().collect(Collectors.toSet()));
                }

                originals.add(doc);

                List<PDDocument> splitPages = splitter.split(doc);
                originals.addAll(splitPages); //ensures non-ranged page will still get closed

                for(int pg = 0; pg < splitPages.size(); pg++) {
                    if (pagesToPrint.contains(pg + 1)) { //ranges are 1-indexed
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

    public static PDDocument loadPdf(String data, PrintingUtilities.Flavor flavor) throws IOException {
        switch(flavor) {
            case PLAIN:
                // There's really no such thing as a 'PLAIN' PDF, assume it's a URL
            case FILE:
                return PDDocument.load(ConnectionUtilities.getInputStream(data, true));
            default:
                return PDDocument.load(new ByteArrayInputStream(flavor.read(data)));
        }
    }

    /**
     * Creates a raw-compatible BufferedImage
     */
    @Override
    public BufferedImage createBufferedImage(String data, JSONObject opt, PrintingUtilities.Flavor flavor, PrintOptions.Raw rawOpts, PrintOptions.Pixel pxlOpts) throws IOException {
        PDDocument doc = loadPdf(data, flavor);

        double scale;
        PDRectangle rect = doc.getPage(0).getBBox();
        double pw = opt.optDouble("pageWidth", 0), ph = opt.optDouble("pageHeight", 0);
        if (ph <= 0 || (pw > 0 && (rect.getWidth() / rect.getHeight()) >= (pw / ph))) {
            scale = pw / rect.getWidth();
        } else {
            scale = ph / rect.getHeight();
        }
        if (scale <= 0) { scale = 1.0; }

        return new PDFRenderer(doc).renderImage(0, (float)scale);
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
                // clear density for vector prints (applied via print attributes instead)
                useDensity = 0;
            } else if (SystemUtilities.isMac() && Constants.JAVA_VERSION.compareWithBuildsTo(Version.valueOf("1.8.0+121")) < 0) {
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

            PDFWrapper wrapper = new PDFWrapper(doc, scale, false, ignoreTransparency, altFontRendering,
                                                (float)(useDensity * pxlOpts.getUnits().as1Inch()),
                                                false, pxlOpts.getOrientation(), hints);

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

        docWidth = 0;
        docHeight = 0;
        ignoreTransparency = false;
        altFontRendering = false;
    }
}
