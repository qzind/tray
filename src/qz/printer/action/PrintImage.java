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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Constants;
import qz.printer.PrintOptions;
import qz.printer.PrintOutput;
import qz.utils.ConnectionUtilities;
import qz.utils.PrintingUtilities;
import qz.utils.SystemUtilities;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrinterResolution;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Tres Finocchiaro, Anton Mezerny
 */
public class PrintImage extends PrintPixel implements PrintProcessor, Printable {

    private static final Logger log = LogManager.getLogger(PrintImage.class);

    protected List<BufferedImage> images;

    protected double dpiScale = 1;
    protected boolean scaleImage = false;
    protected Object dithering = RenderingHints.VALUE_DITHER_DEFAULT;
    protected Object interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
    protected double imageRotation = 0;
    protected boolean manualReverse = false;

    public PrintImage() {
        images = new ArrayList<>();
    }

    @Override
    public PrintingUtilities.Format getFormat() {
        return PrintingUtilities.Format.IMAGE;
    }

    @Override
    public void parseData(JSONArray printData, PrintOptions options) throws JSONException, UnsupportedOperationException {
        dpiScale = (options.getPixelOptions().getDensity() * options.getPixelOptions().getUnits().as1Inch()) / 72.0;

        for(int i = 0; i < printData.length(); i++) {
            JSONObject data = printData.getJSONObject(i);

            PrintingUtilities.Flavor flavor = PrintingUtilities.Flavor.parse(data, PrintingUtilities.Flavor.FILE);

            try {
                images.add(loadImage(data.getString("data"), flavor));
            }
            catch(IIOException e) {
                if (e.getCause() != null && e.getCause() instanceof FileNotFoundException) {
                    throw new UnsupportedOperationException("Image file specified could not be found.", e);
                } else {
                    throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as an image", flavor, data.getString("data")), e);
                }
            }
            catch(IOException e) {
                throw new UnsupportedOperationException(String.format("Cannot parse (%s)%s as an image: %s", flavor, data.getString("data"), e.getLocalizedMessage()), e);
            }
        }

        log.debug("Parsed {} images for printing", images.size());
    }

    private BufferedImage loadImage(String data, PrintingUtilities.Flavor flavor) throws IOException {
        // 2.0 compatibility, base64 was inferred by URL pattern
        if (data.startsWith("data:image/") && data.contains(";base64,")) {
            String[] parts = data.split(";base64,");
            data = parts[parts.length - 1];
            flavor = PrintingUtilities.Flavor.BASE64;
        }

        switch(flavor) {
            case PLAIN:
                // There's really no such thing as a 'PLAIN' image, assume it's a URL
            case FILE:
                return ImageIO.read(ConnectionUtilities.getInputStream(data, true));
            default:
                return ImageIO.read(new ByteArrayInputStream(flavor.read(data)));
        }
    }

