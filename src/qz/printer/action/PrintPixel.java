package qz.printer.action;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.SystemUtilities;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PrintPixel {

    private static final Logger log = LoggerFactory.getLogger(PrintPixel.class);

    private static final List<Integer> MAC_BAD_IMAGE_TYPES = Arrays.asList(BufferedImage.TYPE_BYTE_BINARY, BufferedImage.TYPE_CUSTOM);


    protected PrintRequestAttributeSet applyDefaultSettings(PrintOptions.Pixel pxlOpts, PageFormat page, Media[] supported) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();

        //apply general attributes
        if (pxlOpts.getColorType() != null) {
            attributes.add(pxlOpts.getColorType().getAsChromaticity());
        }
        attributes.add(pxlOpts.getDuplex());
        if (pxlOpts.getOrientation() != null) {
            attributes.add(pxlOpts.getOrientation().getAsOrientRequested());
        }
        if (pxlOpts.getPrinterTray() != null && !pxlOpts.getPrinterTray().isEmpty()) {
            Pattern exactPattern = Pattern.compile("\\b" + Pattern.quote(pxlOpts.getPrinterTray()) + "\\b", Pattern.CASE_INSENSITIVE);
            Pattern fuzzyPattern = Pattern.compile("\\b.*?[" + Pattern.quote(pxlOpts.getPrinterTray()) +"]+.*?\\b", Pattern.CASE_INSENSITIVE);
            Media bestFit = null;
            Integer fuzzyFitDelta = null;

            for(Media m : supported) {
                if (m instanceof MediaTray) {
                    Matcher exactly = exactPattern.matcher(m.toString().trim());
                    Matcher fuzzily = fuzzyPattern.matcher(m.toString().trim());

                    if (exactly.find()) {
                        bestFit = m;
                        break;
                    }

                    while(fuzzily.find()) {
                        //look for as close to exact match as possible
                        int delta = Math.abs(fuzzily.group().length() - pxlOpts.getPrinterTray().length());
                        if (fuzzyFitDelta == null || delta < fuzzyFitDelta) {
                            fuzzyFitDelta = delta;
                            bestFit = m;
                        }
                    }
                }
            }

            if (bestFit != null) {
                attributes.add(bestFit);
            }
        }

        //TODO - set paper thickness


        // Java prints using inches at 72dpi
        final float CONVERT = pxlOpts.getUnits().toInches() * 72f;

        log.trace("DPI: [{}x{}]\tCNV: {}", pxlOpts.getDensity(), pxlOpts.getCrossDensity(), CONVERT);
        if (pxlOpts.getDensity() > 0) {
            double cross = pxlOpts.getCrossDensity();
            if (cross == 0) { cross = pxlOpts.getDensity(); }

            attributes.add(new PrinterResolution((int)cross, (int)pxlOpts.getDensity(), pxlOpts.getUnits().getDPIUnits()));
        }

        //apply sizing and margins
        Paper paper = page.getPaper();

        float pageX = 0f;
        float pageY = 0f;
        float pageW = (float)page.getWidth() / CONVERT;
        float pageH = (float)page.getHeight() / CONVERT;

        //page size
        if (pxlOpts.getSize() != null && pxlOpts.getSize().getWidth() > 0 && pxlOpts.getSize().getHeight() > 0) {
            pageW = (float)pxlOpts.getSize().getWidth();
            pageH = (float)pxlOpts.getSize().getHeight();

            paper.setSize(pageW * CONVERT, pageH * CONVERT);
        }

        //margins
        if (pxlOpts.getMargins() != null) {
            pageX += pxlOpts.getMargins().left();
            pageY += pxlOpts.getMargins().top();
            pageW -= (pxlOpts.getMargins().right() + pxlOpts.getMargins().left());
            pageH -= (pxlOpts.getMargins().bottom() + pxlOpts.getMargins().top());
        }

        log.trace("Drawable area: {},{}:{},{}", pageX, pageY, pageW, pageH);
        if (pageW > 0 && pageH > 0) {
            attributes.add(new MediaPrintableArea(pageX, pageY, pageW, pageH, pxlOpts.getUnits().getMediaSizeUnits()));
            paper.setImageableArea(pageX * CONVERT, pageY * CONVERT, pageW * CONVERT, pageH * CONVERT);
            page.setPaper(paper);
        } else {
            log.warn("Could not apply custom size, using printer default");
            attributes.add(new MediaPrintableArea(0, 0, (float)page.getWidth() / 72f, (float)page.getHeight() / 72f, PrintOptions.Unit.INCH.getMediaSizeUnits()));
        }

        log.trace("{}", Arrays.toString(attributes.toArray()));

        return attributes;
    }


    protected void printCopies(PrintOutput output, PrintOptions.Pixel pxlOpts, PrinterJob job, PrintRequestAttributeSet attributes) throws PrinterException {
        log.info("Starting printing ({} copies)", pxlOpts.getCopies());

        PrinterResolution rUsing = (PrinterResolution)attributes.get(PrinterResolution.class);
        if (rUsing != null) {
            List<PrinterResolution> rSupport = output.getNativePrinter().getResolutions();
            if (!rSupport.isEmpty()) {
                if (!rSupport.contains(rUsing)) {
                    log.warn("Not using a supported DPI for printing");
                    log.debug("Available DPI: {}", ArrayUtils.toString(rSupport));
                }
            } else {
                log.warn("Supported printer densities not found");
            }
        }

        CopiesSupported cSupport = (CopiesSupported)output.getPrintService()
                .getSupportedAttributeValues(Copies.class, output.getPrintService().getSupportedDocFlavors()[0], attributes);

        if (cSupport != null && cSupport.contains(pxlOpts.getCopies())) {
            attributes.add(new Copies(pxlOpts.getCopies()));
            job.print(attributes);
        } else {
            for(int i = 0; i < pxlOpts.getCopies(); i++) {
                job.print(attributes);
            }
        }
    }

    /**
     * FIXME:  Temporary fix for OS X 10.10 hard crash.
     * See https://github.com/qzind/qz-print/issues/75
     */
    protected BufferedImage fixColorModel(BufferedImage imgToPrint) {
        if (SystemUtilities.isMac()) {
            if (MAC_BAD_IMAGE_TYPES.contains(imgToPrint.getType())) {
                BufferedImage sanitizedImage;
                ColorModel cm = imgToPrint.getColorModel();

                if (cm instanceof IndexColorModel) {
                    log.info("Image converted to 256 colors for OSX 10.10 Workaround");
                    sanitizedImage = new BufferedImage(imgToPrint.getWidth(), imgToPrint.getHeight(), BufferedImage.TYPE_BYTE_INDEXED, (IndexColorModel)cm);
                } else {
                    log.info("Image converted to ARGB for OSX 10.10 Workaround");
                    sanitizedImage = new BufferedImage(imgToPrint.getWidth(), imgToPrint.getHeight(), BufferedImage.TYPE_INT_ARGB);
                }

                sanitizedImage.createGraphics().drawImage(imgToPrint, 0, 0, null);
                imgToPrint = sanitizedImage;
            }
        }

        return imgToPrint;
    }

    protected static Map<RenderingHints.Key,Object> buildRenderingHints(Object dithering, Object interpolation) {
        Map<RenderingHints.Key,Object> rhMap = new HashMap<>();
        rhMap.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        rhMap.put(RenderingHints.KEY_DITHERING, dithering);
        rhMap.put(RenderingHints.KEY_INTERPOLATION, interpolation);
        rhMap.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        rhMap.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        rhMap.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        if (RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR.equals(interpolation)) {
            rhMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            rhMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        } else {
            rhMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            rhMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }


        return rhMap;
    }

}
