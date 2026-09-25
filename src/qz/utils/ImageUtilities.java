package qz.utils;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.ui.ThemeUtilities;
import qz.ui.component.IconCache;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import static qz.utils.FileUtilities.*;

public class ImageUtilities {
    private static final Logger log = LogManager.getLogger(ImageUtilities.class);

    // TODO: Change this to IconCache.class after refactor
    private static final Class<?> RELATIVE_CLASS = ThemeUtilities.class;

    /**
     * Center an image with the specified padding
     */
    public static BufferedImage addPadding(BufferedImage image, float amount) {
        if(image == null) {
            return null;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int wPad = (int)(amount * w);
        int hPad = (int)(amount * h);

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
     * Returns a buffered image from the specified <code>path</code> relative
     * to ui directory
     *
     * @param path The file path of the image to load
     * @return The BufferedImage representing the data
     */
    static BufferedImage imageFromResource(Path path) {
        try(InputStream is = RELATIVE_CLASS.getResourceAsStream(resourcePath(path).toString())) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch(IOException e) {
            log.error("Cannot load {}", path, e);
        }
        return null;
    }

    public static BufferedImage imageFromResource(Path path, Integer size) {
        if(path.toString().endsWith(".svg")) {
            return imageFromSvgResource(path, size);
        }
        return imageFromResource(path);
    }

    /**
     * Returns a buffered image from the specified <code>path</code> relative to ui directory
     *
     * @param path The file path of the SVG to load
     * @param size The desired size to scale the SVG to, or the natural SVG size if <code>null</code>
     * @return The BufferedImage representing the data
     */
    static BufferedImage imageFromSvgResource(Path path, Integer size) {
        URL url = RELATIVE_CLASS.getResource(resourcePath(path).toString());
        if(url == null) {
            return null;
        }
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
        } else {
            log.warn("SVGDocument is null '{}'", path);
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

    /**
     * Inverts the color of all pixels in an image
     */
    public static BufferedImage invert(BufferedImage bi) {
        if (bi == null) {
            return null;
        }
        BufferedImage inverted = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                int pixel = bi.getRGB(x, y);
                int a = (pixel>>24)&0xFF;
                int r = 0xFF ^ ((pixel>>16)&0xFF);
                int g = 0xFF ^ ((pixel>>8)&0xFF);
                int b = 0xFF ^ ((pixel>>0)&0xFF);
                inverted.setRGB(x, y,  a << 24 | r  << 16 | g << 8 | b << 0);
            }
        }
        return inverted;
    }

    /**
     * Sets transparency of an image to the specified value
     */
    public static BufferedImage addTransparency(BufferedImage bi, float amount) {
        BufferedImage transparentImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = transparentImage.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, amount));
        g2d.drawImage(bi, 0, 0, null);
        g2d.dispose();

        return transparentImage;
    }

    /**
     * Reads the contents of the provided svg and returns the contents with transparency applied
     * to the node's root <code>style="opacity: ..."</code> attribute.
     */
    public static String addSvgTransparency(Path svgPath, float amount) throws IOException {
       return XmlUtilities.setSvgAttribute(svgPath, "svg", "style", String.format("opacity: %s", amount));
    }

    /**
     * Reads the contents of the provided svg and returns the contents with transparency applied
     * to the node's root <code>fill="#..."</code> attribute.
     */
    public static String addSvgFill(Path svgPath, Color color) throws IOException {
        return XmlUtilities.setSvgAttribute(svgPath, "svg", "style", String.format("fill: #%06x", 0xFFFFFF & color.getRGB()));
    }
}
