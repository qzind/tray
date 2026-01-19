package qz.printer.action.raw.converter.escpos;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.ByteAppender;
import qz.printer.action.raw.PixelGrid;
import qz.printer.action.raw.converter.EscPos;
import qz.utils.ByteUtilities;

import java.io.UnsupportedEncodingException;

public class GsV0 implements ByteAppender {
    private final EscPos converter;
    private static final int SLICE_HEIGHT = 24;

    public GsV0(EscPos converter) {
        this.converter = converter;
    }

    @Override
    public ByteArrayBuilder appendTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException {
        PixelGrid pixelGrid = converter.getImageAsPixelGrid();
        int w = converter.getWidth();
        int h = converter.getHeight();

        for (int y = 0; y < h; y += SLICE_HEIGHT) {
            int slicedHeight = Math.min(SLICE_HEIGHT, h - y);

            // isolate a sliced BitSet from the full BitSet
            PixelGrid slice = pixelGrid.getSlice(y, slicedHeight);

            // Append the GS v 0 command for the slice
            appendGsV0CommandTo(byteBuffer, w, slicedHeight, slice);
        }

        return byteBuffer;
    }

    /**
     * Generates the GS v 0 command for the given BitSet slice.
     * Command: GS v 0 m xL xH yL yH d1...dk
     */
    private static void appendGsV0CommandTo(ByteArrayBuilder byteBuffer, int width, int height, PixelGrid slice) throws UnsupportedEncodingException {
        // Calculate bytes needed for image data
        int bytesPerRow = (width + 7) / 8; // Round up to the nearest byte

        // Calculate command parameters
        byte[] imageData = ByteUtilities.toByteArray(slice);
        int xL = bytesPerRow & 0xFF;
        int xH = (bytesPerRow >> 8) & 0xFF;
        int yL = height & 0xFF;
        int yH = (height >> 8) & 0xFF;

        // Build command
        byteBuffer
                .appendRaw(0x1D) // GS
                .append('v')  // 0x76
                .append('0')  // 0x30
                .appendRaw(0)    // m = 0 (normal mode)
                .appendRaw(xL)
                .appendRaw(xH)
                .appendRaw(yL)
                .appendRaw(yH)
                .append(imageData);
    }
}
