package qz.printer.action.html;

import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoadListener;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageLoaderFactory;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage;
import com.sun.javafx.iio.jpeg.JPEGImageLoaderFactory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEG;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFReader;
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

final class JpegOrientationLoaderFactory implements ImageLoaderFactory {
    private static final JpegOrientationLoaderFactory INSTANCE = new JpegOrientationLoaderFactory();

    private final ImageLoaderFactory delegate = JPEGImageLoaderFactory.getInstance();

    private JpegOrientationLoaderFactory() {}

    static JpegOrientationLoaderFactory getInstance() {
        return INSTANCE;
    }

    static void register() {
        ImageStorage.getInstance().addImageLoaderFactory(getInstance());
    }

    @Override
    public ImageFormatDescription getFormatDescription() {
        return delegate.getFormatDescription();
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public ImageLoader createImageLoader(InputStream input) throws IOException {
        byte[] jpeg = input.readAllBytes();
        ExifOrientation orientation = readOrientation(jpeg);
        ImageLoader loader = delegate.createImageLoader(new ByteArrayInputStream(jpeg));
        return switch(orientation) {
            case NORMAL -> loader;
            default -> new JpegOrientationLoader(loader, orientation);
        };
    }

    private static ExifOrientation readOrientation(byte[] jpeg) {
        try (ImageInputStream input = new ByteArrayImageInputStream(jpeg)) {
            List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(input, JPEG.APP1, "Exif");
            if (segments.isEmpty()) {
                return ExifOrientation.NORMAL;
            }

            try (InputStream segment = segments.getFirst().data()) {
                if (segment.read() != 0) {
                    return ExifOrientation.NORMAL;
                }

                try (ImageInputStream tiff = new MemoryCacheImageInputStream(segment)) {
                    Directory directory = new TIFFReader().read(tiff);
                    return ExifOrientation.fromValue(directory.getEntryById(TIFF.TAG_ORIENTATION));
                }
            }
        }
        catch(IOException | RuntimeException ignored) {}

        return ExifOrientation.NORMAL;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    static ImageFrame transform(ImageFrame frame, ExifOrientation orientation) {
        if (orientation == ExifOrientation.NORMAL || !(frame.getImageData() instanceof ByteBuffer source)) {
            return frame;
        }

        int bands;
        try {
            bands = ImageStorage.getInstance().getNumBands(frame.getImageType());
        }
        catch(IllegalArgumentException e) {
            return frame;
        }

        int sourceWidth = frame.getWidth();
        int sourceHeight = frame.getHeight();
        int targetWidth = swapsDimensions(orientation) ? sourceHeight : sourceWidth;
        int targetHeight = swapsDimensions(orientation) ? sourceWidth : sourceHeight;
        int targetSize;
        try {
            targetSize = Math.multiplyExact(Math.multiplyExact(targetWidth, targetHeight), bands);
        }
        catch(ArithmeticException e) {
            return frame;
        }

        source = source.duplicate();
        int sourceStart = source.position();
        ByteBuffer target = ByteBuffer.allocate(targetSize);

        for(int y = 0; y < sourceHeight; y++) {
            for(int x = 0; x < sourceWidth; x++) {
                int targetX, targetY;
                switch (orientation.degrees) {
                    case 90  -> { targetX = sourceHeight - 1 - y; targetY = x; }
                    case 180 -> { targetX = sourceWidth - 1 - x;  targetY = sourceHeight - 1 - y; }
                    case 270 -> { targetX = y;                 targetY = sourceWidth - 1 - x; }
                    default  -> { targetX = x;                 targetY = y; }
                }

                if (orientation.isMirrored) {
                    targetX = targetWidth - 1 - targetX;
                }

                int sourceOffset = sourceStart + y * frame.getStride() + x * bands;
                int targetOffset = (targetY * targetWidth + targetX) * bands;
                for(int band = 0; band < bands; band++) {
                    target.put(targetOffset + band, source.get(sourceOffset + band));
                }
            }
        }

        return new ImageFrame(frame.getImageType(), target, targetWidth, targetHeight,
                targetWidth * bands, frame.getPalette(), frame.getPaletteIndexBits(),
                frame.getPixelScale(), copyMetadata(frame.getMetadata(), targetWidth, targetHeight));
    }

    private static ImageMetadata copyMetadata(ImageMetadata source, int width, int height) {
        if (source == null) {
            return null;
        }

        return new ImageMetadata(source.gamma, source.blackIsZero, source.backgroundIndex,
                source.backgroundColor, source.transparentIndex, source.delayTime,
                source.loopCount, width, height, source.imageLeftPosition,
                source.imageTopPosition, source.disposalMethod);
    }

    private static boolean swapsDimensions(ExifOrientation orientation) {
        return orientation.degrees == 90 || orientation.degrees == 270;
    }

    public enum ExifOrientation {
        NORMAL(1, 0, false),
        FLIP_HORIZONTAL(2, 0, true),
        ROTATE_180(3, 180, false),
        FLIP_VERTICAL(4, 180, true),
        TRANSPOSE(5, 90, true),
        ROTATE_90(6, 90, false),
        TRANSVERSE(7, 270, true),
        ROTATE_270(8, 270, false);

        private final int value;
        private final int degrees;
        private final boolean isMirrored;

        ExifOrientation(int value, int degrees, boolean isMirrored) {
            this.value = value;
            this.degrees = degrees;
            this.isMirrored = isMirrored;
        }

        public static ExifOrientation fromValue(Object o) {
            try {
                return switch(o) {
                    case String s -> fromInt(Integer.parseInt(s));
                    case Number n -> fromInt(n.intValue());
                    case Entry e -> e.getValue() instanceof Number n? fromValue(n):NORMAL;
                    default -> NORMAL;
                };
            } catch(NumberFormatException ignore) {}
            return NORMAL;
        }

        public static ExifOrientation fromInt(int i) {
            for (ExifOrientation o : ExifOrientation.values()) {
                if (o.value == i) return o;
            }
            return NORMAL;
        }
    }

    private record JpegOrientationLoader(ImageLoader delegate, ExifOrientation orientation) implements ImageLoader {
        @Override
        public ImageFormatDescription getFormatDescription() {
            return delegate.getFormatDescription();
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }

        @Override
        public void addListener(ImageLoadListener listener) {
            delegate.addListener(listener);
        }

        @Override
        public void removeListener(ImageLoadListener listener) {
            delegate.removeListener(listener);
        }

        @Override
        public ImageFrame load(int imageIndex, double width, double height,
                               boolean preserveAspectRatio, boolean smooth,
                               float screenPixelScale, float imagePixelScale) throws IOException {
            double delegatedWidth = swapsDimensions(orientation)? height:width;
            double delegatedHeight = swapsDimensions(orientation)? width:height;
            ImageFrame frame = delegate.load(imageIndex, delegatedWidth, delegatedHeight,
                                             preserveAspectRatio, smooth, screenPixelScale, imagePixelScale);
            return frame == null? null:transform(frame, orientation);
        }
    }
}
