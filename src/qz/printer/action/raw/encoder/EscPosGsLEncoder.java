package qz.printer.action.raw.encoder;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.mono.MonoImageConverter;

import java.awt.*;
import java.io.IOException;
import java.util.BitSet;

public class EscPosGsLEncoder implements ImageEncoder {
    private ByteArrayBuilder byteBuffer;

    @Override
    public byte[] encode(ImageConverter imageConverter) throws IOException {
        MonoImageConverter converter = (MonoImageConverter)imageConverter;
        byteBuffer = new ByteArrayBuilder();

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

            // Append the store graphic command
            appendStoreCommand(w, sliceHeight, sliceSet);

            // Append the print graphic command
            appendPrintCommand();
        }

        return byteBuffer.toByteArray();
    }

    /**
     * Generates the store graphic command (GS ( L with fn = 112) for the given BitSet slice
     */
    private void appendStoreCommand(int width, int height, BitSet slice) throws IOException {
        // Calculate command parameters
        byte[] imageData = slice.toByteArray();
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
        byteBuffer.append(0x1D); // GS
        byteBuffer.append('(');
        byteBuffer.append('L');
        byteBuffer.append(pL);
        byteBuffer.append(pH);
        byteBuffer.append(m);
        byteBuffer.append(fn);
        byteBuffer.append(a);
        byteBuffer.append(bx);
        byteBuffer.append(by);
        byteBuffer.append(c);
        byteBuffer.append(xL);
        byteBuffer.append(xH);
        byteBuffer.append(yL);
        byteBuffer.append(yH);
        byteBuffer.append(imageData);
    }

    /**
     * Generates the print graphic command (GS ( L with fn = 50)
     */
    public void appendPrintCommand() throws IOException {
        byteBuffer.append(0x1D); // GS
        byteBuffer.append('(');
        byteBuffer.append('L');
        byteBuffer.append(2); // pL
        byteBuffer.append(0); // pH
        byteBuffer.append(48); // m
        byteBuffer.append(50); // Function 50: Print the graphics data in the print buffer
    }
}
