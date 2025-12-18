package qz.printer.action.raw.converter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.LanguageType;
import qz.printer.action.raw.MonoImageConverter;
import qz.printer.action.raw.PixelGrid;
import qz.utils.ConnectionUtilities;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Evolis extends ImageConverter {
    private static final Logger log = LogManager.getLogger(Evolis.class);

    private int precision;
    private Object overlay;

    public void setParams(JSONObject params) {
        this.precision = params.optInt("precision", 128);
        this.overlay = params.opt("overlay");
    }

    @Override
    public String getHeader() {
        return
                "\u001BPps;0\r" +   // Enable raw/disable driver printer parameter supervision
                "\u001BPwr;0\r" +   // Landscape (zero degree) orientation
                "\u001BWcb;k;0\r" + // Clear card memory
                "\u001BSs\r";       // Start of sequence
    }

    @Override
    public String getFooter() {
        return "\u001BSe\r";       // End of sequence
    }

    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws InvalidRawImageException {
        int w = getWidth();
        int h = getHeight();

        try {
            ArrayList<float[]> cymkData = convertToCYMK(getBufferedImage(), w, h);

            // Y,M,C,K,O ribbon
            appendRibbonDataTo(byteBuffer, 'y', precision, cymkData.get(1));
            appendRibbonDataTo(byteBuffer, 'm', precision, cymkData.get(2));
            appendRibbonDataTo(byteBuffer, 'c', precision, cymkData.get(0));

            // K(black) and O(overlay) are always precision 2
            appendRibbonDataTo( byteBuffer, 'k', 2, cymkData.get(3));

            if (overlay != null) {
                appendRibbonDataTo(byteBuffer, 'o', 2, parseOverlay(overlay, w, h));
            }
        }
        catch(IOException | JSONException e) {
            throw new InvalidRawImageException(e.getMessage(), e);
        }

        return byteBuffer;
    }

    private ArrayList<float[]> convertToCYMK(BufferedImage bufferedImage, int width, int height) throws IOException {
        int[] pixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);

        float[] cyan = new float[pixels.length];
        float[] yellow = new float[pixels.length];
        float[] magenta = new float[pixels.length];
        float[] black = new float[pixels.length];

        for(int i = 0; i < pixels.length; i++) {
            float[] rgb = new Color(pixels[i]).getRGBColorComponents(null);
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

    /**
     * Parses the provided JSON|String|boolean data for determining the clear/overlay layer
     */
    private static float[] parseOverlay(Object overlay, int width, int height) throws IOException, JSONException {
        float[] overlayData = new float[width * height];

        if (overlay instanceof JSONArray) {
            // array of rectangles
            JSONArray masterBlock = (JSONArray)overlay;
            for(int i = 0; i < masterBlock.length(); i++) {
                JSONArray block = masterBlock.getJSONArray(i);
                if (block != null && block.length() == 4) {
                    for(int y = block.getInt(1) - 1; y < block.getInt(3); y++) {
                        int off = (y * width);
                        for(int x = block.getInt(0) - 1; x < block.getInt(2); x++) {
                            if ((off + x) >= 0 && (off + x) < overlayData.length) {
                                overlayData[off + x] = 1.0f;
                            }
                        }
                    }
                }
            }
        } else if (overlay instanceof String) {
            // image mask
            BufferedImage maskImage = ImageIO.read(ConnectionUtilities.getInputStream((String)overlay, true));
            PixelGrid mask = MonoImageConverter.generateBlackPixels(maskImage, LanguageType.EVOLIS, MonoImageConverter.Quantization.BLACK, 127);
            for(int i = 0; i < mask.size(); i++) {
                overlayData[i] = (mask.get(i)? 1.0f:0.0f);
            }
        } else if (overlay instanceof Boolean && (boolean)overlay) {
            // boolean coat
            Arrays.fill(overlayData, 1.0f);
        }

        return overlayData;
    }

    private static void appendRibbonDataTo(ByteArrayBuilder byteBuffer, char ribbon, int precision, float[] colorData) throws IOException {
        log.debug("Building ribbon 'Db;{};{};...'", ribbon, precision);

        byteBuffer.append("\u001BDb;", ribbon, ";", precision, ";");
        byteBuffer.append(compactBits(precision, colorData));
        byteBuffer.append(new byte[] {0x0D});
    }

    private static ArrayList<Byte> compactBits(int precision, float[] colorData) {
        ArrayList<Byte> bytes = new ArrayList<>();

        int bits = precisionBits(precision);
        int empty = 8 - bits;

        for(int i = 0; i < colorData.length; i++) {
            byte b = 0;
            int captured = 0;

            b |= (byte)(byteValue(colorData[i], precision) << empty);
            captured += 8 - empty;

            while(captured < 8 && (i + 1) < colorData.length) {
                int excess = bits - empty;

                if (excess > 0) { //because negative shifts don't go backwards
                    b |= (byte)(byteValue(colorData[i + 1], precision) >> excess);
                } else {
                    b |= (byte)(byteValue(colorData[i + 1], precision) << Math.abs(excess));
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

    private static int precisionBits(int precision) {
        precision--;  // "128" is actually 0-127, subtract one
        int ones = 0;
        while(precision > 0) {
            if (precision % 2 != 0) { ones++; }
            precision /= 2;
        }

        return ones;
    }

    private static byte byteValue(float value, int precision) {
        return (byte)(value * (precision - 1));
    }
}
