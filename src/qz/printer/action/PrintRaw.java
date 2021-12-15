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

import com.ibm.icu.text.ArabicShapingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.ssl.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import qz.common.ByteArrayBuilder;
import qz.common.Constants;
import qz.exception.NullCommandException;
import qz.exception.NullPrintServiceException;
import qz.printer.ImageWrapper;
import qz.printer.LanguageType;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.info.NativePrinter;
import qz.utils.*;

import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends raw data to the printer, overriding your operating system's print
 * driver. Most useful for printers such as zebra card or barcode printers.
 *
 * @author A. Tres Finocchiaro
 */
public class PrintRaw implements PrintProcessor {

    private static final Logger log = LogManager.getLogger(PrintRaw.class);

    private ByteArrayBuilder commands;

    private String destEncoding = null;


    public PrintRaw() {
        commands = new ByteArrayBuilder();
    }

    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.COMMAND;
    }

    private byte[] getBytes(String str, String destEncoding) throws ArabicShapingException, IOException {
        switch(destEncoding.toLowerCase(Locale.ENGLISH)) {
            case "ibm864":
            case "cp864":
            case "csibm864":
            case "864":
            case "ibm-864":
                return ArabicConversionUtilities.convertToIBM864(str);
            default:
                return str.getBytes(destEncoding);
        }
    }


    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.optJSONObject(i);
            if (data == null) {
                data = new JSONObject();
                data.put("data", printData.getString(i));
            }

            String cmd = data.getString("data");
            JSONObject opt = data.optJSONObject("options");
            if (opt == null) { opt = new JSONObject(); }

            PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", "COMMAND").toUpperCase(Locale.ENGLISH));
            PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.valueOf(data.optString("flavor", "PLAIN").toUpperCase(Locale.ENGLISH));
            PrintOptions.Raw rawOpts = options.getRawOptions();
            PrintOptions.Pixel pxlOpts = options.getPixelOptions();

            destEncoding = rawOpts.getDestEncoding();
            if (destEncoding == null || destEncoding.isEmpty()) { destEncoding = Charset.defaultCharset().name(); }

            try {
                switch(format) {
                    case HTML:
                        commands.append(getHtmlWrapper(cmd, opt, flavor, rawOpts, pxlOpts).getImageCommand(opt));
                        break;
                    case IMAGE:
                        commands.append(getImageWrapper(cmd, opt, flavor, rawOpts, pxlOpts).getImageCommand(opt));
                        break;
                    case PDF:
                        commands.append(getPdfWrapper(cmd, opt, flavor, rawOpts, pxlOpts).getImageCommand(opt));
                        break;
                    case COMMAND:
                    default:
                        switch(flavor) {
                            case BASE64:
                                commands.append(seekConversion(Base64.decodeBase64(cmd), rawOpts));
                                break;
                            case FILE:
                                commands.append(seekConversion(FileUtilities.readRawFile(cmd), rawOpts));
                                break;
                            case HEX:
                                commands.append(seekConversion(ByteUtilities.hexStringToByteArray(cmd), rawOpts));
                                break;
                            case XML:
                                commands.append(seekConversion(Base64.decodeBase64(FileUtilities.readXMLFile(cmd, opt.optString("xmlTag"))), rawOpts));
                                break;
                            case PLAIN:
                            default:
                                commands.append(getBytes(cmd, destEncoding));
                                break;
                        }
                        break;
                }
            }
            catch(Exception e) {
                throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s into a raw %s command: %s", flavor, data.getString("data"), format, e.getLocalizedMessage()), e);
            }
        }
    }

    private byte[] seekConversion(byte[] rawBytes, PrintOptions.Raw rawOpts) {
        if (rawOpts.getSrcEncoding() != null) {
            if(rawOpts.getSrcEncoding().equals(rawOpts.getDestEncoding())) {
                log.warn("Provided srcEncoding and destEncoding are the same, skipping");
            } else {
                try {
                    String rawConvert = new String(rawBytes, rawOpts.getSrcEncoding());
                    return rawConvert.getBytes(rawOpts.getDestEncoding());
                }
                catch(UnsupportedEncodingException e) {
                    throw new UnsupportedOperationException(e);
                }
            }
        }
        return rawBytes;
    }

    private ImageWrapper getImageWrapper(String data, JSONObject opt, PrintingUtilities.Flavor flavor, PrintOptions.Raw rawOpts, PrintOptions.Pixel pxlOpts) throws IOException {
        BufferedImage bi;
        // 2.0 compat
        if (data.startsWith("data:image/") && data.contains(";base64,")) {
            String[] parts = data.split(";base64,");
            data = parts[parts.length - 1];
            flavor = PrintingUtilities.Flavor.BASE64;
        }

        if (flavor == PrintingUtilities.Flavor.BASE64) {
            bi = ImageIO.read(new ByteArrayInputStream(seekConversion(Base64.decodeBase64(data), rawOpts)));
        } else {
            bi = ImageIO.read(ConnectionUtilities.getInputStream(data));
        }

        return getWrapper(bi, opt, pxlOpts);
    }

    private ImageWrapper getPdfWrapper(String data, JSONObject opt, PrintingUtilities.Flavor flavor, PrintOptions.Raw rawOpts, PrintOptions.Pixel pxlOpts) throws IOException {
        PDDocument doc;

        if (flavor == PrintingUtilities.Flavor.BASE64) {
            doc = PDDocument.load(new ByteArrayInputStream(seekConversion(Base64.decodeBase64(data), rawOpts)));
        } else {
            doc = PDDocument.load(ConnectionUtilities.getInputStream(data));
        }

        double scale;
        PDRectangle rect = doc.getPage(0).getBBox();
        double pw = opt.optDouble("pageWidth", 0), ph = opt.optDouble("pageHeight", 0);
        if (ph <= 0 || (pw > 0 && (rect.getWidth() / rect.getHeight()) >= (pw / ph))) {
            scale = pw / rect.getWidth();
        } else {
            scale = ph / rect.getHeight();
        }
        if (scale <= 0) { scale = 1.0; }

        BufferedImage bi = new PDFRenderer(doc).renderImage(0, (float)scale);
        return getWrapper(bi, opt, pxlOpts);
    }

    private ImageWrapper getHtmlWrapper(String data, JSONObject opt, PrintingUtilities.Flavor flavor, PrintOptions.Raw rawOpts, PrintOptions.Pixel pxlOpts) throws IOException {
        if (flavor == PrintingUtilities.Flavor.BASE64) {
            data = new String(seekConversion(Base64.decodeBase64(data), rawOpts), rawOpts.getDestEncoding());
        }

        double density = (pxlOpts.getDensity() * pxlOpts.getUnits().as1Inch());
        if (density <= 1) {
            density = LanguageType.getType(opt.optString("language")).getDefaultDensity();
        }
        double pageZoom = density / 72.0;

        double pageWidth = opt.optInt("pageWidth") / density * 72;
        double pageHeight = opt.optInt("pageHeight") / density * 72;

        BufferedImage bi;
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

        return getWrapper(bi, opt, pxlOpts);
    }

    private ImageWrapper getWrapper(BufferedImage img, JSONObject opt, PrintOptions.Pixel pxlOpts) {
        // Rotate image using orientation or rotation before sending to ImageWrapper
        if (pxlOpts.getOrientation() != null && pxlOpts.getOrientation() != PrintOptions.Orientation.PORTRAIT) {
            img = PrintImage.rotate(img, pxlOpts.getOrientation().getDegreesRot(), pxlOpts.getDithering(), pxlOpts.getInterpolation());
        } else if (pxlOpts.getRotation() % 360 != 0) {
            img = PrintImage.rotate(img, pxlOpts.getRotation(), pxlOpts.getDithering(), pxlOpts.getInterpolation());
        }

        ImageWrapper iw = new ImageWrapper(img, LanguageType.getType(opt.optString("language")));
        iw.setCharset(Charset.forName(destEncoding));

        //ESC/POS only
        int density = opt.optInt("dotDensity", -1);
        if (density == -1) {
            String dStr = opt.optString("dotDensity", null);
            if (dStr != null && !dStr.isEmpty()) {
                switch(dStr.toLowerCase(Locale.ENGLISH)) {
                    case "single": density = 32; break;
                    case "double": density = 33; break;
                    case "triple": density = 39; break;
                }
            } else {
                density = 32; //default
            }
        }
        iw.setDotDensity(density);

        //EPL only
        iw.setxPos(opt.optInt("x", 0));
        iw.setyPos(opt.optInt("y", 0));

        return iw;
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrintException {
        PrintOptions.Raw rawOpts = options.getRawOptions();

        List<ByteArrayBuilder> pages;
        if (rawOpts.getSpoolSize() > 0 && rawOpts.getSpoolEnd() != null && !rawOpts.getSpoolEnd().isEmpty()) {
            try {
                pages = ByteUtilities.splitByteArray(commands.getByteArray(), rawOpts.getSpoolEnd().getBytes(destEncoding), rawOpts.getSpoolSize());
            }
            catch(UnsupportedEncodingException e) {
                throw new PrintException(e);
            }
        } else {
            pages = new ArrayList<>();
            pages.add(commands);
        }

        for(int i = 0; i < rawOpts.getCopies(); i++) {
            for(ByteArrayBuilder bab : pages) {
                try {
                    if (output.isSetHost()) {
                        printToHost(output.getHost(), output.getPort(), bab.getByteArray());
                    } else if (output.isSetFile()) {
                        printToFile(output.getFile(), bab.getByteArray());
                    } else {
                        if (rawOpts.isAltPrinting()) {
                            printToAlternate(output.getNativePrinter(), bab.getByteArray());
                        } else {
                            printToPrinter(output.getPrintService(), bab.getByteArray(), rawOpts);
                        }
                    }
                }
                catch(IOException e) {
                    throw new PrintException(e);
                }
            }
        }
    }

    /**
     * A brute-force, however surprisingly elegant way to send a file to a networked printer.
     * <p/>
     * Please note that this will completely bypass the Print Spooler,
     * so the Operating System will have absolutely no printer information.
     * This is printing "blind".
     */
    private void printToHost(String host, int port, byte[] cmds) throws IOException {
        log.debug("Printing to host {}:{}", host, port);

        //throws any exception and auto-closes socket and stream
        try(Socket socket = new Socket(host, port); DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            out.write(cmds);
        }
    }

    /**
     * Writes the raw commands directly to a file.
     *
     * @param file File to be written
     */
    private void printToFile(File file, byte[] cmds) throws IOException {
        log.debug("Printing to file: {}", file.getName());

        //throws any exception and auto-closes stream
        try(OutputStream out = new FileOutputStream(file)) {
            out.write(cmds);
        }
    }

    /**
     * Constructs a {@code SimpleDoc} with the {@code commands} byte array.
     */
    private void printToPrinter(PrintService service, byte[] cmds, PrintOptions.Raw rawOpts) throws PrintException {
        if (service == null) { throw new NullPrintServiceException("Service cannot be null"); }
        if (cmds == null || cmds.length == 0) { throw new NullCommandException("No commands found to send to the printer"); }

        SimpleDoc doc = new SimpleDoc(cmds, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);

        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add(new JobName(rawOpts.getJobName(Constants.RAW_PRINT), Locale.getDefault()));

        DocPrintJob printJob = service.createPrintJob();

        waitForPrint(printJob, doc, attributes);
    }

    protected void waitForPrint(DocPrintJob printJob, Doc doc, PrintRequestAttributeSet attributes) throws PrintException {
        final AtomicBoolean finished = new AtomicBoolean(false);
        printJob.addPrintJobListener(new PrintJobListener() {
            @Override
            public void printDataTransferCompleted(PrintJobEvent printJobEvent) {
                log.debug("{}", printJobEvent);
                finished.set(true);
            }

            @Override
            public void printJobCompleted(PrintJobEvent printJobEvent) {
                log.debug("{}", printJobEvent);
                finished.set(true);
            }

            @Override
            public void printJobFailed(PrintJobEvent printJobEvent) {
                log.error("{}", printJobEvent);
                finished.set(true);
            }

            @Override
            public void printJobCanceled(PrintJobEvent printJobEvent) {
                log.warn("{}", printJobEvent);
                finished.set(true);
            }

            @Override
            public void printJobNoMoreEvents(PrintJobEvent printJobEvent) {
                log.debug("{}", printJobEvent);
                finished.set(true);
            }

            @Override
            public void printJobRequiresAttention(PrintJobEvent printJobEvent) {
                log.info("{}", printJobEvent);
            }
        });

        log.trace("Sending print job to printer");
        printJob.print(doc, attributes);

        while(!finished.get()) {
            try { Thread.sleep(100); } catch(Exception ignore) {}
        }

        log.trace("Print job received by printer");
    }

    /**
     * Alternate printing mode for CUPS capable OSs, issues lp via command line
     * on Linux, BSD, Solaris, OSX, etc. This will never work on Windows.
     */
    public void printToAlternate(NativePrinter printer, byte[] cmds) throws IOException, PrintException {
        File tmp = File.createTempFile("qz_raw_", null);
        try {
            printToFile(tmp, cmds);
            String[] lpCmd = new String[] {
                    "lp", "-d", printer.getPrinterId(), "-o", "raw", tmp.getAbsolutePath()
            };
            boolean success = ShellUtilities.execute(lpCmd);

            if (!success) {
                throw new PrintException("Alternate printing failed: " + StringUtils.join(lpCmd, ' '));
            }
        }
        finally {
            if (!tmp.delete()) {
                tmp.deleteOnExit();
            }
        }
    }

    @Override
    public void cleanup() {
        commands.clear();
        destEncoding = null;
    }

}
