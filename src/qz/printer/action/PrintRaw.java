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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PrintRaw.class);

    private ByteArrayBuilder commands;

    private String encoding = null;


    public PrintRaw() {
        commands = new ByteArrayBuilder();
    }

    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.COMMAND;
    }

    private byte[] getBytes(String str, String encoding) throws ArabicShapingException, IOException {
        switch(encoding.toLowerCase()) {
            case "ibm864":
            case "cp864":
            case "csibm864":
            case "864":
            case "ibm-864":
                return ArabicConversionUtilities.convertToIBM864(str);
            default:
                return str.getBytes(encoding);
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

            encoding = rawOpts.getEncoding();
            if (encoding == null || encoding.isEmpty()) { encoding = Charset.defaultCharset().name(); }

            try {
                switch(format) {
                    case HTML:
                        commands.append(getHtmlWrapper(cmd, opt, flavor != PrintingUtilities.Flavor.PLAIN).getImageCommand(opt));
                        break;
                    case IMAGE:
                        commands.append(getImageWrapper(cmd, opt, flavor != PrintingUtilities.Flavor.BASE64).getImageCommand(opt));
                        break;
                    case PDF:
                        commands.append(getPdfWrapper(cmd, opt, flavor != PrintingUtilities.Flavor.BASE64).getImageCommand(opt));
                        break;
                    case COMMAND:
                    default:
                        switch(flavor) {
                            case BASE64:
                                commands.append(Base64.decodeBase64(cmd));
                                break;
                            case FILE:
                                commands.append(FileUtilities.readRawFile(cmd));
                                break;
                            case HEX:
                                commands.append(ByteUtilities.hexStringToByteArray(cmd));
                                break;
                            case XML:
                                commands.append(Base64.decodeBase64(FileUtilities.readXMLFile(cmd, opt.optString("xmlTag"))));
                                break;
                            case PLAIN:
                            default:
                                commands.append(getBytes(cmd, encoding));
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

    private ImageWrapper getImageWrapper(String data, JSONObject opt, boolean fromFile) throws IOException {
        BufferedImage bi;

        if (fromFile) {
            bi = ImageIO.read(ConnectionUtilities.getInputStream(data));
        } else {
            bi = ImageIO.read(new ByteArrayInputStream(Base64.decodeBase64(data)));
        }

        return getWrapper(bi, opt);
    }

    private ImageWrapper getPdfWrapper(String data, JSONObject opt, boolean fromFile) throws IOException {
        PDDocument doc;

        if (fromFile) {
            doc = PDDocument.load(ConnectionUtilities.getInputStream(data));
        } else {
            doc = PDDocument.load(new ByteArrayInputStream(Base64.decodeBase64(data)));
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
        return getWrapper(bi, opt);
    }

    private ImageWrapper getHtmlWrapper(String data, JSONObject opt, boolean fromFile) throws IOException {
        BufferedImage bi;

        try {
            WebApp.initialize(); //starts if not already started

            WebAppModel model = new WebAppModel(data, !fromFile, opt.optInt("pageWidth"), opt.optInt("pageHeight"), false, opt.optDouble("zoom"));
            bi = WebApp.raster(model);
        }
        catch(Throwable t) {
            log.error("Failed to capture html raster");
            throw new IOException(t);
        }

        return getWrapper(bi, opt);
    }

    private ImageWrapper getWrapper(BufferedImage img, JSONObject opt) {
        ImageWrapper iw = new ImageWrapper(img, LanguageType.getType(opt.optString("language")));
        iw.setCharset(Charset.forName(encoding));

        //ESC/POS only
        int density = opt.optInt("dotDensity", -1);
        if (density == -1) {
            String dStr = opt.optString("dotDensity", null);
            if (dStr != null && !dStr.isEmpty()) {
                switch(dStr.toLowerCase()) {
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
        if (rawOpts.getPerSpool() > 0 && rawOpts.getEndOfDoc() != null && !rawOpts.getEndOfDoc().isEmpty()) {
            try {
                pages = ByteUtilities.splitByteArray(commands.getByteArray(), rawOpts.getEndOfDoc().getBytes(encoding), rawOpts.getPerSpool());
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
        encoding = null;
    }

}
