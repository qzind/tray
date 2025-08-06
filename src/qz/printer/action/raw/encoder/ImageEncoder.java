package qz.printer.action.raw.encoder;

import java.awt.image.BufferedImage;

public interface ImageEncoder {
    byte[] encode(BufferedImage image);
}
