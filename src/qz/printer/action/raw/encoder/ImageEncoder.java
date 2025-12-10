package qz.printer.action.raw.encoder;

import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.ImageConverter;

import java.io.IOException;

public interface ImageEncoder {
    byte[] encode(ImageConverter imageConverter) throws IOException, InvalidRawImageException;
}
