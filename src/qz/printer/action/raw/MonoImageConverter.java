/*
 *
 * Copyright (C) 2025 Tres Finocchiaro, QZ Industries
 * Copyright (C) 2013 Antoni Ten Monro's
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ByteUtilities;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Abstract wrapper for images to be printed with thermal printers.
 *
 * @author Tres Finocchiaro
 * @author Antoni Ten Monro's
 * @author Oleg Morozov
 */
public abstract class MonoImageConverter extends ImageConverter {
    public enum Quantization {
        ALPHA, // alpha is more than a set threshold is considered black (discarding color info)
        BLACK, // color value must be the exact value of black
        DITHER, // image is processed via a separate black & white dithering algorithm
        LUMA; // luma (or alpha) must be less than a set threshold to be considered black


        /**
         * Parses the quantization from String <code>input</code>, falling back to
         * <code>defaultVal</code> if <code>null</code> was provided or if no match is found.
         */
        public static Quantization parse(String input, Quantization defaultVal) {
            for(Quantization quantization : Quantization.values()) {
                if (quantization.name().equalsIgnoreCase(input)) {
                    return quantization;
                }
            }
            return defaultVal;
        }
    }

    private static final Logger log = LogManager.getLogger(MonoImageConverter.class);

    private PixelGrid imageAsPixelGrid; // pixels stored as 1/0 (black/white) array
    private byte[] imageAsByteArray; // packs every eight zero's to a full byte, in decimal
    private Quantization quantization;
    private int threshold;

    @Override
    public void setBufferedImage(BufferedImage bufferedImage) {
        super.setBufferedImage(validateWidth(getLanguageType(), bufferedImage));
        log.info("Initializing black & white pixels...");
        this.imageAsPixelGrid = generateBlackPixels(getBufferedImage(), getLanguageType(), quantization, threshold);
        this.imageAsByteArray = ByteUtilities.toByteArray(this.imageAsPixelGrid);
    }

    /**
     * Params shared between all monochrome image converters
     * NOTE: Remember to <code>@Override</code> and <code>super(...)</code> for subclasses requiring additional params
     */
    public void setParams(JSONObject params) {
        quantization = Quantization.parse(params.optString("quantization"), Quantization.LUMA);
        threshold = params.optInt("threshold", 127);
    }

    public static PixelGrid generateBlackPixels(BufferedImage bi, LanguageType languageType, Quantization quantization, int threshold) {
        log.info("Converting image to monochrome...");
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[] rgbPixels = bi.getRGB(0, 0, w, h, null, 0, w);

        /*
         * It makes most sense to have black pixels as 1's and white pixels
         * as zero's, however some printer manufacturers had this reversed
         * and used 0's for the black pixels.  EPL is a common language that
         * uses 0's for black pixels.
         * See also: https://support.zebra.com/cpws/docs/eltron/gw_command.htm
         */
        PixelGrid pixels = new PixelGrid(w, h);
        for(int i = 0; i < rgbPixels.length; i++) {
            boolean isBlack = languageType.requiresImageOutputInverted() != isBlack(rgbPixels[i], quantization, threshold);
            pixels.set(i, isBlack);
        }

        return pixels;
    }

    /**
     * Tests if a given pixel should be black. Multiple quantization algorithms
     * are available.
     */
    public static boolean isBlack(int rgbPixel, Quantization quantization, int threshold) {
        Color color = new Color(rgbPixel, true);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        switch(quantization) {
            case ALPHA:
                return a > threshold; // pixels that are more opaque than the threshold are black
            case BLACK: // only fully black pixels are black
                return color.equals(Color.BLACK);
            case LUMA:
                if (a < threshold) {
                    return false; // assume pixels that are less opaque than the luma threshold should be considered to be white
                }

                int luma = ((r * 299) + (g * 587) + (b * 114)) / 1000; // luma formula
                return luma < threshold; // pixels that have less luma than the threshold are black
            default:
                throw new UnsupportedOperationException("Image quantization " + quantization + " is not yet supported");
        }
    }

    public PixelGrid getImageAsPixelGrid() {
        return imageAsPixelGrid;
    }

    public byte[] getBytes() {
        return imageAsByteArray;
    }

    public int getThreshold() {
        return threshold;
    }

    public String convertImageToHexString() {
        return ByteUtilities.toHexString(imageAsByteArray);
    }

    /**
     * Checks if the image width is a multiple of 8, and if it's not,
     * pads the image on the right side with blank pixels. <br />
     */
    private static BufferedImage validateWidth(LanguageType languageType, BufferedImage inputImage) {
        if(languageType.requiresImageWidthValidated()) {
            int height = inputImage.getHeight();
            int width = inputImage.getWidth();
            if (width % 8 != 0) {
                log.info("{} requires image are a multiple of 8, padding", languageType);
                int newWidth = (width / 8 + 1) * 8;
                BufferedImage newImage = new BufferedImage(newWidth, height, BufferedImage.TYPE_INT_ARGB);

                Graphics2D g = newImage.createGraphics();
                g.drawImage(inputImage, 0, 0, null);
                g.dispose();
                return newImage;
            }
        }
        return inputImage;
    }
}
