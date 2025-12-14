package qz.printer.action.raw.converter;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.MonoImageConverter;

import java.io.UnsupportedEncodingException;

public class Sbpl extends MonoImageConverter {
    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException {
        String w = String.format("%03d", getWidth() / 8);
        String h = String.format("%03d", getHeight() / 8);

        return byteBuffer.append(new byte[] {27}, "GH", w, h, convertImageToHexString());
    }
}
