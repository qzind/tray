package qz.printer.action.raw.encoder;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.mono.MonoImageConverter;

import java.io.IOException;

public class ZplEncoder implements ImageEncoder {
    @Override
    public byte[] encode(ImageConverter imageConverter) throws IOException {
        MonoImageConverter converter = (MonoImageConverter)imageConverter;
        ByteArrayBuilder byteBuffer = new ByteArrayBuilder();

        String zplHexAsString = converter.convertImageToHexString();
        int byteLen = zplHexAsString.length() / 2;
        int perRow = byteLen / converter.getHeight();

        return byteBuffer.append("^GFA,", byteLen, ",", byteLen, ",", perRow, ",", zplHexAsString).toByteArray();
    }
}
