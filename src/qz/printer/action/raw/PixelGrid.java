package qz.printer.action.raw;

import java.util.BitSet;

/**
 * BitSet-backed pixel array
 */
public class PixelGrid {
    private final int width;
    private final int height;
    private final BitSet pixels;

    public PixelGrid(int width, int height) {
        this(width, height, new BitSet(width * height));
    }

    private PixelGrid(int width, int height, BitSet pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public void set(int index, boolean isBlack) {
        if(index >= width * height) {
            throw new IllegalArgumentException("Location exceeds bounds.");
        }
        pixels.set(index, isBlack);
    }

    public boolean get(int index) {
        if(index >= width * height) {
            throw new IllegalArgumentException("Location exceeds bounds.");
        }
        return pixels.get(index);
    }

    public void set(int x, int y, boolean isBlack) {
        if(x >= width || y >= height) {
            throw new IllegalArgumentException("Location exceeds bounds.");
        }
        pixels.set(y * width + x, isBlack);
    }

    public boolean get(int x, int y) {
        if(x >= width || y >= height) {
            throw new IllegalArgumentException("Location exceeds bounds.");
        }
        return pixels.get(y * width + x);
    }

    public int size() {
        return width * height;
    }

    public PixelGrid getSlice(int startY, int sliceHeight) {
        if (startY + sliceHeight > this.height) {
            throw new IllegalArgumentException("Slice exceeds vertical bounds.");
        }

        // Calculate bit range
        int fromIndex = startY * width;
        int toIndex = (startY + sliceHeight) * width;

        // BitSet.get(from, to) returns a new BitSet containing the range
        BitSet slicedBits = this.pixels.get(fromIndex, toIndex);

        return new PixelGrid(this.width, sliceHeight, slicedBits);
    }
}
