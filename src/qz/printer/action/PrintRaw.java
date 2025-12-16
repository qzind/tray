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

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.ByteArrayBuilder;
import qz.common.Constants;
import qz.exception.NullCommandException;
import qz.exception.NullPrintServiceException;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.LanguageType;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.printer.info.NativePrinter;
import qz.printer.status.CupsUtils;
import qz.utils.*;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.event.PrintJobEvent;
import javax.print.event.PrintJobListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
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

    private final ByteArrayBuilder commands;

    public enum Backend {
        CUPS_RSS,
        CUPS_LPR,
        WIN32_WMI
    }

    public PrintRaw() {
        commands = new ByteArrayBuilder();
    }

    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.COMMAND;
    }

    @Override
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
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

            PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", PrintingUtilities.Format.COMMAND.name()).toUpperCase(Locale.ENGLISH));
            PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.parse(data, PrintingUtilities.Flavor.PLAIN);
            PrintOptions.Raw rawOpts = options.getRawOptions();
            PrintOptions.Pixel pxlOpts = options.getPixelOptions();

            try {
                switch(format) {
                    case COMMAND:
                        switch(flavor) {
                            case PLAIN:
                                commands.append(ByteUtilities.toByteArray(cmd, rawOpts.getDestEncoding()));
                                break;
                            default:
                                commands.append(ByteUtilities.seekConversion(
                                        flavor.read(cmd, opt.optString("xmlTag", null)),
                                        rawOpts.getSrcEncoding(),
                                        rawOpts.getDestEncoding()
                                ));
                        }
                        break;
                    case HTML:
                    case IMAGE:
                    case PDF:
                        BufferedImage orig = format.newBiCreator().createBufferedImage(cmd, opt, flavor, rawOpts, pxlOpts);
                        BufferedImage oriented = applyOrientation(orig, pxlOpts);
                        ImageConverter converter = LanguageType.parse(opt.optString("language")).newImageConverter(oriented, opt);
                        converter.appendTo(commands);
                        break;
                    default:
                        throw new Exception(); // deliberately throw
                }
            }
            catch(Exception e) {
                throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s into a raw %s command: %s", flavor, data.getString("data"), format, e.getLocalizedMessage()), e);
            }
        }
    }

    /**
     * Rotate image using orientation or rotation before sending to ImageConverter
     */
    private BufferedImage applyOrientation(BufferedImage img, PrintOptions.Pixel pxlOpts) throws IOException {
        if(img == null) {
            throw new IOException("Image provided is empty or null and cannot be converted.");
        }

        if (pxlOpts.getOrientation() != null && pxlOpts.getOrientation() != PrintOptions.Orientation.PORTRAIT) {
            return PrintImage.rotate(img, pxlOpts.getOrientation().getDegreesRot(), pxlOpts.getDithering(), pxlOpts.getInterpolation());
        } else if (pxlOpts.getRotation() % 360 != 0) {
            return PrintImage.rotate(img, pxlOpts.getRotation(), pxlOpts.getDithering(), pxlOpts.getInterpolation());
        }
        return img;
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrintException {
        PrintOptions.Raw rawOpts = options.getRawOptions();

        List<ByteArrayBuilder> pages;
        if (rawOpts.getSpoolSize() > 0 && rawOpts.getSpoolEnd() != null && !rawOpts.getSpoolEnd().isEmpty()) {
            pages = ByteUtilities.splitByteArray(commands.toByteArray(), rawOpts.getSpoolEnd().getBytes(rawOpts.getDestEncoding()), rawOpts.getSpoolSize());
        } else {
            pages = new ArrayList<>();
            pages.add(commands);
        }

        List<File> tempFiles = null;
        for(int i = 0; i < rawOpts.getCopies(); i++) {
            for(int j = 0; j < pages.size(); j++) {
                ByteArrayBuilder bab = pages.get(j);
                try {
                    if (output.isSetHost()) {
                        printToHost(output.getHost(), output.getPort(), bab.toByteArray());
                    } else if (output.isSetFile()) {
                        printToFile(output.getFile(), bab.toByteArray(), true);
                    } else {
                        if (rawOpts.isForceRaw()) {
                            if(tempFiles == null) {
                                tempFiles = new ArrayList<>(pages.size());
                            }
                            File tempFile;
                            if(tempFiles.size() <= j) {
                                tempFile = File.createTempFile("qz_raw_", null);
                                tempFiles.add(j, tempFile);
                                printToFile(tempFile, bab.toByteArray(), false);
                            } else {
                                tempFile = tempFiles.get(j);
                            }
                            if(SystemUtilities.isWindows()) {
                                // Placeholder only; not yet supported
                                printToBackend(output.getNativePrinter(), tempFile, Backend.WIN32_WMI);
                            } else {
                                // Try CUPS backend first, fallback to LPR
                                printToBackend(output.getNativePrinter(), tempFile, Backend.CUPS_RSS, Backend.CUPS_LPR);
                            }
                        } else {
                            printToPrinter(output.getPrintService(), bab.toByteArray(), rawOpts);
                        }
                    }
                }
                catch(IOException e) {
                    cleanupTempFiles(rawOpts.isRetainTemp(), tempFiles);
                    throw new PrintException(e);
                }
            }
        }
        cleanupTempFiles(rawOpts.isRetainTemp(), tempFiles);
    }

    private void cleanupTempFiles(boolean retainTemp, List<File> tempFiles) {
        if(tempFiles != null) {
            if (!retainTemp) {
                for(File tempFile : tempFiles) {
                    if(tempFile != null) {
                        if(!tempFile.delete()) {
                            tempFile.deleteOnExit();
                        }
                    }
                }
            } else {
                log.warn("Temp file(s) retained: {}", Arrays.toString(tempFiles.toArray()));
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
    private void printToFile(File file, byte[] cmds, boolean locationRestricted) throws IOException {
        if(file == null) throw new IOException("No file specified");

        if(locationRestricted && !PrefsSearch.getBoolean(ArgValue.SECURITY_PRINT_TOFILE)) {
            log.error("Printing to file '{}' is not permitted.  Configure property '{}' to modify this behavior.",
                      file, ArgValue.SECURITY_PRINT_TOFILE.getMatch());
            throw new IOException(String.format("Printing to file '%s' is not permitted", file));
        }

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
     * Direct/backend printing modes for forced raw printing
     */
    public void printToBackend(NativePrinter printer, File tempFile, Backend... backends) throws IOException, PrintException {
        boolean success = false;

        for(Backend backend : backends) {
            switch(backend) {
                case CUPS_LPR:
                    // Use command line "lp" on Linux, BSD, Solaris, OSX, etc.
                    String[] lpCmd = new String[] {"lp", "-d", printer.getPrinterId(), "-o", "raw", tempFile.getAbsolutePath()};
                    if (!(success = ShellUtilities.execute(lpCmd))) {
                        log.debug(StringUtils.join(lpCmd, ' '));
                    }
                    break;
                case CUPS_RSS:
                    // Submit job via cupsDoRequest(...) via JNA against localhost:631\
                    success = CupsUtils.sendRawFile(printer, tempFile);
                    break;
                case WIN32_WMI:
                default:
                    throw new UnsupportedOperationException("Raw backend \"" + backend + "\" is not yet supported.");
            }
            if(success) {
                break;
            }
        }
        if (!success) {
            throw new PrintException("Forced raw printing failed");
        }
    }

    @Override
    public void cleanup() {
        commands.clear();
    }

}
