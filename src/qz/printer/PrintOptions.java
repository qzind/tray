package qz.printer;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;

import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrinterResolution;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import java.util.Locale;

public class PrintOptions {

    private static final Logger log = LoggerFactory.getLogger(PrintOptions.class);

    private Pixel psOptions = new Pixel();
    private Raw rawOptions = new Raw();
    private Default defOptions = new Default();


    /**
     * Parses the provided JSON Object into relevant Pixel and Raw options
     */
    public PrintOptions(JSONObject configOpts, PrintOutput output) {
        if (configOpts == null) { return; }

        //check for raw options
        if (!configOpts.isNull("altPrinting")) {
            try {
                rawOptions.altPrinting = configOpts.getBoolean("altPrinting");
                if (rawOptions.altPrinting && SystemUtilities.isWindows()) {
                    log.warn("Alternate raw printing is not supported on Windows");
                    rawOptions.altPrinting = false;
                }
            }
            catch(JSONException e) { warn("boolean", "altPrinting", configOpts.opt("altPrinting")); }
        }
        if (!configOpts.isNull("encoding")) {
            rawOptions.encoding = configOpts.optString("encoding", null);
        }
        if (!configOpts.isNull("endOfDoc")) {
            rawOptions.endOfDoc = configOpts.optString("endOfDoc", null);
        }
        if (!configOpts.isNull("language")) {
            rawOptions.language = configOpts.optString("language", null);
        }
        if (!configOpts.isNull("perSpool")) {
            try { rawOptions.perSpool = configOpts.getInt("perSpool"); }
            catch(JSONException e) { warn("integer", "perSpool", configOpts.opt("perSpool")); }
        }
        if (!configOpts.isNull("copies")) {
            try { rawOptions.copies = configOpts.getInt("copies"); }
            catch(JSONException e) { warn("integer", "copies", configOpts.opt("copies")); }
        }
        if (!configOpts.isNull("jobName")) {
            rawOptions.jobName = configOpts.optString("jobName", null);
        }

        //check for pixel options
        if (!configOpts.isNull("colorType")) {
            try {
                psOptions.colorType = ColorType.valueOf(configOpts.optString("colorType").toUpperCase(Locale.ENGLISH));
            }
            catch(IllegalArgumentException e) {
                warn("valid value", "colorType", configOpts.opt("colorType"));
            }
        }
        if (!configOpts.isNull("copies")) {
            try { psOptions.copies = configOpts.getInt("copies"); }
            catch(JSONException e) { warn("integer", "copies", configOpts.opt("copies")); }
            if (psOptions.copies < 1) {
                log.warn("Cannot have less than one copy");
                psOptions.copies = 1;
            }
        }
        if (!configOpts.isNull("density")) {
            JSONArray possibleDPIs = configOpts.optJSONArray("density");
            if (possibleDPIs != null && possibleDPIs.length() > 0) {
                int usableDpi = -1;

                List<Integer> rSupport = PrintingUtilities.getSupportedDensities(output.getPrintService());
                if (!rSupport.isEmpty()) {
                    for(int i = 0; i < possibleDPIs.length(); i++) {
                        if (rSupport.contains(possibleDPIs.optInt(i))) {
                            usableDpi = possibleDPIs.optInt(i);
                            break;
                        }
                    }
                }

                if (usableDpi == -1) {
                    log.warn("Supported printer densities not found, using first value provided");
                    usableDpi = possibleDPIs.optInt(0);
                }

                psOptions.density = usableDpi;
            } else {
                try { psOptions.density = configOpts.getDouble("density"); }
                catch(JSONException e) { warn("double", "density", configOpts.opt("density")); }
            }
        }
        if (!configOpts.isNull("duplex")) {
            try { psOptions.duplex = configOpts.getBoolean("duplex"); }
            catch(JSONException e) { warn("boolean", "duplex", configOpts.opt("duplex")); }
        }
        if (!configOpts.isNull("interpolation")) {
            switch(configOpts.optString("interpolation")) {
                case "bicubic":
                    psOptions.interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC; break;
                case "bilinear":
                    psOptions.interpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR; break;
                case "nearest-neighbor": case "nearest":
                    psOptions.interpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR; break;
                default:
                    warn("valid value", "interpolation", configOpts.opt("interpolation")); break;
            }
        }
        if (!configOpts.isNull("jobName")) {
            psOptions.jobName = configOpts.optString("jobName", null);
        }
        if (!configOpts.isNull("legacy")) {
            psOptions.legacy = configOpts.optBoolean("legacy", false);
        }
        if (!configOpts.isNull("margins")) {
            Margins m = new Margins();
            JSONObject subMargins = configOpts.optJSONObject("margins");
            if (subMargins != null) {
                //each individually
                if (!subMargins.isNull("top")) {
                    try { m.top = subMargins.getDouble("top"); }
                    catch(JSONException e) { warn("double", "margins.top", subMargins.opt("top")); }
                }
                if (!subMargins.isNull("right")) {
                    try { m.right = subMargins.getDouble("right"); }
                    catch(JSONException e) { warn("double", "margins.right", subMargins.opt("right")); }
                }
                if (!subMargins.isNull("bottom")) {
                    try { m.bottom = subMargins.getDouble("bottom"); }
                    catch(JSONException e) { warn("double", "margins.bottom", subMargins.opt("bottom")); }
                }
                if (!subMargins.isNull("left")) {
                    try { m.left = subMargins.getDouble("left"); }
                    catch(JSONException e) { warn("double", "margins.left", subMargins.opt("left")); }
                }
            } else {
                try { m.setAll(configOpts.getDouble("margins")); }
                catch(JSONException e) { warn("double", "margins", configOpts.opt("margins")); }
            }

            psOptions.margins = m;
        }
        if (!configOpts.isNull("orientation")) {
            try {
                psOptions.orientation = Orientation.valueOf(configOpts.optString("orientation").replaceAll("\\-", "_").toUpperCase(Locale.ENGLISH));
            }
            catch(IllegalArgumentException e) {
                warn("valid value", "orientation", configOpts.opt("orientation"));
            }
        }
        if (!configOpts.isNull("paperThickness")) {
            try { psOptions.paperThickness = configOpts.getDouble("paperThickness"); }
            catch(JSONException e) { warn("double", "paperThickness", configOpts.opt("paperThickness")); }
        }
        if (!configOpts.isNull("printerTray")) {
            psOptions.printerTray = configOpts.optString("printerTray", null);
        }
        if (!configOpts.isNull("rasterize")) {
            try { psOptions.rasterize = configOpts.getBoolean("rasterize"); }
            catch(JSONException e) { warn("boolean", "rasterize", configOpts.opt("rasterize")); }
        }
        if (!configOpts.isNull("rotation")) {
            try { psOptions.rotation = configOpts.getDouble("rotation"); }
            catch(JSONException e) { warn("double", "rotation", configOpts.opt("rotation")); }
        }
        if (!configOpts.isNull("scaleContent")) {
            try { psOptions.scaleContent = configOpts.getBoolean("scaleContent"); }
            catch(JSONException e) { warn("boolean", "scaleContent", configOpts.opt("scaleContent")); }
        }
        if (!configOpts.isNull("size")) {
            Size s = new Size();
            JSONObject subSize = configOpts.optJSONObject("size");
            if (subSize != null) {
                if (!subSize.isNull("width")) {
                    try { s.width = subSize.getDouble("width"); }
                    catch(JSONException e) { warn("double", "size.width", subSize.opt("width")); }
                }
                if (!subSize.isNull("height")) {
                    try { s.height = subSize.getDouble("height"); }
                    catch(JSONException e) { warn("double", "size.height", subSize.opt("height")); }
                }

                psOptions.size = s;
            } else {
                warn("JSONObject", "size", configOpts.opt("size"));
            }
        }
        if (!configOpts.isNull("units")) {
            switch(configOpts.optString("units")) {
                case "mm":
                    psOptions.units = Unit.MM; break;
                case "cm":
                    psOptions.units = Unit.CM; break;
                case "in":
                    psOptions.units = Unit.INCH; break;
                default:
                    warn("valid value", "units", configOpts.opt("units")); break;
            }
        }

        //grab any useful service defaults
        PrinterResolution defaultRes = PrintingUtilities.getNativeDensity(output.getPrintService());
        if (defaultRes != null) {
            //convert dphi to unit-dependant density ourselves (to keep as double type)
            defOptions.density = (double)defaultRes.getFeedResolution(1) / psOptions.getUnits().getDPIUnits();
        } else {
            try { defOptions.density = configOpts.getDouble("fallbackDensity"); }
            catch(JSONException e) {
                warn("double", "fallbackDensity", configOpts.opt("fallbackDensity"));
                //manually convert default dphi to a density value based on units
                defOptions.density = 60000d / psOptions.getUnits().getDPIUnits();
            }
        }
        if (psOptions.isRasterize() && psOptions.getDensity() == 0) {
            psOptions.density = defOptions.density;
        }

        if (output.isSetService()) {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintService(output.getPrintService());
                PageFormat page = job.getPageFormat(null);
                defOptions.pageSize = new Size(page.getWidth(), page.getHeight());
            }
            catch(PrinterException e) {
                log.warn("Unable to find the default paper size");
            }
        }
    }

    /**
     * Helper method for parse warnings
     *
     * @param expectedType Expected entry type
     * @param name         Option name
     * @param actualValue  Invalid value passed
     */
    private static void warn(String expectedType, String name, Object actualValue) {
        log.warn("Cannot read {} as a {} for {}, using default", actualValue, expectedType, name);
    }


    public Raw getRawOptions() {
        return rawOptions;
    }

    public Pixel getPixelOptions() {
        return psOptions;
    }

    public Default getDefaultOptions() { return defOptions; }


    // Option groups //

    /** Raw printing options */
    public class Raw {
        private boolean altPrinting = false;    //Alternate printing for linux systems
        private String encoding = null;         //Text encoding / charset
        private String endOfDoc = null;         //End of document character
        private String language = null;         //Printer language
        private int perSpool = 1;               //Pages per spool
        private int copies = 1;                 //Job copies
        private String jobName = null;          //Job name


        public boolean isAltPrinting() {
            return altPrinting;
        }

        public String getEncoding() {
            return encoding;
        }

        public String getEndOfDoc() {
            return endOfDoc;
        }

        public String getLanguage() {
            return language;
        }

        public int getPerSpool() {
            return perSpool;
        }

        public int getCopies() {
            return copies;
        }

        public String getJobName(String defaultVal) {
            return jobName == null || jobName.isEmpty()? defaultVal:jobName;
        }
    }

    /** Pixel printing options */
    public class Pixel {
        private ColorType colorType = ColorType.COLOR;                              //Color / black&white
        private int copies = 1;                                                     //Job copies
        private double density = 0;                                                 //Pixel density (DPI or DPMM)
        private boolean duplex = false;                                             //Double/single sided
        private Object interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;  //Image interpolation
        private String jobName = null;                                              //Job name
        private boolean legacy = false;                                             //Legacy printing
        private Margins margins = new Margins();                                    //Page margins
        private Orientation orientation = null;                                     //Page orientation
        private double paperThickness = -1;                                         //Paper thickness
        private String printerTray = null;                                          //Printer tray to use
        private boolean rasterize = true;                                           //Whether documents are rasterized before printing
        private double rotation = 0;                                                //Image rotation
        private boolean scaleContent = true;                                        //Adjust paper size for best image fit
        private Size size = null;                                                   //Paper size
        private Unit units = Unit.INCH;                                             //Units for density, margins, size


        public ColorType getColorType() {
            return colorType;
        }

        public int getCopies() {
            return copies;
        }

        public double getDensity() {
            return density;
        }

        public boolean isDuplex() {
            return duplex;
        }

        public Object getInterpolation() {
            return interpolation;
        }

        public String getJobName(String defaultVal) {
            return jobName == null || jobName.isEmpty()? defaultVal:jobName;
        }

        public boolean isLegacy() {
            return legacy;
        }

        public Margins getMargins() {
            return margins;
        }

        public Orientation getOrientation() {
            return orientation;
        }

        public double getPaperThickness() {
            return paperThickness;
        }

        public String getPrinterTray() {
            return printerTray;
        }

        public boolean isRasterize() {
            return rasterize;
        }

        public double getRotation() {
            return rotation;
        }

        public boolean isScaleContent() {
            return scaleContent;
        }

        public Size getSize() {
            return size;
        }

        public Unit getUnits() {
            return units;
        }
    }

    /** PrintService Defaults **/
    public class Default {
        private double density;
        private Size pageSize;


        public double getDensity() {
            return density;
        }

        public Size getPageSize() {
            return pageSize;
        }
    }

    // Sub options //

    /** Pixel page size options */
    public class Size {
        private double width = -1;  //Page width
        private double height = -1; //Page height


        public Size() {}

        public Size(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
        }
    }

    /** Pixel page margins options */
    public class Margins {
        private double top = 0;     //Top page margin
        private double right = 0;   //Right page margin
        private double bottom = 0;  //Bottom page margin
        private double left = 0;    //Left page margin

        private void setAll(double margin) {
            top = margin;
            right = margin;
            bottom = margin;
            left = margin;
        }


        public double top() {
            return top;
        }

        public double right() {
            return right;
        }

        public double bottom() {
            return bottom;
        }

        public double left() {
            return left;
        }
    }

    /** Pixel dimension values */
    public enum Unit {
        INCH(ResolutionSyntax.DPI, 1.0f, 1.0f, Size2DSyntax.INCH),       //1in = 1in
        CM(ResolutionSyntax.DPCM, .3937f, 2.54f, 10000),                 //1cm = .3937in ; 1in = 2.54cm
        MM(ResolutionSyntax.DPCM * 10, .03937f, 25.4f, Size2DSyntax.MM); //1mm = .03937in ; 1in = 25.4mm

        private final float fromInch;
        private final float toInch; //multiplicand to convert to inches
        private final int resSyntax;
        private final int µm;

        Unit(int resSyntax, float toIN, float fromIN, int µm) {
            toInch = toIN;
            fromInch = fromIN;
            this.resSyntax = resSyntax;
            this.µm = µm;
        }

        public float toInches() {
            return toInch;
        }

        public float as1Inch() {
            return fromInch;
        }

        public int getDPIUnits() {
            return resSyntax;
        }

        public int getMediaSizeUnits() {
            return µm;
        }
    }

    /** Pixel page orientation option */
    public enum Orientation {
        PORTRAIT(OrientationRequested.PORTRAIT, PageFormat.PORTRAIT, 0),
        REVERSE_PORTRAIT(OrientationRequested.PORTRAIT, PageFormat.PORTRAIT, 180),
        LANDSCAPE(OrientationRequested.LANDSCAPE, PageFormat.LANDSCAPE, 270),
        REVERSE_LANDSCAPE(OrientationRequested.REVERSE_LANDSCAPE, PageFormat.REVERSE_LANDSCAPE, 90);

        private final OrientationRequested asAttribute; //OrientationRequested const
        private final int asFormat; //PageFormat const
        private final int degreesRot; //degrees rotated

        Orientation(OrientationRequested asAttribute, int asFormat, int degreesRot) {
            this.asAttribute = asAttribute;
            this.asFormat = asFormat;
            this.degreesRot = degreesRot;
        }


        public OrientationRequested getAsAttribute() {
            return asAttribute;
        }

        public int getAsFormat() {
            return asFormat;
        }

        public int getDegreesRot() {
            return degreesRot;
        }
    }

    /** Pixel page color option */
    public enum ColorType {
        COLOR(Chromaticity.COLOR),
        GREYSCALE(Chromaticity.MONOCHROME),
        GRAYSCALE(Chromaticity.MONOCHROME),
        BLACKWHITE(Chromaticity.MONOCHROME);

        private Chromaticity chromatic;

        ColorType(Chromaticity chromatic) {
            this.chromatic = chromatic;
        }


        public Chromaticity getChromatic() {
            return chromatic;
        }
    }

}
