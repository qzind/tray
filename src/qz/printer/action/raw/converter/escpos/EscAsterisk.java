package qz.printer.action.raw.converter.escpos;

import qz.common.ByteArrayBuilder;
import qz.exception.InvalidRawImageException;
import qz.printer.action.raw.ByteAppender;
import qz.printer.action.raw.PixelGrid;
import qz.printer.action.raw.converter.EscPos;

import java.io.UnsupportedEncodingException;

public class EscAsterisk implements ByteAppender {
    private final EscPos converter;

    public EscAsterisk(EscPos converter) {
        this.converter = converter;
    }

    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException, InvalidRawImageException {
        int w = converter.getWidth();
        int h = converter.getHeight();
        int dotDensity = converter.getDotDensity();
        boolean legacyMode = converter.isLegacyMode();

        PixelGrid pixelGrid = converter.getImageAsPixelGrid();

        // set line height to the size of each chunk we will be sending
        int segmentHeight = dotDensity > 1 ? 24 : (dotDensity == 1 ? 8 : 16); // height will be handled explicitly below if striping
        // impact printers (U220, etc.) benefit from double-pass striping (odd/even) for higher quality (dotDensity = 1)
        boolean stripe = dotDensity == 1;
        int bytesNeeded = dotDensity <= 1? 1:3;

        if(legacyMode) {
            // temporarily set line spacing to 24 dots
            byteBuffer.append(new byte[] { 0x1B, 0x33, 24});
        }

        int offset = 0; // keep track of chunk offset currently being written
        boolean zeroPass = true; // track if this segment get rewritten with 1 pixel offset, always true if not striping

        while(offset < h) {
            // compute 2 byte value of the image width (documentation states width is 'nL' + ('nH' * 256))
            byte nL = (byte)((w % 256));
            byte nH = (byte)((w / 256));
            byteBuffer.append(new byte[] {0x1B, 0x2A, (byte)dotDensity, nL, nH});

            for(int x = 0; x < w; x++) {
                for(int bite = 0; bite < bytesNeeded; bite++) {
                    byte slice = 0;

                    // iterate bit for the byte - striping spans 2 bytes (taking every other bit) to be compacted down into one
                    for(int bit = (zeroPass? 0:1); bit < 8 * (stripe? 2:1); bit += (stripe? 2:1)) {
                        // get the y position of the current pixel being found
                        int y = offset + ((bite * 8) + bit);

                        // calculate the location of the pixel we want in the bit array and update the slice if it is supposed to be black
                        int i = (y * w) + x;
                        if(i < pixelGrid.size() && pixelGrid.get(i)) {
                            // append desired bit to current byte being built, remembering that bits go right to left
                            slice |= (byte)(1 << (7 - (bit - (zeroPass? 0:1)) / (stripe? 2:1)));
                        }
                    }

                    byteBuffer.append(slice);
                }
            }

            // move print head down to next segment (or offset by one if striping)
            if(stripe) {
                if(zeroPass) {
                    byteBuffer.append(new byte[] {0x1B, 0x4A, 0x01}); // only shift down 1 pixel for the next pass
                } else {
                    byteBuffer.append(new byte[] {0x1B, 0x4A, (byte)(segmentHeight - 1)}); // shift down remaining pixels
                    offset += 8 * bytesNeeded; // only shift offset on every other pass (along with segments)
                }

                zeroPass = !zeroPass;
            } else {
                if(legacyMode) {
                    // render a newline to bump the print head down
                    byteBuffer.append(new byte[] {10});
                } else {
                    //shift down for next segment
                    byteBuffer.append(new byte[] {0x1B, 0x4A, (byte)segmentHeight});
                }
                offset += 8 * bytesNeeded;
            }
        }

        if(legacyMode) {
            // Restore line spacing to 30 dots
            byteBuffer.append(new byte[] { 0x1B, 0x33, 30});
        }
        return byteBuffer;
    }
}
