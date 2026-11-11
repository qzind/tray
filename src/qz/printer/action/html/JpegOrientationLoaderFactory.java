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
    private static final int NORMAL_ORIENTATION = 1;
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

    @Override
    public ImageLoader createImageLoader(InputStream input) throws IOException {
        byte[] jpeg = input.readAllBytes();
        int orientation = readOrientation(jpeg);
        ImageLoader loader = delegate.createImageLoader(new ByteArrayInputStream(jpeg));
        return orientation == NORMAL_ORIENTATION ? loader : new JpegOrientationLoader(loader, orientation);
    }

    private static int readOrientation(byte[] jpeg) {
        try (ImageInputStream input = new ByteArrayImageInputStream(jpeg)) {
            List<JPEGSegment> segments = JPEGSegmentUtil.readSegments(input, JPEG.APP1, "Exif");
            if (segments.isEmpty()) {
                return NORMAL_ORIENTATION;
            }

            try (InputStream segment = segments.getFirst().data()) {
                if (segment.read() != 0) {
                    return NORMAL_ORIENTATION;
                }

                try (ImageInputStream tiff = new MemoryCacheImageInputStream(segment)) {
                    Directory directory = new TIFFReader().read(tiff);
                    Entry entry = directory.getEntryById(TIFF.TAG_ORIENTATION);
                    if (entry != null && entry.getValue() instanceof Number value) {
                        int orientation = value.intValue();
                        if (orientation >= NORMAL_ORIENTATION && orientation <= 8) {
                            return orientation;
                        }
                    }
                }
            }
        }
        catch(IOException | RuntimeException ignored) {}

        return NORMAL_ORIENTATION;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    static ImageFrame transform(ImageFrame frame, int orientation) {
        if (orientation == NORMAL_ORIENTATION || !(frame.getImageData() instanceof ByteBuffer source)) {
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
                int targetX;
                int targetY;
                switch(orientation) {
                    case 2:
                        targetX = sourceWidth - 1 - x;
                        targetY = y;
                        break;
                    case 3:
                        targetX = sourceWidth - 1 - x;
                        targetY = sourceHeight - 1 - y;
                        break;
                    case 4:
                        targetX = x;
                        targetY = sourceHeight - 1 - y;
                        break;
                    case 5:
                        targetX = y;
                        targetY = x;
                        break;
                    case 6:
                        targetX = sourceHeight - 1 - y;
                        targetY = x;
                        break;
                    case 7:
                        targetX = sourceHeight - 1 - y;
                        targetY = sourceWidth - 1 - x;
                        break;
                    case 8:
                        targetX = y;
                        targetY = sourceWidth - 1 - x;
                        break;
                    default:
                        return frame;
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

    private static boolean swapsDimensions(int orientation) {
        return orientation >= 5 && orientation <= 8;
    }

    private record JpegOrientationLoader(ImageLoader delegate, int orientation) implements ImageLoader {
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
