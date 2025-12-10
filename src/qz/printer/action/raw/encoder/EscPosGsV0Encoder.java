package qz.printer.action.raw.encoder;

import qz.common.ByteArrayBuilder;
import qz.printer.action.raw.ImageConverter;
import qz.printer.action.raw.mono.MonoImageConverter;

import java.io.IOException;
import java.util.BitSet;

public class EscPosGsV0Encoder implements ImageEncoder {
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

            // Append the GS v 0 command for the slice
            appendGsV0Command(w, slicedHeight, sliceSet);
        }

        return byteBuffer.toByteArray();
    }

    /**
     * Generates the GS v 0 command for the given BitSet slice.
     * Command: GS v 0 m xL xH yL yH d1...dk
     */
    private void appendGsV0Command(int width, int height, BitSet slice) throws IOException {
        // Calculate bytes needed for image data
        int bytesPerRow = (width + 7) / 8; // Round up to the nearest byte

        // Calculate command parameters
        byte[] imageData = slice.toByteArray();
        int xL = bytesPerRow & 0xFF;
        int xH = (bytesPerRow >> 8) & 0xFF;
        int yL = height & 0xFF;
        int yH = (height >> 8) & 0xFF;

        // Build command
        byteBuffer.append(0x1D); // GS
        byteBuffer.append('v');  // 0x76
        byteBuffer.append('0');  // 0x30
        byteBuffer.append(0);    // m = 0 (normal mode)
        byteBuffer.append(xL);
        byteBuffer.append(xH);
        byteBuffer.append(yL);
        byteBuffer.append(yH);
        byteBuffer.append(imageData);
    }
}
