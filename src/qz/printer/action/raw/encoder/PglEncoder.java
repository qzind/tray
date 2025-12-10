package qz.printer.action.raw.encoder;

import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.mono.MonoImageConverter;

import java.io.IOException;
import java.util.BitSet;

public class PglEncoder implements ImageEncoder {
    private final String logoId; // logo name (mandatory)
    private final boolean igpDots; // toggle IGP/PGL default resolution of 72dpi

    public PglEncoder(String logoId, boolean igpDots) {
        this.logoId = logoId;
        this.igpDots = igpDots;
    }

    @Override
    public byte[] encode(ImageConverter imageConverter) throws IOException, InvalidRawImageException {
        MonoImageConverter converter = (MonoImageConverter)imageConverter;
        ByteArrayBuilder byteBuffer = new ByteArrayBuilder();

        int w = converter.getWidth();
        int h = converter.getHeight();
        BitSet bitSet = converter.getImageAsBitSet();

        if(logoId == null || logoId.trim().isEmpty()) {
            throw new InvalidRawImageException("Printronix graphics require a logoId");
        }
        if(igpDots) {
            // IGP images cannot exceed 240x252
            if(w > 240 || h > 252) {
                throw new InvalidRawImageException("IGP dots is enabled; Size values HL/VL cannot exceed 240x252");
            }
        }

        // igpDots: true: Use IGP standard 60dpi/72dpi graphics (removes "DOTS" from raw command)
        // igpDots: false: Use the printer's native resolution (appends "DOTS" to raw command)
        byteBuffer.append("~LOGO", ";", logoId, ";", h, ";", w, ";", igpDots ? "" : "DOT;", "\n");

        /*
         * Printronix format
         *    [line];[black dots range];[more black dots range][newline]
         * e.g
         *    1;1-12;19-22;38-39
         */
        int bitSetIndex = 0;
        for(int lineNum = 1; lineNum <= h; lineNum++) {
            StringBuilder line = new StringBuilder();

            int start = -1;
            int end = -1;

            for(int pixelNum = 1; pixelNum <= w; pixelNum++) {
                if (bitSet.get(bitSetIndex)) {
                    if (start == -1) {
                        start = pixelNum;
                    }
                } else {
                    if (start != -1) {
                        end = pixelNum - 1;
                    }
                }

                // Handle trailing pixel
                if (pixelNum == w) {
                    end = pixelNum;
                }

                if (start != -1 && end != -1) {
                    if (start == end) {
                        // append a single dot
                        line.append(start).append(";");
                    } else {
                        // append a range of dots
                        line.append(start).append("-").append(end).append(";");
                    }
                    start = -1;
                    end = -1;
                }
                bitSetIndex++;
            }
            if (line.length() > 0) {
                // Remove trailing ";"
                if (line.charAt(line.length() - 1) == ';') {
                    line.replace(line.length() - 1, line.length(), "");
                }
                // Add line number
                line.insert(0, lineNum + ";");

                // Add to final commands
                byteBuffer.append(line).append("\n");
            }
        }
        return byteBuffer.append("END", "\n").toByteArray();
    }
}
