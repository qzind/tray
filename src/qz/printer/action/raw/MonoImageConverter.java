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
import java.util.BitSet;

/**
 * Abstract wrapper for images to be printed with thermal printers.
 *
 * @author Tres Finocchiaro
 * @author Antoni Ten Monro's
 * @author Oleg Morozov
 */

public abstract class MonoImageConverter extends ImageConverter {
    public enum Quantization {
        BLACK, // color value must be the exact value of black
        ALPHA, // alpha is more than a set threshold is considered black (discarding color info)
        LUMA, // luma (or alpha) must be less than a set threshold to be considered black
        DITHER; // image is processed via a separate black & white dithering algorithm

        public static Quantization parse(String input) {
            for(Quantization quantization : Quantization.values()) {
                if (quantization.name().equalsIgnoreCase(input)) {
                    return quantization;
                }
            }
            return BLACK;
        }
    }
    private static final Logger log = LogManager.getLogger(MonoImageConverter.class);

    private BitSet imageAsBitSet; // pixels stored as 1/0 (black/white) array
    private int[] imageAsIntArray; // packs every eight zero's to a full byte, in decimal
    private Quantization quantization;
    private int threshold;

    @Override
    public void setBufferedImage(BufferedImage bufferedImage) {
        super.setBufferedImage(validateWidth(getLanguageType(), bufferedImage));
        log.info("Initializing black & white pixels...");
        this.imageAsBitSet = generateBlackPixels(getBufferedImage(), getLanguageType(), quantization, threshold);
        this.imageAsIntArray = convertToIntArray(this.imageAsBitSet);
    }

    /**
     * Params shared between all monochrome image converters
     * NOTE: Remember to @Override for subclasses requiring additional params
     */
    public void setParams(JSONObject params) {
        quantization = Quantization.parse(params.optString("quantization", Quantization.BLACK.toString()));
        threshold = params.optInt("threshold", 127);
    }

    public static BitSet generateBlackPixels(BufferedImage bi, LanguageType languageType, Quantization quantization, int threshold) {
        log.info("Converting image to monochrome...");
        int h = bi.getHeight();
        int w = bi.getWidth();
        int[] rgbPixels = bi.getRGB(0, 0, w, h, null, 0, w);

        /*
         * It makes most sense to have black pixels as 1's and white pixels
         * as zero's, however some printer manufacturers had this reversed
         * and used 0's for the black pixels.  EPL is a common language that
         * uses 0's for black pixels.
         * See also: https://support.zebra.com/cpws/docs/eltron/gw_command.htm
         */
        BitSet pixels = new BitSet(rgbPixels.length);
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
            case LUMA:
                if (a < threshold) {
                    return false; // assume pixels that are less opaque than the luma threshold should be considered to be white
                }

                int luma = ((r * 299) + (g * 587) + (b * 114)) / 1000; // luma formula
                return luma < threshold; // pixels that have less luma than the threshold are black
            case ALPHA:
                return a > threshold; // pixels that are more opaque than the threshold are black
            case BLACK: // only fully black pixels are black
                return color.equals(Color.BLACK);
            default:
                throw new UnsupportedOperationException("Image quantization " + quantization + " is not yet supported");
        }
    }

    public static int[] convertToIntArray(BitSet bitSet) {
        log.info("Packing bits...");
        int[] intArray = new int[bitSet.size() / 8];
        // Convert every eight zero's to a full byte, in decimal
        for(int i = 0; i < intArray.length; i++) {
            for(int k = 0; k < 8; k++) {
                intArray[i] += (bitSet.get(8 * i + k)? 1:0) << 7 - k;
            }
        }
        return intArray;
    }

    public BitSet getImageAsBitSet() {
        return imageAsBitSet;
    }

    public byte[] toBytes() {
        return ByteUtilities.toByteArray(imageAsIntArray);
    }

    public int getThreshold() {
        return threshold;
    }

    public String convertImageToHexString() {
        return ByteUtilities.getHexString(imageAsIntArray);
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
