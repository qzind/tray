/*
 *
 * Copyright (C) 2025 Tres Finocchiaro, QZ Industries
 * Copyright (C) 2013 Antoni Ten Monro's
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.printer.action.raw.mono;

import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.ImageConverterType;
import qz.printer.action.raw.LanguageType;
import qz.printer.action.raw.encoder.*;
import qz.utils.ByteUtilities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.BitSet;

import static qz.printer.action.raw.encoder.EscPosEncoder.*;

/**
 * Abstract wrapper for images to be printed with thermal printers.
 *
 * @author Tres Finocchiaro
 * @author Antoni Ten Monro's
 * @author Oleg Morozov
 */
@SuppressWarnings("UnusedDeclaration") //Library class
public class MonoImageConverter implements ImageConverter {
    private static final Logger log = LogManager.getLogger(MonoImageConverter.class);

    private final BufferedImage bufferedImage;
    private final LanguageType languageType;
    private final BitSet imageAsBitSet; // pixels stored as 1/0 (black/white) array
    private final int[] imageAsIntArray; // packs every eight zero's to a full byte, in decimal

    /**
     * Creates a new <code>ImageWrapper</code> from a <code>BufferedImage</code>.
     */
    public MonoImageConverter(BufferedImage bufferedImage, LanguageType languageType, Quantization quantization, int threshold) {
        this.bufferedImage = validateWidth(languageType, bufferedImage);
        this.languageType = languageType;

        log.info("Loaded BufferedImage: {}x{}", this.bufferedImage.getWidth(), this.bufferedImage.getHeight());
        log.info("Initializing black & white pixels...");
        this.imageAsBitSet = generateBlackPixels(languageType, quantization, threshold, bufferedImage);
        this.imageAsIntArray = convertToIntArray(this.imageAsBitSet);
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

    public static BitSet generateBlackPixels(LanguageType languageType, Quantization quantization, int threshold, BufferedImage bi) {
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
            boolean isBlack =  languageType.requiresImageOutputInverted() != isBlack(rgbPixels[i], quantization, threshold);
            pixels.set(i, isBlack);
        }

        return pixels;
    }

    private static int[] convertToIntArray(BitSet bitSet) {
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

    /**
     * Generates the raw commands to print an image
     */
    public byte[] getImageCommand(JSONObject opt) throws InvalidRawImageException, IOException {
        switch(languageType) {
            case ESCPOS:
                String imageEncoding = opt.optString("imageEncoding");
                EscPosEncoderType encoderType = EscPosEncoderType.parse(imageEncoding);

                switch(encoderType) {
                    case DEFAULT:
                        String dotDensity = opt.optString("dotDensity");
                        return new EscPosEncoder(dotDensity).encode(this);
                    case GS_L:
                        return new EscPosGsLEncoder().encode(this);
                    case GS_V_0:
                        return new EscPosGsV0Encoder().encode(this);
                    default:
                        throw new UnsupportedOperationException("EscPosEncoderType " + encoderType + " is not yet supported");
                }
            case ZPL:
                return new ZplEncoder().encode(this);
            case EPL:
                int x = opt.optInt("x", 0);
                int y = opt.optInt("y", 0);
                return new EplEncoder(x, y).encode(this);
            case CPCL:
                return new CpclEncoder().encode(this);
            case SBPL:
                return new SbplEncoder().encode(this);
            case PGL:
                String logoId = opt.optString("logoId", "");
                boolean igpDots = opt.optBoolean("igpDots", false);
                return new PglEncoder(logoId, igpDots).encode(this);
            default:
                throw new InvalidRawImageException(languageType + " image conversion is not yet supported.");
        }
    }

    public int getWidth() {
        return bufferedImage.getWidth();
    }

    public int getHeight() {
        return bufferedImage.getHeight();
    }

    public BitSet getImageAsBitSet() {
        return imageAsBitSet;
    }

    public int[] getImageAsIntArray() {
        return imageAsIntArray;
    }

    public String convertImageToHexString() {
        return ByteUtilities.getHexString(imageAsIntArray);
    }

    /**
     * Checks if the image width is a multiple of 8, and if it's not,
     * pads the image on the right side with blank pixels. <br />
     */
    private BufferedImage validateWidth(LanguageType languageType, BufferedImage inputImage) {
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

    @Override
    public ImageConverterType getImageType() {
        return ImageConverterType.MONO;
    }
}
