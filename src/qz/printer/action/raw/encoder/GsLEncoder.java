package qz.printer.action.raw.encoder;

import qz.common.ByteArrayBuilder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class GsLEncoder implements ImageEncoder {
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

            // Append the store graphic command
            byte[] storeCommand = generateStoreCommand(slicedImage);
            builder.append(storeCommand);

            // Append the print graphic command
            byte[] printCommand = generatePrintCommand();
            builder.append(printCommand);
        }

        return builder.getByteArray();
    }

    /**
     * Generates the store graphic command (GS ( L with fn = 112) for the given image
     */
    private static byte[] generateStoreCommand(BufferedImage image) {
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
        ByteArrayOutputStream command = new ByteArrayOutputStream();
        command.write(0x1D); // GS
        command.write('(');
        command.write('L');
        command.write(pL);
        command.write(pH);
        command.write(m);
        command.write(fn);
        command.write(a);
        command.write(bx);
        command.write(by);
        command.write(c);
        command.write(xL);
        command.write(xH);
        command.write(yL);
        command.write(yH);
        command.write(imageData, 0, imageData.length);

        return command.toByteArray();
    }

    /**
     * Generates the print graphic command (GS ( L with fn = 50)
     */
    public static byte[] generatePrintCommand() {
        ByteArrayOutputStream command = new ByteArrayOutputStream();
        command.write(0x1D); // GS
        command.write('(');
        command.write('L');
        command.write(2); // pL
        command.write(0); // pH
        command.write(48); // m
        command.write(50); // Function 50: Print the graphics data in the print buffer

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