    private List<BufferedImage> breakupOverPages(BufferedImage img, PageFormat page, PrintRequestAttributeSet attributes) {
        List<BufferedImage> splits = new ArrayList<>();

        Rectangle printBounds = new Rectangle(0, 0, (int)page.getImageableWidth(), (int)page.getImageableHeight());
        PrinterResolution res = (PrinterResolution)attributes.get(PrinterResolution.class);
        float dpi = res.getFeedResolution(1) / (float)ResolutionSyntax.DPI;
        float cdpi = res.getCrossFeedResolution(1) / (float)ResolutionSyntax.DPI;

        //printing uses 72dpi, convert so we can check split size correctly
        int useWidth = (int)((img.getWidth() / cdpi) * 72);
        int useHeight = (int)((img.getHeight() / dpi) * 72);

        int columnsNeed = (int)Math.ceil(useWidth / page.getImageableWidth());
        int rowsNeed = (int)Math.ceil(useHeight / page.getImageableHeight());

        if (columnsNeed == 1 && rowsNeed == 1) {
            log.trace("Unscaled image does not need spit");
            splits.add(img);
        } else {
            log.trace("Image to be printed across {} pages", columnsNeed * rowsNeed);
            //allows us to split the image at the actual dpi instead of 72
            float upscale = dpi / 72f;
            float c_upscale = cdpi / 72f;

            for(int row = 0; row < rowsNeed; row++) {
                for(int col = 0; col < columnsNeed; col++) {
                    Rectangle clip = new Rectangle((col * (int)(printBounds.width * c_upscale)), (row * (int)(printBounds.height * upscale)),
                                                   (int)(printBounds.width * c_upscale), (int)(printBounds.height * upscale));

                    if (clip.x + clip.width > img.getWidth()) { clip.width = img.getWidth() - clip.x; }
                    if (clip.y + clip.height > img.getHeight()) { clip.height = img.getHeight() - clip.y; }

                    splits.add(img.getSubimage(clip.x, clip.y, clip.width, clip.height));
                }
            }
        }

        return splits;
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
        PrintRequestAttributeSet attributes = applyDefaultSettings(pxlOpts, page, output.getSupportedMedia());

        scaleImage = pxlOpts.isScaleContent();
        dithering = pxlOpts.getDithering();
        interpolation = pxlOpts.getInterpolation();
        imageRotation = pxlOpts.getRotation();

        //reverse fix for OSX
        if (SystemUtilities.isMac() && pxlOpts.getOrientation() != null
                && pxlOpts.getOrientation().getAsOrientRequested() == OrientationRequested.REVERSE_LANDSCAPE) {
            imageRotation += 180;
            manualReverse = true;
        }

        if (!scaleImage) {
            //breakup large images to print across pages as needed
            List<BufferedImage> split = new ArrayList<>();
            for(BufferedImage bi : images) {
                split.addAll(breakupOverPages(bi, page, attributes));
            }
            images = split;
        }

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

        if ("sun.print.PeekGraphics".equals(graphics.getClass().getCanonicalName())) {
            //java uses class only to query if a page needs printed - save memory/time by short circuiting
            return PAGE_EXISTS;
        }


        //allows pages view to rotate in different orientations
        graphics.drawString(" ", 0, 0);

        BufferedImage imgToPrint = fixColorModel(images.get(pageIndex));
        if (imageRotation % 360 != 0) {
            imgToPrint = rotate(imgToPrint, imageRotation);
        }

        // apply image scaling
        double boundW = pageFormat.getImageableWidth();
        double boundH = pageFormat.getImageableHeight();

        double imgW = imgToPrint.getWidth() / dpiScale;
        double imgH = imgToPrint.getHeight() / dpiScale;

        if (scaleImage) {
            imgToPrint = scale(imgToPrint, pageFormat);

            // adjust dimensions to smallest edge, keeping size ratio
            if (((float)imgToPrint.getWidth() / (float)imgToPrint.getHeight()) >= (boundW / boundH)) {
                imgW = boundW;
                imgH = (imgToPrint.getHeight() / (imgToPrint.getWidth() / boundW));
            } else {
                imgW = (imgToPrint.getWidth() / (imgToPrint.getHeight() / boundH));
                imgH = boundH;
            }
        }

        double boundX = pageFormat.getImageableX();
        double boundY = pageFormat.getImageableY();

        log.debug("Paper area: {},{}:{},{}", (int)boundX, (int)boundY, (int)boundW, (int)boundH);
        log.trace("Image size: {},{}", imgW, imgH);

        // Now we perform our rendering
        Graphics2D graphics2D = (Graphics2D)graphics;
        graphics2D.setRenderingHints(buildRenderingHints(dithering, interpolation));
        log.trace("{}", graphics2D.getRenderingHints());

        log.debug("Memory: {}m/{}m", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576, Runtime.getRuntime().maxMemory() / 1048576);

        if (!manualReverse) {
            graphics2D.drawImage(imgToPrint, (int)boundX, (int)boundY, (int)(boundX + imgW), (int)(boundY + imgH),
                                 0, 0, imgToPrint.getWidth(), imgToPrint.getHeight(), null);
        } else {
            graphics2D.drawImage(imgToPrint, (int)(boundW + boundX - imgW), (int)(boundH + boundY - imgH), (int)(boundW + boundX), (int)(boundH + boundY),
                                 0, 0, imgToPrint.getWidth(), imgToPrint.getHeight(), null);
        }

        // Valid page
        return PAGE_EXISTS;
    }

