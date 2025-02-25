package qz.printer.action.pdf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.printing.Scaling;
import org.codehaus.jettison.json.JSONObject;
import qz.printer.PrintOptions;

import javax.print.attribute.standard.OrientationRequested;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PdfParams {
    private static final Logger log = LogManager.getLogger(PdfParams.class);
    private PrintOptions options;
    private double docWidth;
    private double docHeight;
    private boolean ignoreTransparency;
    private boolean altFontRendering;
    private double convert;
    private float dpi;
    private Scaling scaling;
    HashSet<Integer> pageRange;

    public PdfParams(JSONObject params, PrintOptions options) {
        this.options = options;
        convert = 72.0 / options.getPixelOptions().getUnits().as1Inch();
        scaling = options.getPixelOptions().isScaleContent() ? Scaling.SCALE_TO_FIT:Scaling.ACTUAL_SIZE;

        if(params != null) {
            docWidth = params.optDouble("pageWidth", 0) * convert;
            docHeight = params.optDouble("pageHeight", 0) * convert;
            ignoreTransparency = params.optBoolean("ignoreTransparency", false);
            altFontRendering = params.optBoolean("altFontRendering", false);
            pageRange = parsePageRange(params);
            dpi = calculateDpi(options);
        } else {
            docWidth = 0;
            docHeight = 0;
            ignoreTransparency = false;
            altFontRendering = false;
            pageRange = new HashSet<>();
            dpi = 0;
        }
    }

    /**
     * Calculates the DPI based on PDF-specific factors
     * - Vector prints will use a value of 0
     * - Rasterized prints will use the value specified, converted to inches
     */
    private static float calculateDpi(PrintOptions options) {
        PrintOptions.Pixel pxlOpts = options.getPixelOptions();
        double density = pxlOpts.getDensity();

        if (!pxlOpts.isRasterize()) {
            // clear density for vector prints (applied via print attributes instead)
            return 0;
        }
        return (float)(density * pxlOpts.getUnits().as1Inch());
    }

    private static HashSet<Integer> parsePageRange(JSONObject params) {
        HashSet<Integer> pageRange = new HashSet<>();
        String[] ranges = params.optString("pageRanges", "").split(",");
        for(String range : ranges) {
            range = range.trim();
            if(range.isEmpty()) {
                continue;
            }
            String[] period = range.split("-");

            try {
                int start = Integer.parseInt(period[0]);
                pageRange.add(start);

                if (period.length > 1) {
                    int end = Integer.parseInt(period[period.length - 1]);
                    pageRange.addAll(IntStream.rangeClosed(start, end).boxed().collect(Collectors.toSet()));
                }
            }
            catch(NumberFormatException nfe) {
                log.warn("Unable to parse page range {}.", range);
            }
        }
        return pageRange;
    }

    /**
     * Sets the fallback page range based on the number of pages in the document
     */
    public void setPageRange(PDDocument doc) {
        if(pageRange.isEmpty()) {
            pageRange.addAll(IntStream.rangeClosed(1, doc.getNumberOfPages()).boxed().collect(Collectors.toSet()));
        }
    }

    public PDRectangle calculateMediaBox(PDPage page) {
        PrintOptions.Bounds bnd = options.getPixelOptions().getBounds();
        return new PDRectangle(
                (float)(bnd.getX() * convert),
                page.getMediaBox().getUpperRightY() - (float)((bnd.getHeight() + bnd.getY()) * convert),
                (float)(bnd.getWidth() * convert),
                (float)(bnd.getHeight() * convert));
    }

    public double getDocWidth(double defaultVal) {
        return docWidth > 0 ? docWidth : defaultVal;
    }

    public double getDocHeight(double defaultVal) {
        return docHeight > 0 ? docHeight : defaultVal;
    }

    public boolean isIgnoreTransparency() {
        return ignoreTransparency;
    }

    public boolean isAltFontRendering() {
        return altFontRendering;
    }

    public double getConvert() {
        return convert;
    }

    public boolean isCustomSize() {
        return docWidth >= 0 || docHeight >= 0;
    }

    public HashSet<Integer> getPageRange() {
        return pageRange;
    }

    public float getDpi() {
        return dpi;
    }

    public Scaling getScaling() {
        return scaling;
    }

    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
    }

    public PrintOptions.Orientation getOrientation() {
        return options.getPixelOptions().getOrientation();
    }

    public boolean isOrientationRequested(OrientationRequested match) {
        return getOrientation() == null ? false : getOrientation().getAsOrientRequested() == match;
    }

}


