package qz.utils;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ui.component.IconCache;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

public class ImageUtilities {
    private static final Logger log = LogManager.getLogger(ImageUtilities.class);

    /**
     * Shrink and center an image the specified percentage
     */
    public static BufferedImage padImage(BufferedImage image, float percent) {
        if(image == null) {
            return null;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int wPad = (int)((percent/100.0) * w);
        int hPad = (int)((percent/100.0) * h);

        BufferedImage padded = new BufferedImage(w + wPad, h + hPad, BufferedImage.TYPE_INT_ARGB);
        Graphics g = padded.getGraphics();

        g.drawImage(image, wPad/2, hPad/2, null);
        g.dispose();

        return padded;
    }

    /**
     * For rebranded installs, calculates the "BRAND_COLOR" by inspecting the center of the specified <code>IconCache.Icon</code>.
     * For "QZ" brained installs, returns Constants.BRAND_COLOR
     * @return String value representing the brand color.
     */
    public static String getHtmlColorFromImage(BufferedImage image, String fallback) {
        if(image == null) {
            return fallback;
        }
        int pixel = image.getRGB(image.getWidth() / 2, image.getHeight() / 2);
        return String.format("#%06X", (0xFFFFFF & pixel));
    }

    /**
     * Returns a buffered image from the specified <code>path</code> relative to the
     * class specified.
     *
     * @param path The file path of the image to load
     * @return The BufferedImage representing the data
     */
    public static BufferedImage imageFromResource(Path path, Object relativeClass) {
        try(InputStream is = relativeClass.getClass().getResourceAsStream(path.toString())) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch(IOException e) {
            log.error("Cannot load {}", path, e);
        }
        return null;
    }

    /**
     * Returns a buffered image from the specified <code>path</code> relative to the
     * class specified.
     *
     * @param path The file path of the SVG to load
     * @param size The desired size to scale the SVG to, or the natural SVG size if <code>null</code>
     * @param relativeClass Class used for looking up relative resources
     * @return The BufferedImage representing the data
     */
    public static BufferedImage imageFromSvgResource(Path path, Integer size, Object relativeClass) {
        URL url = relativeClass.getClass().getResource(path.toString());
        if (url != null) {
            SVGLoader loader = new SVGLoader();
            SVGDocument svgDocument = loader.load(url);
            if(svgDocument != null) {
                FloatSize svgSize = svgDocument.size();
                double w = svgSize.getWidth();
                double h = svgSize.getHeight();

                // scale proportionally
                if(size != null) {
                    w = svgSize.getWidth() * (size / h);
                    h = size;
                }

                BufferedImage image = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                g.scale(w / svgSize.getWidth(), h / svgSize.getHeight());
                svgDocument.render(null, g);
                g.dispose();
                return image;
            }
        }
        return null;
    }

    /**
     * Converts a BufferedImage into a browser-compatible base64 image
     * Useful for serializing
     */
    static String imageToBase64(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, format, bos);
            byte[] imageBytes = bos.toByteArray();
            bos.close();
            // Java's encoder won't add newlines
            return java.util.Base64.getEncoder().encodeToString(imageBytes);
        } catch(IOException e) {
            log.warn("Could not convert BufferedImage to base64", e);
            return "";
        }

    }

    public static String imageToBase64(IconCache.Icon icon, String format) {
        return imageToBase64(IconCache.getInstance().getImage(icon, false), format);
    }
}
