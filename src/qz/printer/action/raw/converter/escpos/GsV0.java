package qz.printer.action.raw.converter.escpos;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.ByteAppender;
import qz.printer.action.raw.MonoImageConverter;
import qz.printer.action.raw.converter.EscPos;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;

public class GsV0 implements ByteAppender {
    private final EscPos converter;

    public GsV0(EscPos converter) {
        this.converter = converter;
    }

    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException {
        BitSet bitSet = converter.getImageAsBitSet();
        int w = converter.getWidth();
        int h = converter.getHeight();
        final int sliceHeight = 24;

        for (int y = 0; y < h; y += sliceHeight) {
            int slicedHeight = Math.min(sliceHeight, h - y);

            // isolate a sliced BitSet from the full BitSet
            int start = slicedHeight * y;
            int end = Math.min(start + w, bitSet.size());
            BitSet sliceSet = bitSet.get(start, end);

            // Append the GS v 0 command for the slice
            appendGsV0CommandTo(byteBuffer, w, slicedHeight, sliceSet);
        }

        return byteBuffer;
    }

    /**
     * Generates the GS v 0 command for the given BitSet slice.
     * Command: GS v 0 m xL xH yL yH d1...dk
     */
    private static void appendGsV0CommandTo(ByteArrayBuilder byteBuffer, int width, int height, BitSet slice) throws UnsupportedEncodingException {
        // Calculate bytes needed for image data
        int bytesPerRow = (width + 7) / 8; // Round up to the nearest byte

        // Calculate command parameters
        byte[] imageData = MonoImageConverter.toBytes(slice);
        int xL = bytesPerRow & 0xFF;
        int xH = (bytesPerRow >> 8) & 0xFF;
        int yL = height & 0xFF;
        int yH = (height >> 8) & 0xFF;

        // Build command
        byteBuffer
                .append(0x1D) // GS
                .append('v')  // 0x76
                .append('0')  // 0x30
                .append(0)    // m = 0 (normal mode)
                .append(xL)
                .append(xH)
                .append(yL)
                .append(yH)
                .append(imageData);
    }
}
