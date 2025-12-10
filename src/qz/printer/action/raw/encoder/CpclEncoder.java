package qz.printer.action.raw.encoder;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.mono.MonoImageConverter;

import java.io.IOException;

public class CpclEncoder implements ImageEncoder {
    private final int x;
    private final int y;

    public CpclEncoder() {
        this(0, 0);
    }

    public CpclEncoder(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public byte[] encode(ImageConverter imageConverter) throws IOException {
        MonoImageConverter converter = (MonoImageConverter)imageConverter;
        ByteArrayBuilder byteBuffer = new ByteArrayBuilder();

        int w = converter.getWidth() / 8;
        int h = converter.getHeight();

        return byteBuffer.append("EG", " ", w, " ", h, " ", x, " ", y, " ", converter.convertImageToHexString(), new byte[] {13, 10}).toByteArray();
    }
}
