package qz.printer.action.raw.encoder;

import qz.common.ByteArrayBuilder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class GsV0Encoder implements ImageEncoder {
    @Override
    public byte[] encode(BufferedImage image) {
        ByteArrayBuilder builder = new ByteArrayBuilder();
        if (image == null) return builder.getByteArray();

        int width = image.getWidth();
        int height = image.getHeight();
        final int sliceHeight = 24;

        for (int y = 0; y < height; y += sliceHeight) {
            int slicedHeight = Math.min(sliceHeight, height - y);

            // Create a sliced image from the full image
            BufferedImage slicedImage = image.getSubimage(0, y, width, slicedHeight);

            // Append the GS v 0 command for the slice
            byte[] command = generateGsV0Command(slicedImage);
            builder.append(command);
        }

        return builder.getByteArray();
    }

    /**
     * Generates the GS v 0 command for the given image slice.
     * Command: GS v 0 m xL xH yL yH d1...dk
     */
    private byte[] generateGsV0Command(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Convert image to monochrome
        BufferedImage monoImage = convertToMonochrome(image);

        // Calculate bytes needed for image data
        int bytesPerRow = (width + 7) / 8; // Round up to the nearest byte
        byte[] imageData = new byte[bytesPerRow * height];

        // Convert image to the bit array
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = monoImage.getRGB(x, y);
                // If the pixel is black (or dark), set bit to 1
                boolean isBlack = (rgb & 0xFF) < 128;
                if (isBlack) {
                    int byteIndex = y * bytesPerRow + (x / 8);
                    int bitIndex = 7 - (x % 8);
                    imageData[byteIndex] |= (byte) (1 << bitIndex);
                }
            }
        }

        // Calculate command parameters
        int xL = bytesPerRow & 0xFF;
        int xH = (bytesPerRow >> 8) & 0xFF;
        int yL = height & 0xFF;
        int yH = (height >> 8) & 0xFF;

        // Build command
        ByteArrayOutputStream command = new ByteArrayOutputStream();
        command.write(0x1D); // GS
        command.write('v');  // 0x76
        command.write('0');  // 0x30
        command.write(0);    // m = 0 (normal mode)
        command.write(xL);
        command.write(xH);
        command.write(yL);
        command.write(yH);
        command.write(imageData, 0, imageData.length);

        return command.toByteArray();
    }

    /**
     * Converts a BufferedImage to monochrome (1-bit) format
     */
    private static BufferedImage convertToMonochrome(BufferedImage original) {
        BufferedImage mono = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = mono.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
        return mono;
    }
}
