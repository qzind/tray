/*
 *
 * Copyright (C) 2013 Tres Finocchiaro, QZ Industries
 * Copyright (C) 2013 Antoni Ten Monro's
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */
package qz.printer.action.raw;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;
import qz.utils.ByteUtilities;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Abstract wrapper for images to be printed with thermal printers.
 *
 * @author Tres Finocchiaro
 * @author Antoni Ten Monro's
 *         <p/>
 *         Changelog:
 *         <p/>
 *         20130805 (Tres Finocchiaro) Merged Antoni's changes with original keeping
 *         Antoni's better instantiation, "black" pixel logic, removing class abstraction
 *         (uses LanguageType Enum switching instead for smaller codebase)
 *         <p/>
 *         20130710 (Antoni Ten Monro's) Refactored the original, to have the
 *         actual implementation of the different ImageWrapper classes in derived
 *         classes, while leaving common functionality here.
 * @author Oleg Morozov 02/21/2013 (via public domain)
 * @author Tres Finocchiaro 10/01/2013
 */
@SuppressWarnings("UnusedDeclaration") //Library class
public class ImageWrapper {

    private static final Logger log = LogManager.getLogger(ImageWrapper.class);

    /**
     * Represents the CHECK_BLACK quantization method, where only fully black
     * pixels are considered black when translating them to printer format.
     */
    public static final int CHECK_BLACK = 0;
    /**
     * Represents the CHECK_LUMA quantization method, pixels are considered
     * black if their luma is less than a set threshold. Transparent pixels, and
     * pixels whose alpha channel is less than the threshold are considered not
     * black.
     */
    public static final int CHECK_LUMA = 1;
    /**
     * Represents the CHECK_ALPHA quantization method, pixels are considered
     * black if their alpha is more than a set threshold. Color information is
     * discarded.
     */
    public static final int CHECK_ALPHA = 2;

    private int lumaThreshold = 127;
    private boolean[] imageAsBooleanArray;        //Image representation as an array of boolean, with true values representing imageAsBooleanArray dots
    private int[] imageAsIntArray;                //Image representation as an array of ints, with each bit representing a imageAsBooleanArray dot
    private ByteArrayBuilder byteBuffer = new ByteArrayBuilder();
    private int alphaThreshold = 127;
    private BufferedImage bufferedImage;
    private LanguageType languageType;
    private Charset charset = Charset.defaultCharset();
    private int imageQuantizationMethod = CHECK_LUMA;
    private int xPos = 0;   // X coordinate used for EPL2, CPCL.  Irrelevant for ZPLII, ESC/POS, etc
    private int yPos = 0;   // Y coordinate used for EPL2, CPCL.  Irrelevant for ZPLII, ESC/POS, etc
    private String logoId = "";  // PGL only, the logo ID
    private boolean igpDots = false; // PGL only, toggle IGP/PGL default resolution of 72dpi
    private int dotDensity = 32;  // Generally 32 = Single (normal) 33 = Double (higher res) for ESC/POS.  Irrelevant for all other languages.

    /**
     * Creates a new
     * <code>ImageWrapper</code> from a
     * <code>BufferedImage.</code>
     *
     * @param bufferedImage The image to convert for thermal printing
     */
    public ImageWrapper(BufferedImage bufferedImage, LanguageType languageType) {
        this.bufferedImage = bufferedImage;
        this.languageType = languageType;
        log.info("Loading BufferedImage");
        log.info("Dimensions: {}x{}", bufferedImage.getWidth(), bufferedImage.getHeight());
        init();

        if (languageType.requiresImageWidthValidated()) {
            validateImageWidth();
        }
    }

    /**
     * Returns the luma threshold used for the CHECK_LUMA quantization method.
     * Pixels that are more transparent than this, or that have a luma greater
     * than this will be considered white. The threshold goes from 0 (black) to
     * 255 (white).
     *
     * @return the current threshold
     */
    public int getLumaThreshold() {
        return lumaThreshold;
    }

    /**
     * Sets the luma threshold used for the CHECK_LUMA quantization method.
     * Pixels that are more transparent than this, or that have a luma greater
     * than this will be considered white. The threshold goes from 0 (black) to
     * 255 (white).
     *
     * @param lumaThreshold the threshold to set
     */
    public void setLumaThreshold(int lumaThreshold) {
        this.lumaThreshold = lumaThreshold;
    }

    /**
     * Get the method used to convert the image to monochrome. Currently
     * implemented methods are: <ul> <li><code>CHECK_BLACK</code>: Pixels are
     * considered black if and only if they are completely black and opaque
     * <li><code>CHECK_LUMA</code>: Pixels are considered black if and only if
     * their luma is under a threshold, and their opacity is over a threshold.
     * This threshold is set with
     * <code>setLumaThreshold</code> <li><code>CHECK_ALPHA</code>: Pixels are
     * considered black if and only if their opacity (alpha) is over a
     * threshold,. This threshold is set with
     * <code>setAlphaThreshold</code> </ul>
     * <p/>
     * Default quantization method is
     * <code>CHECK_BLACK</code>.
     *
     * @return the current quantization method
     */
    public int getImageQuantizationMethod() {
        return imageQuantizationMethod;
    }

    /**
     * Sets the method used to convert the image to monochrome. Currently
     * implemented methods are: <ul> <li><code>CHECK_BLACK</code>: Pixels are
     * considered black if and only if they are completely black and opaque
     * <li><code>CHECK_LUMA</code>: Pixels are considered black if and only if
     * their luma is under a threshold, and their opacity is over a threshold.
     * This threshold is set with
     * <code>setLumaThreshold</code> <li><code>CHECK_ALPHA</code>: Pixels are
     * considered black if and only if their opacity (alpha) is over a
     * threshold,. This threshold is set with
     * <code>setAlphaThreshold</code> </ul>
     * <p/>
     * Default (and fallback) quantization method is
     * <code>CHECK_BLACK</code>.
     *
     * @param imageQuantizationMethod the quantization method to set
     */
    public void setImageQuantizationMethod(int imageQuantizationMethod) {
        this.imageQuantizationMethod = imageQuantizationMethod;
    }

    /**
     * Returns the transparency (alpha) threshold used for the CHECK_ALPHA
     * quantization method. Pixels that are more transparent than this will be
     * considered white. The threshold goes from 0 (fully transparent) to 255
     * (fully opaque)
     *
     * @return the current threshold
     */
    public int getAlphaThreshold() {
        return alphaThreshold;
    }

    /**
     * Sets the transparency (alpha) threshold used for the CHECK_ALPHA
     * quantization method. Pixels that are more transparent than this will be
     * considered white. The threshold goes from 0 (fully transparent) to 255
     * (fully opaque)
     *
     * @param alphaThreshold the Threshold to set
     */
    public void setAlphaThreshold(int alphaThreshold) {
        this.alphaThreshold = alphaThreshold;
    }

    public int getDotDensity() {
        return dotDensity;
    }

    public void setDotDensity(int dotDensity) {
        this.dotDensity = dotDensity;
    }

    public void setLogoId(String logoId) {
        this.logoId = logoId;
    }

    public String getLogoId() {
        return logoId;
    }

    public void setIgpDots(boolean igpDots) {
        this.igpDots = igpDots;
    }

    public boolean getIgpDots() {
        return igpDots;
    }

    public int getxPos() {
        return xPos;
    }

    public void setxPos(int xPos) {
        this.xPos = xPos;
    }

    public int getyPos() {
        return yPos;
    }

    public void setyPos(int yPos) {
        this.yPos = yPos;
    }

    /**
     * Tests if a given pixel should be black. Multiple quantization algorithms
     * are available. The quantization method should be adjusted with
     * setQuantizationMethod. Should an invalid value be set as the
     * quantization method, CHECK_BLACK will be used
     *
     * @param rgbPixel the color of the pixel as defined in getRGB()
     * @return true if the pixel should be black, false otherwise
     */
    private boolean isBlack(int rgbPixel) {
        Color color = new Color(rgbPixel, true);

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        switch(getImageQuantizationMethod()) {
            case CHECK_LUMA:
                if (a < getLumaThreshold()) {
                    return false; // assume pixels that are less opaque than the luma threshold should be considered to be white
                }

                int luma = ((r * 299) + (g * 587) + (b * 114)) / 1000; //luma formula
                return luma < getLumaThreshold(); //pixels that have less luma than the threshold are black
            case CHECK_ALPHA:
                return a > getAlphaThreshold(); //pixels that are more opaque than the threshold are black
            case CHECK_BLACK: //only fully black pixels are black
            default:
                return color.equals(Color.BLACK); //The default

        }
    }

    /**
     * Sets ImageAsBooleanArray. boolean is used instead of int for memory
     * considerations.
     */
    private boolean[] generateBlackPixels(BufferedImage bi) {
        log.info("Converting image to monochrome");
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
        boolean[] pixels = new boolean[rgbPixels.length];
        for(int i = 0; i < rgbPixels.length; i++) {
            pixels[i] = languageType.requiresImageOutputInverted() != isBlack(rgbPixels[i]);
        }

        return pixels;
    }

    /**
     * Converts the internal representation of the image into an array of bytes,
     * suitable to be sent to a raw printer.
     *
     * @return The raw bytes that compose the image
     */
    private byte[] getBytes() {
        log.info("Generating byte array");
        int[] ints = getImageAsIntArray();
        byte[] bytes = new byte[ints.length];
        for(int i = 0; i < ints.length; i++) {
            bytes[i] = (byte)ints[i];
        }

        return bytes;
    }

    private void generateIntArray() {
        log.info("Packing bits");
        imageAsIntArray = new int[imageAsBooleanArray.length / 8];
        // Convert every eight zero's to a full byte, in decimal
        for(int i = 0; i < imageAsIntArray.length; i++) {
            for(int k = 0; k < 8; k++) {
                imageAsIntArray[i] += (imageAsBooleanArray[8 * i + k]? 1:0) << 7 - k;
            }
        }
    }

    /**
     * Generates the EPL2 commands to print an image. One command is emitted per
     * line of the image. This avoids issues with commands being too long.
     *
     * @return The commands to print the image as an array of bytes, ready to be
     * sent to the printer
     */
    public byte[] getImageCommand(JSONObject opt) throws InvalidRawImageException, UnsupportedEncodingException {
        getByteBuffer().clear();

        switch(languageType) {
            case ESCP:
                appendEpsonSlices(getByteBuffer());
                break;
            case ZPL:
                String zplHexAsString = ByteUtilities.getHexString(getImageAsIntArray());
                int byteLen = zplHexAsString.length() / 2;
                int perRow = byteLen / getHeight();
                StringBuilder zpl = new StringBuilder("^GFA,")
                        .append(byteLen).append(",").append(byteLen).append(",")
                        .append(perRow).append(",").append(zplHexAsString);

                getByteBuffer().append(zpl, charset);
                break;
            case EPL:
                StringBuilder epl = new StringBuilder("GW")
                        .append(getxPos()).append(",")
                        .append(getyPos()).append(",")
                        .append(getWidth() / 8).append(",")
                        .append(getHeight()).append(",");

                getByteBuffer().append(epl, charset).append(getBytes()).append(new byte[] {10});
                break;
            case CPCL:
                String cpclHexAsString = ByteUtilities.getHexString(getImageAsIntArray());
                StringBuilder cpcl = new StringBuilder("EG ")
                        .append(getWidth() / 8).append(" ")
                        .append(getHeight()).append(" ")
                        .append(getxPos()).append(" ")
                        .append(getyPos()).append(" ")
                        .append(cpclHexAsString);

                getByteBuffer().append(cpcl, charset).append(new byte[] {13, 10});
                break;
            case EVOLIS:
                try {
                    ArrayList<float[]> cymkData = convertToCYMK();
                    int precision = opt.optInt("precision", 128);

                    // Y,M,C,K,O ribbon
                    generateRibbonData('y', precision, cymkData.get(1));
                    generateRibbonData('m', precision, cymkData.get(2));
                    generateRibbonData('c', precision, cymkData.get(0));

                    //K(black) and O(overlay) are always precision 2
                    generateRibbonData('k', 2, cymkData.get(3));

                    if (opt.has("overlay")) {
                        try { generateRibbonData('o', 2, parseOverlay(opt.get("overlay"))); }
                        catch(Exception e) {
                            log.error("Failed to parse overlay data: {}", e.getMessage());
                        }
                    }
                }
                catch(IOException ioe) {
                    throw new InvalidRawImageException(ioe.getMessage(), ioe);
                }

                break;
            case SBPL:
                String sbplHexAsString = ByteUtilities.getHexString(getImageAsIntArray());
                StringBuilder sbpl = new StringBuilder("GH")
                        .append(String.format("%03d", getWidth() / 8))
                        .append(String.format("%03d", getHeight() / 8))
                        .append(sbplHexAsString);

                getByteBuffer().append(new byte[] {27}).append(sbpl, charset);
                break;
            case PGL:
                if(logoId.isEmpty()) {
                    throw new InvalidRawImageException("Printronix graphics require a logoId");
                }
                if(igpDots) {
                    // IGP images cannot exceed 240x252
                    if(getWidth() > 240 || getHeight() > 252) {
                        throw new InvalidRawImageException("IGP dots is enabled; Size values HL/VL cannot exceed 240x252");
                    }
                }

                // igpDots: true: Use IGP standard 60dpi/72dpi graphics (removes ";DOTS" from raw command)
                // igpDots: false: Use the printer's native resolution (appends ";DOTS" to raw command)
                StringBuilder pgl = new StringBuilder("~LOGO;").append(logoId).append(";")
                        .append(getHeight()).append(";").append(getWidth()).append(igpDots ? ";DOTS" : "").append("\n")
                        .append(getImageAsPGLDots())
                        .append("END").append("\n");

                getByteBuffer().append(pgl, charset);
                break;
            default:
                throw new InvalidRawImageException(languageType + " image conversion is not yet supported.");
        }

        return getByteBuffer().getByteArray();
    }

    /**
     * @return the width of the image
     */
    public int getWidth() {
        return bufferedImage.getWidth();
    }

    /**
     * @return the height of the image
     */
    public int getHeight() {
        return bufferedImage.getHeight();
    }

    /**
     * @return the image as an array of booleans
     */
    private boolean[] getImageAsBooleanArray() {
        return imageAsBooleanArray;
    }

    /**
     * Printronix format
     *    [line];[black dots range];[more black dots range][newline]
     * e.g
     *    1;1-12;19-22;38-39
     *
     */
    private String getImageAsPGLDots() {
        StringBuilder pglDots = new StringBuilder();
        int pixelIndex = 0;
        for(int h = 1; h <= getHeight(); h++) {
            StringBuilder line = new StringBuilder();

            int start = -1;
            int end = -1;

            for(int w = 1; w <= getWidth(); w++) {
                if(imageAsBooleanArray[pixelIndex]) {
                    System.out.print(".");
                    if(start == -1) {
                        start = w;
                    }
                } else {
                    System.out.print(" ");
                    if(start != -1) {
                        end = w - 1;
                    }
                }

                // Handle trailing pixel
                if(w == getWidth()) {
                    end = w;
                }

                if(start != -1 && end != -1) {
                    if(start == end) {
                        // append a single dot
                        line.append(start).append(";");
                    } else {
                        // append a range of dots
                        line.append(start).append("-").append(end).append(";");
                    }
                    start = -1;
                    end = -1;
                }
                pixelIndex++;
            }
            System.out.print("\n");
            if(line.length() > 0) {
                // Remove trailing ";"
                if(line.charAt(line.length() -1) == ';') {
                    line.replace(line.length() - 1, line.length(), "");
                }
                // Add line number
                line.insert(0, h + ";");

                // Add to final commands
                pglDots.append(line).append("\n");
            }

        }

        return pglDots.toString();
    }

    /**
     * @param imageAsBooleanArray the imageAsBooleanArray to set
     */
    private void setImageAsBooleanArray(boolean[] imageAsBooleanArray) {
        this.imageAsBooleanArray = imageAsBooleanArray;
    }

    /**
     * @return the imageAsIntArray
     */
    private int[] getImageAsIntArray() {
        return imageAsIntArray;
    }

    /**
     * @param imageAsIntArray the imageAsIntArray to set
     */
    private void setImageAsIntArray(int[] imageAsIntArray) {
        this.imageAsIntArray = imageAsIntArray;
    }

    /**
     * Initializes the ImageWrapper. This populates the internal structures with
     * the data created from the original image. It is normally called by the
     * constructor, but if for any reason you change the image contents (for
     * example, if you resize the image), it must be initialized again prior to
     * calling getImageCommand()
     */
    private void init() {
        log.info("Initializing Image Fields");
        setImageAsBooleanArray(generateBlackPixels(bufferedImage));
        generateIntArray();
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    /**
     * @return the byteBuffer
     */
    private ByteArrayBuilder getByteBuffer() {
        return byteBuffer;
    }

    /**
     * @return the buffer
     */
    private BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    /**
     * @param buffer the buffer to set
     */
    private void setBufferedImage(BufferedImage buffer) {
        bufferedImage = buffer;
    }

    /**
     * http://android-essential-devtopics.blogspot.com/2013/02/sending-bit-image-to-epson-printer.html
     * <p>
     * Images are read as one long array of black or white pixels, as scanned top to bottom and left to right.
     * Printer format needs this sent in height chunks in bytes (normally 3, for 24 pixels at a time) for each x position along a segment,
     * and repeated for each segment of height over the byte limit.
     *
     * @param builder the ByteArrayBuilder to use
     */
    private void appendEpsonSlices(ByteArrayBuilder builder) {
        // set line height to the size of each chunk we will be sending
        int segmentHeight = dotDensity > 1 ? 24 : (dotDensity == 1 ? 8 : 16); // height will be handled explicitly below if striping
        // Impact printers (U220, etc) benefit from double-pass striping (odd/even) for higher quality (dotDensity = 1)
        boolean stripe = dotDensity == 1;
        int bytesNeeded = (dotDensity <= 1 || stripe)? 1:3;

        int offset = 0; // keep track of chunk offset currently being written
        boolean zeroPass = true; // track if this segment get rewritten with 1 pixel offset, always true if not striping

        while(offset < getHeight()) {
            // compute 2 byte value of the image width (documentation states width is 'nL' + ('nH' * 256))
            byte nL = (byte)((getWidth() % 256));
            byte nH = (byte)((getWidth() / 256));
            builder.append(new byte[] {0x1B, 0x2A, (byte)dotDensity, nL, nH});

            for(int x = 0; x < getWidth(); x++) {
                for(int bite = 0; bite < bytesNeeded; bite++) {
                    byte slice = 0;

                    //iterate bit for the byte - striping spans 2 bytes (taking every other bit) to be compacted down into one
                    for(int bit = (zeroPass? 0:1); bit < 8 * (stripe? 2:1); bit += (stripe? 2:1)) {
                        // get the y position of the current pixel being found
                        int y = offset + ((bite * 8) + bit);

                        // calculate the location of the pixel we want in the bit array and update the slice if it is supposed to be black
                        int i = (y * getWidth()) + x;
                        if (i < imageAsBooleanArray.length && imageAsBooleanArray[i]) {
                            // append desired bit to current byte being built, remembering that bits go right to left
                            slice |= (byte)(1 << (7 - (bit - (zeroPass? 0:1)) / (stripe? 2:1)));
                        }
                    }

                    builder.append(new byte[] {slice});
                }
            }

            // move print head down to next segment (or offset by one if striping)
            if (stripe) {
                if (zeroPass) {
                    builder.append(new byte[] {0x1B, 0x4A, 0x01}); // only shift down 1 pixel for the next pass
                } else {
                    builder.append(new byte[] {0x1B, 0x4A, (byte)(segmentHeight - 1)}); // shift down remaining pixels
                    offset += 8 * bytesNeeded; // only shift offset on every other pass (along with segments)
                }

                zeroPass = !zeroPass;
            } else {
                //shift down for next segment
                builder.append(new byte[] {0x1B, 0x4A, (byte)segmentHeight});
                offset += 8 * bytesNeeded;
            }
        }
    }

    private ArrayList<float[]> convertToCYMK() throws IOException {
        int[] pixels = bufferedImage.getRGB(0, 0, getWidth(), getHeight(), null, 0, getWidth());

        float[] cyan = new float[pixels.length];
        float[] yellow = new float[pixels.length];
        float[] magenta = new float[pixels.length];
        float[] black = new float[pixels.length];

        for(int i = 0; i < pixels.length; i++) {
            float rgb[] = new Color(pixels[i]).getRGBColorComponents(null);
            if (rgb[0] == 0.0f && rgb[1] == 0.0f && rgb[2] == 0.0f) {
                black[i] = 1.0f;
            } else {
                cyan[i] = 1.0f - rgb[0];
                magenta[i] = 1.0f - rgb[1];
                yellow[i] = 1.0f - rgb[2];
            }
        }

        ArrayList<float[]> colorData = new ArrayList<>();
        colorData.add(cyan);
        colorData.add(yellow);
        colorData.add(magenta);
        colorData.add(black);

        return colorData;
    }

    private float[] parseOverlay(Object overlay) throws IOException, JSONException {
        float[] overlayData = new float[getWidth() * getHeight()];

        if (overlay instanceof JSONArray) {
            //array of rectangles
            JSONArray masterBlock = (JSONArray)overlay;
            for(int i = 0; i < masterBlock.length(); i++) {
                JSONArray block = masterBlock.getJSONArray(i);
                if (block != null && block.length() == 4) {
                    for(int y = block.getInt(1) - 1; y < block.getInt(3); y++) {
                        int off = (y * getWidth());
                        for(int x = block.getInt(0) - 1; x < block.getInt(2); x++) {
                            if ((off + x) >= 0 && (off + x) < overlayData.length) {
                                overlayData[off + x] = 1.0f;
                            }
                        }
                    }
                }
            }
        } else if (overlay instanceof String) {
            //image mask
            boolean[] mask = generateBlackPixels(ImageIO.read(new URL((String)overlay)));
            for(int i = 0; i < overlayData.length; i++) {
                overlayData[i] = (mask[i]? 1.0f:0.0f);
            }
        } else if (overlay instanceof Boolean && (boolean)overlay) {
            //boolean coat
            for(int i = 0; i < overlayData.length; i++) {
                overlayData[i] = 1.0f;
            }
        }

        return overlayData;
    }

    private void generateRibbonData(char ribbon, int precision, float[] colorData) throws UnsupportedEncodingException {
        log.debug("Building ribbon 'Db;{};{};..'", ribbon, precision);

        getByteBuffer().append("\u001BDb;" + ribbon + ";" + precision + ";", charset);
        getByteBuffer().append(compactBits(precision, colorData));
        getByteBuffer().append(new byte[] {0x0D});
    }

    private ArrayList<Byte> compactBits(int precision, float[] colorData) {
        ArrayList<Byte> bytes = new ArrayList<>();

        int bits = precisionBits(precision);
        int empty = 8 - bits;

        for(int i = 0; i < colorData.length; i++) {
            byte b = 0;
            int captured = 0;

            b |= byteValue(colorData[i], precision) << empty;
            captured += 8 - empty;

            while(captured < 8 && (i + 1) < colorData.length) {
                int excess = bits - empty;

                if (excess > 0) { //because negative shifts don't go backwards
                    b |= byteValue(colorData[i + 1], precision) >> excess;
                } else {
                    b |= byteValue(colorData[i + 1], precision) << Math.abs(excess);
                }
                captured += bits - Math.max(0, excess);
                if (captured < 8 && excess <= 0) { i++; } //if we've eaten an entire color point but haven't filled the byte, increase index looking at

                empty = 8 - excess;
                if (empty > 8) { empty -= 8; } //wrap around so we never shift over a byte length
            }

            bytes.add(b);
        }

        return bytes;
    }

    private int precisionBits(int precision) {
        precision--;  // "128" is actually 0-127, subtract one
        int ones = 0;
        while(precision > 0) {
            if (precision % 2 != 0) { ones++; }
            precision /= 2;
        }

        return ones;
    }

    private byte byteValue(float value, int precision) {
        return (byte)(value * (precision - 1));
    }

    /**
     * Checks if the image width is a multiple of 8, and if it's not,
     * pads the image on the right side with blank pixels. <br />
     * Due to limitations on the EPL2 language, image widths must be a multiple
     * of 8.
     */
    private void validateImageWidth() {
        BufferedImage oldBufferedImage = bufferedImage;
        int height = oldBufferedImage.getHeight();
        int width = oldBufferedImage.getWidth();
        if (width % 8 != 0) {
            int newWidth = (width / 8 + 1) * 8;
            BufferedImage newBufferedImage = new BufferedImage(newWidth, height,
                                                               BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = newBufferedImage.createGraphics();
            g.drawImage(oldBufferedImage, 0, 0, null);
            g.dispose();
            setBufferedImage(newBufferedImage);
            init();
        }
    }
}
