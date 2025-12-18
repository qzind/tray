package qz.printer.action.raw.converter;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.MonoImageConverter;

import java.io.UnsupportedEncodingException;

public class Zpl extends MonoImageConverter {
    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException {
       String zplHexAsString = convertImageToHexString();
        int byteLen = zplHexAsString.length() / 2;
        int perRow = byteLen / getHeight();

        return byteBuffer.append("^GFA,", byteLen, ",", byteLen, ",", perRow, ",", zplHexAsString);
    }

    @Override
    public String getHeader() {
        return "^XA\n";
    }

    @Override
    public String getFooter() {
        return "^XZ\n";
    }
}
