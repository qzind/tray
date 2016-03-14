/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 *
 */
package qz.printer.action;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sourceforge.iharder.Base64;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.PrintingUtilities;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.print.attribute.PrintRequestAttributeSet;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Tres Finocchiaro, Anton Mezerny
 */
public class PrintImage extends PrintPixel implements PrintProcessor, Printable {

    private static final Logger log = LoggerFactory.getLogger(PrintImage.class);

    protected List<BufferedImage> images;

    protected boolean scaleImage = false;
    protected Object interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
    protected double imageRotation = 0;


    public PrintImage() {
        images = new ArrayList<>();
    }


    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.getJSONObject(i);

            PrintingUtilities.Format format = PrintingUtilities.Format.valueOf(data.optString("format", "FILE").toUpperCase());

            try {
                BufferedImage bi;
                if (format == PrintingUtilities.Format.BASE64) {
                    bi = ImageIO.read(new ByteArrayInputStream(Base64.decode(data.getString("data"))));
                } else {
                    bi = ImageIO.read(new URL(data.getString("data")));
                }

                images.add(bi);
            }
            catch(IIOException e) {
                if (e.getCause() != null && e.getCause() instanceof FileNotFoundException) {
                    throw new UnsupportedOperationException("Image file specified could not be found.", e);
                } else {
                    throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as an image", format, data.getString("data")), e);
                }
            }
            catch(IOException e) {
                throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as an image", format, data.getString("data")), e);
            }
        }

        log.debug("Parsed {} images for printing", images.size());
    }

    @Override
    public void print(PrintOutput output, PrintOptions options) throws PrinterException {
        if (images.isEmpty()) {
            log.warn("Nothing to print");
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(output.getPrintService());
        PageFormat page = job.getPageFormat(null);

        PrintOptions.Pixel pxlOpts = options.getPixelOptions();
        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, page);

        scaleImage = pxlOpts.isScaleContent();
        interpolation = pxlOpts.getInterpolation();
        imageRotation = pxlOpts.getRotation();

        job.setJobName(pxlOpts.getJobName(Constants.IMAGE_PRINT));
        job.setPrintable(this, job.validatePage(page));

        printCopies(output, pxlOpts, job, attributes);
    }


    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (graphics == null) { throw new PrinterException("No graphics specified"); }
        if (pageFormat == null) { throw new PrinterException("No page format specified"); }

        if (pageIndex + 1 > images.size()) {
            return NO_SUCH_PAGE;
        }
        log.trace("Requested page {} for printing", pageIndex);


        BufferedImage imgToPrint = images.get(pageIndex);
        imgToPrint = fixColorModel(imgToPrint);
        if (imageRotation % 360 != 0) {
            imgToPrint = rotate(imgToPrint, imageRotation);
        }

        Graphics2D graphics2D = (Graphics2D)graphics;
        // Suggested by Bahadir 8/23/2012
        graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        log.trace("{}", graphics2D.getRenderingHints());


        // apply image scaling
        double boundW = pageFormat.getImageableWidth();
        double boundH = pageFormat.getImageableHeight();

        int imgW = imgToPrint.getWidth();
        int imgH = imgToPrint.getHeight();

        if (scaleImage) {
            // scale image to smallest edge, keeping size ratio
            if (((float)imgToPrint.getWidth() / (float)imgToPrint.getHeight()) >= (boundW / boundH)) {
                imgW = (int)boundW;
                imgH = (int)(imgToPrint.getHeight() / (imgToPrint.getWidth() / boundW));
            } else {
                imgW = (int)(imgToPrint.getWidth() / (imgToPrint.getHeight() / boundH));
                imgH = (int)boundH;
            }
        }

        double boundX = pageFormat.getImageableX();
        double boundY = pageFormat.getImageableY();

        log.debug("Paper area: {},{}:{},{}", (int)boundX, (int)boundY, (int)boundW, (int)boundH);
        log.trace("Image size: {},{}", imgW, imgH);

        // Now we perform our rendering
        graphics2D.drawImage(imgToPrint, (int)boundX, (int)boundY, (int)boundX + imgW, (int)boundY + imgH,
                             0, 0, imgToPrint.getWidth(), imgToPrint.getHeight(), null);

        // Valid page
        return PAGE_EXISTS;
    }

    /**
     * Rotates {@code image} by the specified {@code angle}.
     *
     * @param image BufferedImage to rotate
     * @param angle Rotation angle in degrees
     * @return Rotated image data
     */
    private static BufferedImage rotate(BufferedImage image, double angle) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));

        int sWidth = image.getWidth(), sHeight = image.getHeight();
        int eWidth = (int)Math.floor((sWidth * cos) + (sHeight * sin)), eHeight = (int)Math.floor((sHeight * cos) + (sWidth * sin));

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDefaultConfiguration();
        BufferedImage result = gc.createCompatibleImage(eWidth, eHeight, Transparency.TRANSLUCENT);

        Graphics2D g2d = result.createGraphics();
        g2d.translate((eWidth - sWidth) / 2, (eHeight - sHeight) / 2);
        g2d.rotate(rads, sWidth / 2, sHeight / 2);
        g2d.drawRenderedImage(image, null);
        g2d.dispose();

        return result;
    }

}
