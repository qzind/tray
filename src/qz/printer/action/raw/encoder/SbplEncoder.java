package qz.printer.action.raw.encoder;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.mono.MonoImageConverter;

import java.io.IOException;

public class SbplEncoder implements ImageEncoder  {
    @Override
    public byte[] encode(ImageConverter imageConverter) throws IOException {
        MonoImageConverter converter = (MonoImageConverter)imageConverter;
        ByteArrayBuilder byteBuffer = new ByteArrayBuilder();

        String w = String.format("%03d", converter.getWidth() / 8);
        String h = String.format("%03d", converter.getHeight() / 8);

        return byteBuffer.append(new byte[] {27}, "GH", w, h, converter.convertImageToHexString()).toByteArray();
    }
}
