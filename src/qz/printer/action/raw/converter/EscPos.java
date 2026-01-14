package qz.printer.action.raw.converter;

import org.codehaus.jettison.json.JSONObject;
import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.MonoImageConverter;
import qz.printer.action.raw.converter.escpos.EscAsterisk;
import qz.printer.action.raw.converter.escpos.GsL;
import qz.printer.action.raw.converter.escpos.GsV0;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class EscPos extends MonoImageConverter {
    /** Raw image encoding option */
    public enum ImageEncoding {
        ESC_ASTERISK,
        GS_L,
        GS_V_0;

        public static ImageEncoding parse(String input, ImageEncoding fallback) {
            for(ImageEncoding type : ImageEncoding.values()) {
                if(type.name().equalsIgnoreCase(input)) {
                    return type;
                }
            }
            return fallback;
        }
    }

    static private final int DEFAULT_DOT_DENSITY = 32;

    public int getDotDensity() {
        return dotDensity;
    }

    public boolean isLegacyMode() {
        return legacyMode;
    }

    private int dotDensity;
    private boolean legacyMode;
    private ImageEncoding imageEncoding;

    public void setParams(JSONObject params) {
        super.setParams(params);

        this.imageEncoding = ImageEncoding.parse(params.optString("imageEncoding"), ImageEncoding.ESC_ASTERISK);

        // ESC* only
        int parsed = parseDotDensity(params.optString("dotDensity"), DEFAULT_DOT_DENSITY);
        this.legacyMode = (parsed < 0);
        this.dotDensity = Math.abs(parsed);
    }

    /**
     * Epson ESC/POS image slice appender
     * <p>
     * Images are read as one long array of black or white pixels, as scanned top to bottom and left to right.
     * Printer format needs this sent in height chunks in bytes (normally 3, for 24 pixels at a time) for each x position along a segment,
     * and repeated for each segment of height over the byte limit.
     * </p>
     */
    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException, InvalidRawImageException {
        // Special handling for other ESC/POS encodings
        switch(imageEncoding) {
            case GS_L:
                return new GsL(this).appendTo(byteBuffer);
            case GS_V_0:
                return new GsV0(this).appendTo(byteBuffer);
            case ESC_ASTERISK:
            default:
                return new EscAsterisk(this).appendTo(byteBuffer);
        }

    }

    @Override
    public String getHeader() {
        return "";
    }

    @Override
    public String getFooter() {
        return "";
    }

    private static int parseDotDensity(String dotDensity, int fallback) {
        if(dotDensity == null || dotDensity.isBlank()) {
            return fallback;
        }
        switch(dotDensity.toLowerCase(Locale.ENGLISH)) {
            case "single":
                return 32;
            case "double":
                return 33;
            case "triple":
                return 39;
            // negative: legacy mode
            case "single-legacy":
                return -32;
            case "double-legacy":
                return -33;
            default:
                try {
                    return Integer.parseInt(dotDensity);
                } catch(NumberFormatException ignore) {
                    return fallback;
                }
        }
    }
}