    /**
     *
     * @param image
     * @param pageFormat
     * @return
     */
    private BufferedImage scale(BufferedImage image, PageFormat pageFormat) {
        //scale up to print density (using less of a stretch if image is already larger than page)
        double upScale = dpiScale * Math.min((pageFormat.getImageableWidth() / image.getWidth()), (pageFormat.getImageableHeight() / image.getHeight()));
        if (upScale > dpiScale) { upScale = dpiScale; } else if (upScale < 1) { upScale = 1; }

        if (upScale > 1) {
            log.debug("Scaling image up by x{}", upScale);

            BufferedImage scaled = new BufferedImage((int)(image.getWidth() * upScale), (int)(image.getHeight() * upScale), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = scaled.createGraphics();
            g2d.setRenderingHints(buildRenderingHints(dithering, interpolation));
            g2d.drawImage(image, 0, 0, (int)(image.getWidth() * upScale), (int)(image.getHeight() * upScale), null);
            g2d.dispose();

            return scaled;
        } else {
            log.debug("No need to upscale image");
            return image;
        }
    }

    /**
     * Rotates {@code image} by the specified {@code angle}.
     *
     * @param image BufferedImage to rotate
     * @param angle Rotation angle in degrees
     * @return Rotated image data
     */
    public static BufferedImage rotate(BufferedImage image, double angle, Object dithering, Object interpolation) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));

        int sWidth = image.getWidth(), sHeight = image.getHeight();
        int eWidth = (int)Math.floor((sWidth * cos) + (sHeight * sin)), eHeight = (int)Math.floor((sHeight * cos) + (sWidth * sin));

        BufferedImage result;
        if(!GraphicsEnvironment.isHeadless()) {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getDefaultConfiguration();
            result = gc.createCompatibleImage(eWidth, eHeight, Transparency.TRANSLUCENT);
        } else {
            result = new BufferedImage(eWidth, eHeight, Transparency.TRANSLUCENT);
        }

        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHints(buildRenderingHints(dithering, interpolation));
        g2d.translate((eWidth - sWidth) / 2, (eHeight - sHeight) / 2);
        g2d.rotate(rads, sWidth / 2, sHeight / 2);

        if (angle % 90 == 0 || interpolation == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR) {
            g2d.drawRenderedImage(image, null);
        } else {
            g2d.setPaint(new TexturePaint(image, new Rectangle2D.Float(0, 0, image.getWidth(), image.getHeight())));
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        }

        g2d.dispose();

        return result;
    }

    private BufferedImage rotate(BufferedImage image, double angle) {
        return rotate(image, angle, dithering, interpolation);
    }

    @Override
    public void cleanup() {
        images.clear();

        dpiScale = 1.0;
        scaleImage = false;
        imageRotation = 0;
        dithering = RenderingHints.VALUE_DITHER_DEFAULT;
        interpolation = RenderingHints.VALUE_INTERPOLATION_BICUBIC;
        manualReverse = false;
    }

    /**
     * Creates a raw-compatible BufferedImage
     */
    @Override
    public BufferedImage createBufferedImage(String data, JSONObject opt, PrintingUtilities.Flavor flavor, PrintOptions.Raw rawOpts, PrintOptions.Pixel pxlOpts) throws IOException {
        return loadImage(data, flavor);
    }
}
