package qz.printer;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.LoggerUtilities;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;

import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.Sides;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PrintOptions {

    private static final Logger log = LogManager.getLogger(PrintOptions.class);

    private Pixel psOptions = new Pixel();
    private Raw rawOptions = new Raw();
    private Default defOptions = new Default();


    /**
     * Parses the provided JSON Object into relevant Pixel and Raw options
     */
    public PrintOptions(JSONObject configOpts, PrintOutput output, PrintingUtilities.Format format) {
        if (configOpts == null) { return; }

        //check for raw options
        if (!configOpts.isNull("forceRaw")) {
            rawOptions.forceRaw = configOpts.optBoolean("forceRaw", false);
        } else if (!configOpts.isNull("altPrinting")) {
            log.warn("Raw option \"altPrinting\" is deprecated.  Please use \"forceRaw\" instead.");
            rawOptions.forceRaw = configOpts.optBoolean("altPrinting", false);
        }
        if (rawOptions.forceRaw && SystemUtilities.isWindows()) {
            log.warn("Forced raw printing is not supported on Windows");
            rawOptions.forceRaw = false;
        }

        if (!configOpts.isNull("encoding")) {
            JSONObject encodings = configOpts.optJSONObject("encoding");
            if (encodings != null) {
                rawOptions.srcEncoding = encodings.optString("from", null);
                rawOptions.destEncoding = encodings.optString("to", null);
            } else {
                rawOptions.destEncoding = configOpts.optString("encoding", null);
            }
        }
        if (!configOpts.isNull("spool")) {
            JSONObject spool = configOpts.optJSONObject("spool");
            if (spool != null) {
                if (!spool.isNull("size")) {
                    try { rawOptions.spoolSize = spool.getInt("size"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "spool.size", spool.opt("size")); }
                }
                // TODO: Implement spool.start
                if (!spool.isNull("end")) {
                    rawOptions.spoolEnd = spool.optString("end");
                }

            } else {
                LoggerUtilities.optionWarn(log, "JSONObject", "spool", configOpts.opt("spool"));
            }
        } else {
            // Deprecated
            if (!configOpts.isNull("perSpool")) {
                try { rawOptions.spoolSize = configOpts.getInt("perSpool"); }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "perSpool", configOpts.opt("perSpool")); }
            }
            if (!configOpts.isNull("endOfDoc")) {
                rawOptions.spoolEnd = configOpts.optString("endOfDoc", null);
            }
        }
        if (!configOpts.isNull("copies")) {
            try { rawOptions.copies = configOpts.getInt("copies"); }
            catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "copies", configOpts.opt("copies")); }
        }
        if (!configOpts.isNull("jobName")) {
            rawOptions.jobName = configOpts.optString("jobName", null);
        }
        if (!configOpts.isNull("retainTemp")) {
            rawOptions.retainTemp = configOpts.optBoolean("retainTemp", false);
        }


        //check for pixel options
        if (!configOpts.isNull("units")) {
            switch(configOpts.optString("units")) {
                case "mm":
                    psOptions.units = Unit.MM; break;
                case "cm":
                    psOptions.units = Unit.CM; break;
                case "in":
                    psOptions.units = Unit.INCH; break;
                default:
                    LoggerUtilities.optionWarn(log, "valid value", "units", configOpts.opt("units")); break;
            }
        }
        if (!configOpts.isNull("bounds")) {
            try {
                JSONObject bounds = configOpts.getJSONObject("bounds");
                psOptions.bounds = new Bounds(bounds.optDouble("x", 0), bounds.optDouble("y", 0), bounds.optDouble("width", 0), bounds.optDouble("height", 0));
            }
            catch(JSONException e) {
                LoggerUtilities.optionWarn(log, "JSONObject", "bounds", configOpts.opt("bounds"));
            }
        }
        if (!configOpts.isNull("colorType")) {
            try {
                psOptions.colorType = ColorType.valueOf(configOpts.optString("colorType").toUpperCase(Locale.ENGLISH));
            }
            catch(IllegalArgumentException e) {
                LoggerUtilities.optionWarn(log, "valid value", "colorType", configOpts.opt("colorType"));
            }
        }
        if (!configOpts.isNull("copies")) {
            try { psOptions.copies = configOpts.getInt("copies"); }
            catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "copies", configOpts.opt("copies")); }
            if (psOptions.copies < 1) {
                log.warn("Cannot have less than one copy");
                psOptions.copies = 1;
            }
        }
        if (!configOpts.isNull("density")) {
            JSONObject asymmDPI = configOpts.optJSONObject("density");
            if (asymmDPI != null) {
                psOptions.density = asymmDPI.optInt("feed");
                psOptions.crossDensity = asymmDPI.optInt("cross");
            } else {
                List<PrinterResolution> rSupport = output.isSetService()?
                        output.getNativePrinter().getResolutions():new ArrayList<>();

                JSONArray possibleDPIs = configOpts.optJSONArray("density");
                if (possibleDPIs != null && possibleDPIs.length() > 0) {
                    PrinterResolution usableRes = null;

                    if (!rSupport.isEmpty()) {
                        for(int i = 0; i < possibleDPIs.length(); i++) {
                            PrinterResolution compareRes;
                            asymmDPI = possibleDPIs.optJSONObject(i);
                            if (asymmDPI != null) {
                                compareRes = new PrinterResolution(asymmDPI.optInt("cross"), asymmDPI.optInt("feed"), psOptions.units.resSyntax);
                            } else {
                                compareRes = new PrinterResolution(possibleDPIs.optInt(i), possibleDPIs.optInt(i), psOptions.units.resSyntax);
                            }

                            if (rSupport.contains(compareRes)) {
                                usableRes = compareRes;
                                break;
                            }
                        }
                    }

                    if (usableRes == null) {
                        log.warn("Supported printer densities not found, using first value provided");
                        asymmDPI = possibleDPIs.optJSONObject(0);
                        if (asymmDPI != null) {
                            psOptions.density = asymmDPI.optInt("feed");
                            psOptions.crossDensity = asymmDPI.optInt("cross");
                        } else {
                            psOptions.density = possibleDPIs.optInt(0);
                        }
                    } else {
                        psOptions.density = usableRes.getFeedResolution(psOptions.units.resSyntax);
                        psOptions.crossDensity = usableRes.getCrossFeedResolution(psOptions.units.resSyntax);
                    }
                } else {
                    String relDPI = configOpts.optString("density", "").toLowerCase(Locale.ENGLISH);
                    if ("best".equals(relDPI)) {
                        PrinterResolution bestRes = null;
                        for(PrinterResolution pr : rSupport) {
                            if (bestRes == null || !pr.lessThanOrEquals(bestRes)) {
                                bestRes = pr;
                            }
                        }
                        if (bestRes != null) {
                            psOptions.density = bestRes.getFeedResolution(psOptions.units.resSyntax);
                            psOptions.crossDensity = bestRes.getCrossFeedResolution(psOptions.units.resSyntax);
                        } else {
                            log.warn("No print densities were found; density: \"{}\" is being ignored", relDPI);
                        }
                    } else if ("draft".equals(relDPI)) {
                        PrinterResolution lowestRes = null;
                        for(PrinterResolution pr : rSupport) {
                            if (lowestRes == null || pr.lessThanOrEquals(lowestRes)) {
                                lowestRes = pr;
                            }
                        }
                        if (lowestRes != null) {
                            psOptions.density = lowestRes.getFeedResolution(psOptions.units.resSyntax);
                            psOptions.crossDensity = lowestRes.getCrossFeedResolution(psOptions.units.resSyntax);
                        } else {
                            log.warn("No print densities were found; density: \"{}\" is being ignored", relDPI);
                        }
                    } else {
                        try { psOptions.density = configOpts.getDouble("density"); }
                        catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "density", configOpts.opt("density")); }
                    }
                }
            }
        }
        if (!configOpts.isNull("dithering")) {
            try {
                if (configOpts.getBoolean("dithering")) {
                    psOptions.dithering = RenderingHints.VALUE_DITHER_ENABLE;
                } else {
                    psOptions.dithering = RenderingHints.VALUE_DITHER_DISABLE;
                }
            }
            catch(JSONException e) { LoggerUtilities.optionWarn(log, "boolean", "dithering", configOpts.opt("dithering")); }
        }
        if (!configOpts.isNull("duplex")) {
            try {
                if (configOpts.getBoolean("duplex")) {
                    psOptions.duplex = Sides.DUPLEX;
                }
            }
            catch(JSONException e) {
                //not a boolean, try as a string
                try {
                    String duplex = configOpts.getString("duplex").toLowerCase(Locale.ENGLISH);
                    if (duplex.matches("^(duplex|(two.sided.)?long(.edge)?)$")) {
                        psOptions.duplex = Sides.DUPLEX;
                    } else if (duplex.matches("^(tumble|(two.sided.)?short(.edge)?)$")) {
                        psOptions.duplex = Sides.TUMBLE;
                    }
                    //else - one sided (default)
                }
                catch(JSONException e2) { LoggerUtilities.optionWarn(log, "valid value", "duplex", configOpts.opt("duplex")); }
            }
        }
        if (!configOpts.isNull("interpolation")) {
            switch(configOpts.optString("interpolation")) {
                case "bicubic":
                    psOptions.interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC; break;
                case "bilinear":
                    psOptions.interpolation = RenderingHints.VALUE_INTERPOLATION_BILINEAR; break;
                case "nearest-neighbor":
                case "nearest":
                    psOptions.interpolation = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR; break;
                default:
                    LoggerUtilities.optionWarn(log, "valid value", "interpolation", configOpts.opt("interpolation")); break;
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
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "margins.top", subMargins.opt("top")); }
                }
                if (!subMargins.isNull("right")) {
                    try { m.right = subMargins.getDouble("right"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "margins.right", subMargins.opt("right")); }
                }
                if (!subMargins.isNull("bottom")) {
                    try { m.bottom = subMargins.getDouble("bottom"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "margins.bottom", subMargins.opt("bottom")); }
                }
                if (!subMargins.isNull("left")) {
                    try { m.left = subMargins.getDouble("left"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "margins.left", subMargins.opt("left")); }
                }
            } else {
                try { m.setAll(configOpts.getDouble("margins")); }
                catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "margins", configOpts.opt("margins")); }
            }

            psOptions.margins = m;
        }
        if (!configOpts.isNull("orientation")) {
            try {
                psOptions.orientation = Orientation.valueOf(configOpts.optString("orientation").replaceAll("-", "_").toUpperCase(Locale.ENGLISH));
            }
            catch(IllegalArgumentException e) {
                LoggerUtilities.optionWarn(log, "valid value", "orientation", configOpts.opt("orientation"));
            }
        }
        if (!configOpts.isNull("paperThickness")) {
            try { psOptions.paperThickness = configOpts.getDouble("paperThickness"); }
            catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "paperThickness", configOpts.opt("paperThickness")); }
        }
        if (!configOpts.isNull("spool")) {
            JSONObject spool = configOpts.optJSONObject("spool");
            if (spool != null) {
                if (!spool.isNull("size")) {
                    try { psOptions.spoolSize = spool.getInt("size"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "integer", "spool.size", spool.opt("size")); }
                }
            } else {
                LoggerUtilities.optionWarn(log, "JSONObject", "spool", configOpts.opt("spool"));
            }
        }
        if (!configOpts.isNull("printerTray")) {
            psOptions.printerTray = configOpts.optString("printerTray", null);
            // Guard empty string value; will break pattern matching
            if(psOptions.printerTray != null && psOptions.printerTray.trim().equals("")) {
                psOptions.printerTray = null;
            }
        }
        if (!configOpts.isNull("rasterize")) {
            try { psOptions.rasterize = configOpts.getBoolean("rasterize"); }
            catch(JSONException e) { LoggerUtilities.optionWarn(log, "boolean", "rasterize", configOpts.opt("rasterize")); }
        }
        if (!configOpts.isNull("rotation")) {
            try { psOptions.rotation = configOpts.getDouble("rotation"); }
            catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "rotation", configOpts.opt("rotation")); }
        }
        if (!configOpts.isNull("scaleContent")) {
            try { psOptions.scaleContent = configOpts.getBoolean("scaleContent"); }
            catch(JSONException e) { LoggerUtilities.optionWarn(log, "boolean", "scaleContent", configOpts.opt("scaleContent")); }
        }
        if (!configOpts.isNull("size")) {
            Size s = new Size();
            JSONObject subSize = configOpts.optJSONObject("size");
            if (subSize != null) {
                if (!subSize.isNull("width")) {
                    try { s.width = subSize.getDouble("width"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "size.width", subSize.opt("width")); }
                }
                if (!subSize.isNull("height")) {
                    try { s.height = subSize.getDouble("height"); }
                    catch(JSONException e) { LoggerUtilities.optionWarn(log, "double", "size.height", subSize.opt("height")); }
                }

                if (s.height <= 0 && s.width <= 0) {
                    log.warn("Page size has been set without dimensions, using default");
                } else {
                    psOptions.size = s;
                }
            } else {
                LoggerUtilities.optionWarn(log, "JSONObject", "size", configOpts.opt("size"));
            }
        }

        //grab any useful service defaults
        PrinterResolution defaultRes = null;
        if (output.isSetService()) {
            defaultRes = output.getNativePrinter().getResolution().value();

            if (defaultRes == null) {
                //printer has no default resolution set, see if it is possible to pull anything
                List<PrinterResolution> rSupport = output.getNativePrinter().getResolutions();
                if (rSupport.size() > 0) {
                    defaultRes = rSupport.get(0);
                    log.warn("Default resolution for {} is missing, using fallback: {}", output.getNativePrinter().getName(), defaultRes);
                } else {
                    log.warn("Default resolution for {} is missing, no fallback available.", output.getNativePrinter().getName());
                }
            }
        }
        if (defaultRes != null) {
            //convert dphi to unit-dependant density ourselves (to keep as double type)
            defOptions.density = (double)defaultRes.getFeedResolution(1) / psOptions.getUnits().getDPIUnits();
        } else {
            try { defOptions.density = configOpts.getDouble("fallbackDensity"); }
            catch(JSONException e) {
                LoggerUtilities.optionWarn(log, "double", "fallbackDensity", configOpts.opt("fallbackDensity"));
                //manually convert default dphi to a density value based on units
                defOptions.density = 60000d / psOptions.getUnits().getDPIUnits();
            }
        }
        if ((psOptions.isRasterize() || format == PrintingUtilities.Format.IMAGE) && psOptions.getDensity() <= 1) {
            psOptions.density = defOptions.density;
            psOptions.crossDensity = defOptions.density;
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
        private boolean forceRaw = false;       //Alternate printing for linux systems
        private String destEncoding = null;     //Text encoding / charset
        private String srcEncoding = null;      //Conversion text encoding
        private String spoolEnd = null;         //End of document character(s)
        private int spoolSize = 1;              //Pages per spool
        private int copies = 1;                 //Job copies
        private String jobName = null;          //Job name
        private boolean retainTemp = false;     //Retain any temporary files


        public boolean isForceRaw() {
            return forceRaw;
        }

        public String getDestEncoding() {
            return destEncoding;
        }

        public String getSrcEncoding() {
            return srcEncoding;
        }

        public String getSpoolEnd() {
            return spoolEnd;
        }

        public int getSpoolSize() {
            return spoolSize;
        }

        public int getCopies() {
            return copies;
        }

        public boolean isRetainTemp() { return retainTemp; }

        public String getJobName(String defaultVal) {
            return jobName == null || jobName.isEmpty()? defaultVal:jobName;
        }
    }

    /** Pixel printing options */
    public class Pixel {
        private Bounds bounds = null;                                               //Bounding box rectangle
        private ColorType colorType = ColorType.COLOR;                              //Color / black&white
        private int copies = 1;                                                     //Job copies
        private double crossDensity = 0;                                            //Cross feed density
        private double density = 0;                                                 //Pixel density (DPI or DPMM), feed density if crossDensity is defined
        private Object dithering = RenderingHints.VALUE_DITHER_DEFAULT;             //Image dithering
        private Sides duplex = Sides.ONE_SIDED;                                     //Multi-siding
        private Object interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;  //Image interpolation
        private String jobName = null;                                              //Job name
        private boolean legacy = false;                                             //Legacy printing
        private Margins margins = new Margins();                                    //Page margins
        private Orientation orientation = null;                                     //Page orientation
        private double paperThickness = -1;                                         //Paper thickness
        private int spoolSize = 0;                                                   //Pages before sending to printer
        private String printerTray = null;                                          //Printer tray to use
        private boolean rasterize = true;                                           //Whether documents are rasterized before printing
        private double rotation = 0;                                                //Image rotation
        private boolean scaleContent = true;                                        //Adjust paper size for best image fit
        private Size size = null;                                                   //Paper size
        private Unit units = Unit.INCH;                                             //Units for density, margins, size


        public Bounds getBounds() {
            return bounds;
        }

        public ColorType getColorType() {
            return colorType;
        }

        public int getCopies() {
            return copies;
        }

        public double getCrossDensity() {
            return crossDensity;
        }

        public double getDensity() {
            return density;
        }

        public Object getDithering() {
            return dithering;
        }

        public Sides getDuplex() {
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

        public int getSpoolSize() {
            return spoolSize;
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

    /* Bounding box generic rectangle */
    public class Bounds {
        private double x;
        private double y;
        private double width;
        private double height;

        public Bounds(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getWidth() {
            return width;
        }

        public double getHeight() {
            return height;
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

        private final OrientationRequested orientationRequested;
        private final int orientationFormat;
        private final int degreesRot;

        Orientation(OrientationRequested orientationRequested, int orientationFormat, int degreesRot) {
            this.orientationRequested = orientationRequested;
            this.orientationFormat = orientationFormat;
            this.degreesRot = degreesRot;
        }


        public OrientationRequested getAsOrientRequested() {
            return orientationRequested;
        }

        public int getAsOrientFormat() {
            return orientationFormat;
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

        private final Chromaticity chromatic;

        ColorType(Chromaticity chromatic) {
            this.chromatic = chromatic;
        }


        public Chromaticity getAsChromaticity() {
            return chromatic;
        }
    }

}
