package qz.printer.action.raw.converter.escpos;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.ByteAppender;
import qz.printer.action.raw.PixelGrid;
import qz.printer.action.raw.converter.EscPos;
import qz.utils.ByteUtilities;

import java.io.UnsupportedEncodingException;

public class GsL implements ByteAppender {
    private final EscPos converter;
    private static final int SLICE_HEIGHT = 24;

    public GsL(EscPos converter) {
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

            // Append the store graphic command
            appendStoreCommandTo(byteBuffer, w, slicedHeight, slice);

            // Append the print graphic command
            appendPrintCommandTo(byteBuffer);
        }

        return byteBuffer;
    }

    /**
     * Generates the store graphic command (GS ( L with fn = 112) for the given BitSet slice
     */
    private static void appendStoreCommandTo(ByteArrayBuilder byteBuffer, int width, int height, PixelGrid slice) throws UnsupportedEncodingException {
        // Calculate command parameters
        byte[] imageData = ByteUtilities.toByteArray(slice);
        int dataLength = imageData.length + 10; // 10 bytes for parameters
        int pL = dataLength & 0xFF;
        int pH = (dataLength >> 8) & 0xFF;
        int m = 48; // Command header
        int fn = 112; // Function 112: Store the graphics data in the print buffer (raster format)
        int a = 48; // Normal mode
        int bx = 1; // Horizontal scale
        int by = 1; // Vertical scale
        int c = 49; // Single color
        int xL = width & 0xFF;
        int xH = (width >> 8) & 0xFF;
        int yL = height & 0xFF;
        int yH = (height >> 8) & 0xFF;

        // Build command
        byteBuffer
                .appendRaw(0x1D) // GS
                .append('(')
                .append('L')
                .appendRaw(pL)
                .appendRaw(pH)
                .appendRaw(m)
                .appendRaw(fn)
                .appendRaw(a)
                .appendRaw(bx)
                .appendRaw(by)
                .appendRaw(c)
                .appendRaw(xL)
                .appendRaw(xH)
                .appendRaw(yL)
                .appendRaw(yH)
                .append(imageData);
    }

    /**
     * Generates the print graphic command (GS ( L with fn = 50)
     */
    public void appendPrintCommandTo(ByteArrayBuilder byteBuffer) throws UnsupportedEncodingException {
        byteBuffer
                .appendRaw(0x1D) // GS
                .append('(')
                .append('L')
                .appendRaw(2) // pL
                .appendRaw(0) // pH
                .appendRaw(48) // m
                .appendRaw(50); // Function 50: Print the graphics data in the print buffer
    }
}
