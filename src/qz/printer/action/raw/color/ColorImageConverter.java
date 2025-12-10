package qz.printer.action.raw.color;

import org.codehaus.jettison.json.JSONObject;
import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.ImageConverterType;
import qz.printer.action.raw.LanguageType;
import qz.printer.action.raw.encoder.EvolisEncoder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ColorImageConverter implements ImageConverter {
    private final LanguageType languageType;
    private final BufferedImage bufferedImage;

    public ColorImageConverter(BufferedImage bufferedImage, LanguageType languageType) {
        this.bufferedImage = bufferedImage;
        this.languageType = languageType;
    }

    @Override
    public byte[] getImageCommand(JSONObject opt) throws InvalidRawImageException, IOException {
        switch(languageType) {
            case EVOLIS:
                int precision = opt.optInt("precision", 128);
                Object overlay = opt.opt("overlay");
                return new EvolisEncoder(precision, overlay).encode(this);
            default:
                throw new InvalidRawImageException(languageType + " image conversion is not yet supported.");
        }
    }

    @Override
    public ImageConverterType getImageType() {
        return ImageConverterType.COLOR;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }
}
