package qz.printer.action.raw.converter;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.MonoImageConverter;

import java.io.UnsupportedEncodingException;

public class Sbpl extends MonoImageConverter {
    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException {
        String w = String.format("%03d", getWidth() / 8);
        String h = String.format("%03d", getHeight() / 8);

        return byteBuffer.append(esc('G'), "H", w, h, convertImageToHexString());
    }

    /**
     * Simulate Sbpl's <code>&lt;A&gt;</code>, <code>&lt;Z&gt;</code> which
     * signifies "ESC + A", "ESC + Z", etc.
     */
    private static String esc(char c) {
        return "\u001B" + c;
    }

    @Override
    public String getHeader() {
        return esc('A') +
                esc('H') + "0000" + // horizonal print position
                esc('V') + "0000"; // vertical print position
    }

    @Override
    public String getFooter() {
       return esc('Q') + "1" +
               esc('Z');
    }
}
