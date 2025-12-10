package qz.printer.action.raw.encoder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.color.ColorImageConverter;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.LanguageType;
import qz.printer.action.raw.mono.MonoImageConverter;
import qz.printer.action.raw.mono.Quantization;
import qz.utils.ConnectionUtilities;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;

public class EvolisEncoder implements ImageEncoder {
    private static final Logger log = LogManager.getLogger(EvolisEncoder.class);

    private final int precision;
    private final Object overlay;
    private final ByteArrayBuilder byteBuffer;

    public EvolisEncoder(int precision, Object overlay) {
        this.precision = precision;
        this.overlay = overlay;
        this.byteBuffer = new ByteArrayBuilder();
    }

    @Override
    public byte[] encode(ImageConverter imageConverter) throws IOException, InvalidRawImageException {
        ColorImageConverter converter = (ColorImageConverter)imageConverter;
        BufferedImage bufferedImage = converter.getBufferedImage();
        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();

        try {
            ArrayList<float[]> cymkData = convertToCYMK(bufferedImage, w, h);

            // Y,M,C,K,O ribbon
            appendRibbonData('y', precision, cymkData.get(1));
            appendRibbonData('m', precision, cymkData.get(2));
            appendRibbonData('c', precision, cymkData.get(0));

            // K(black) and O(overlay) are always precision 2
            appendRibbonData('k', 2, cymkData.get(3));

            if (overlay != null) {
                appendRibbonData('o', 2, parseOverlay(overlay, w, h));
            }
        }
        catch(IOException | JSONException e) {
            throw new InvalidRawImageException(e.getMessage(), e);
        }

        return byteBuffer.toByteArray();
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
    private float[] parseOverlay(Object overlay, int width, int height) throws IOException, JSONException {
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
            BitSet mask = MonoImageConverter.generateBlackPixels(LanguageType.EVOLIS, Quantization.BLACK, 127, maskImage);
            for(int i = 0; i < mask.size(); i++) {
                overlayData[i] = (mask.get(i)? 1.0f:0.0f);
            }
        } else if (overlay instanceof Boolean && (boolean)overlay) {
            // boolean coat
            Arrays.fill(overlayData, 1.0f);
        }

        return overlayData;
    }

    private void appendRibbonData(char ribbon, int precision, float[] colorData) throws IOException {
        log.debug("Building ribbon 'Db;{};{};...'", ribbon, precision);

        byteBuffer.append("\u001BDb;", ribbon, ";", precision, ";");
        byteBuffer.append(compactBits(precision, colorData));
        byteBuffer.append(new byte[] {0x0D});
    }

    private ArrayList<Byte> compactBits(int precision, float[] colorData) {
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
}
