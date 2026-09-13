/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.ui.component;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Sluggable;
import qz.ui.component.IconCache.Icon.Theme;
import qz.utils.ColorUtilities;
import qz.utils.SystemUtilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static qz.ui.component.IconCache.Icon.Type.*;

/**
 * Created by Tres Finocchiaro on 12/12/2014.
 */
public class IconCache {
    private static final Logger log = LogManager.getLogger(IconCache.class);

    private static final Path RESOURCES_PATH = Paths.get("../resources");

    /**
     * Stores Icon paths
     */
    public enum Icon implements Sluggable {
        // System tray
        DEFAULT_ICON(SYSTEM_TRAY, "qz-default"),
        WARNING_ICON(SYSTEM_TRAY, "qz-warning"),
        DANGER_ICON(SYSTEM_TRAY, "qz-danger"),
        MASK_ICON(SYSTEM_TRAY, "qz-mask"),

        // Task bar
        TASK_BAR_ICON(TASK_BAR, "qz-default"),

        // Menus, buttons, fields
        ABOUT_ICON(MENU,"qz-about"),
        COPY_ICON(MENU,"qz-copy"),
        DESKTOP_ICON(MENU,"qz-desktop"),
        EXIT_ICON(MENU,"qz-exit"),
        FOLDER_ICON(MENU,"qz-folder"),
        LOG_ICON(MENU,"qz-log"),
        RELOAD_ICON(MENU,"qz-reload"),
        SAVED_ICON(MENU,"qz-saved"),
        SETTINGS_ICON(MENU,"qz-settings"),

        ALLOW_ICON(MENU,"qz-allow"),
        BLOCK_ICON(MENU,"qz-block"),
        CANCEL_ICON(MENU,"qz-cancel"),

        DELETE_ICON(MENU,"qz-delete"),
        FIELD_ICON(MENU,"qz-field"),

        // Dialogs
        TRUST_VERIFIED_ICON(DIALOG, "qz-trust-verified"),
        TRUST_SPONSORED_ICON(DIALOG,"qz-trust-sponsored"),
        TRUST_ISSUE_ICON(DIALOG,"qz-trust-issue"),
        TRUST_MISSING_ICON(DIALOG,"qz-trust-missing"),
        QUESTION_ICON(DIALOG,"qz-question"),

        // Banner
        LOGO_ICON(LOGO, "qz-logo");

        public enum Type {
            SYSTEM_TRAY(20, 24, 32, 40, 48),
            TASK_BAR(20, 24, 32, 40, 48),
            DIALOG(45),
            LOGO(260),
            MENU(16);

            final int[] sizes;

            Type(int ... sizes) {
                this.sizes = sizes;
            }
        }

        public enum Format implements Sluggable {
            SVG, PNG; // order matters

            @Override
            public String slug() {
                return Sluggable.slugOf(this);
            }
        }

        public enum Theme implements Sluggable {
            LIGHT, DARK; // order matters

            public static Theme get(boolean isDark) {
                return isDark ? DARK : LIGHT;
            }

            @Override
            public String slug() {
                return Sluggable.slugOf(this);
            }
        }

        private final String file;
        private final Type type;
        private final Format format;
        private final String slug;

        Icon(Type type, String file) {
            this.file = file;
            this.type = type;
            this.format = findFormat(file);
            this.slug = Sluggable.slugOf(this).replace("_icon", ""); // TODO: Remove "_ICON" suffix
        }

        String getFileName(Theme theme, int size) {
            if(format == Format.PNG && size != type.sizes[0]) {
                // expect custom sizes to be appended to the filename
                return String.format(theme == Theme.DARK ? "%s-%s-dark.%s" : "%s-%s.%s", file, size, format.slug());
            }
            // one size fits all
            return  String.format(theme == Theme.DARK ? "%s-dark.%s" : "%s.%s", file, format.slug());
        }

        public Path getPath(Theme theme, int size) {
            return RESOURCES_PATH.resolve(getFileName(theme, size));
        }

        @Override
        public String toString() { return name(); }

        /**
         * Hashable ids created by this resource in format name-theme[-size]
         */
        String[] getIds() {
            return getIds(Theme.values());
        }

        String[] getIds(Theme ... themes) {
            List<String> ids = new ArrayList<>();

            // each resource can be light/dark themed
            Arrays.stream(themes).forEach(theme -> {
                Arrays.stream(type.sizes).mapToObj(width -> getId(theme, width)).forEach(ids::add);
            });

            return ids.toArray(new String[0]);
        }

        String getId(Theme theme, int size) {
            return String.format("%s-%s-%s", slug, theme.slug(), size);
        }

        String getId(Theme theme) {
            return getId(theme, type.sizes[0]);
        }

        String getId(boolean isDark) {
            return getId(Theme.get(isDark));
        }

        public Format getFormat() {
            return format;
        }

        public int getSize() {
            return type.sizes[0];
        }

        @Override
        public String slug() {
            return slug;
        }

        /**
         * Crawls resource path to predict the format based on a file matching
         */
        public static Format findFormat(String name) {
            for(Format format : Format.values()) {
                Path file = RESOURCES_PATH.resolve(String.format("%s.%s", name, format.slug()));
                try(InputStream is = IconCache.class.getResourceAsStream(file.toString())) {
                    if(is != null) {
                        return format;
                    }
                } catch(IOException ignore) {}
            }
            return Format.PNG;
        }
    }

    private final Map<String,ImageIcon> imageIcons;
    private final Map<String,BufferedImage> images;
    private static final Color TRANSPARENT = new Color(0,0,0,0);

    /**
     * Builds a cache of Image and ImageIcon resources by iterating through all IconCache.Icon types
     */
    public IconCache() {
        images = buildImageCache();
        imageIcons = images.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> new ImageIcon(entry.getValue())
                )
        );
    }

    Map<String, BufferedImage> buildImageCache() {
        Map<String, BufferedImage> images = new HashMap<>();
        for(Icon i : Icon.values()) {
            for(int size : i.type.sizes) {
                BufferedImage lightImage = null;
                for(Theme theme : Theme.values()) {
                    Path path = i.getPath(theme, size);
                    BufferedImage image = switch(i.format) {
                        case PNG -> getImageResource(path);
                        case SVG -> getImageResourceFromSvg(size, path);
                    };

                    // Handle undocumented macOS Sytem Tray padding
                    if (SystemUtilities.isMac() && i.type == SYSTEM_TRAY) {
                        image = padImage(image, 25);
                    }

                    // Handle dark fallback
                    if(theme == Theme.LIGHT) {
                        if(image != null) {
                            lightImage = image;
                        } else {
                            log.warn("No image found at {}", path);
                        }
                    } else {
                        if(image == null) {
                            if(i == Icon.MASK_ICON && lightImage != null) {
                                // Duplicate and invert mask icons
                                image = ColorUtilities.invert(lightImage);
                            } else {
                                image = lightImage;
                            }
                        }
                    }
                    images.put(i.getId(theme, size), image);
                }
            }
        }
        return images;

        // TODO: Decide how to handle upscaled Linux task bar icons
        /*
        // Stash scaled 2x, 3x versions if missing
        int maxScale = 3;
        for(Icon i : Icon.values()) {
            // For now, only scale icons that have more than one fileName (tray and taskbar)
            if (i.fileNames.length != 1) {
                continue;
            }
            for(int scale = 2; scale <= maxScale; scale++) {
                BufferedImage bi = images.get(i.getId());
                // Assume square icon (filename is derived from width only)
                String id = i.getId();
                boolean isDark = getBaseName(id).endsWith(DARK_PNG_SUFFIX);
                int loc = id.lastIndexOf(".");
                if(loc == -1) {
                    continue;
                }
                String name = id.substring(0, loc);
                String ext = id.substring(loc + 1);
                String newSize = String.format("%s-%s.%s", name,  bi.getWidth() * scale, ext);
                if (!images.containsKey(newSize)) {
                    i.addId(newSize, isDark);
                    BufferedImage newBi = clone(bi, scale);
                    imageIcons.put(newSize, new ImageIcon(newBi));
                    images.put(newSize, newBi);
                }
            }
        }*/
    }

    /**
     * Returns the ImageIcon from cache
     *
     * @param i an IconCache.Icon
     * @param isDark Whether to return the dark themed version of this resource
     * @return the ImageIcon in the cache
     */
    public ImageIcon getIcon(Icon i, boolean isDark) {
        return imageIcons.get(i.getId(isDark));
    }

    public ImageIcon getIcon(Icon i) {
        return imageIcons.get(i.getId(false));
    }

    private ImageIcon getIcon(String id) {
        return imageIcons.get(id);
    }

    public ImageIcon getIcon(Icon i, Dimension size, boolean isDark) {
        return imageIcons.get(i.getId(Theme.get(isDark), (int)size.getWidth()));
    }

    /**
     * Returns the Image from cache
     *
     * @param i an IconCache.Icon
     * @param isDark Whether to return the dark themed version of this resource
     * @return the Image in the cache
     */
    public BufferedImage getImage(Icon i, boolean isDark) {
        return images.get(i.getId(isDark));
    }

    public BufferedImage getImage(Icon i) {
        return images.get(i.getId(false));
    }

    public List<BufferedImage> getImages(Icon i, boolean isDark) {
        ArrayList<BufferedImage> icons = new ArrayList<>();
        for(String id : i.getIds(Theme.get(isDark))) {
            icons.add(images.get(id));
        }
        return icons;
    }

    public List<BufferedImage> getImages(Icon i) {
        return getImages(i, false);
    }

    public BufferedImage getImage(Icon i, Dimension size, boolean isDark) {
        return images.get(i.getId(Theme.get(isDark), (int)size.getWidth()));
    }

    public BufferedImage getImage(Icon i, Dimension size) {
        return images.get(i.getId(Theme.DARK, (int)size.getWidth()));
    }

    /**
     * Returns all IconCache.Icon's possible values
     *
     * @return the complete list of IconCache.Icon values
     */
    public static Icon[] getTypes() {
        return Icon.values();
    }

    /**
     * Returns a buffered image from the specified imagePath. The image must
     * reside in the RESOURCES_PATH declared above. Images are assumed to be
     * bundled into the jar resource.
     *
     * @param path The file name of the image to load
     * @return The BufferedImage representing the data
     */
    public static BufferedImage getImageResource(Path path) {
        try(InputStream is = IconCache.class.getResourceAsStream(path.toString())) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch(IOException e) {
            log.error("Cannot load {}", path, e);
        }
        return null;
    }

    public static BufferedImage getImageResourceFromSvg(Integer size, Path path) {
        URL url = IconCache.class.getResource(path.toString());
        if (url != null) {
            SVGLoader loader = new SVGLoader();
            SVGDocument svgDocument = loader.load(url);
            if(svgDocument != null) {
                FloatSize svgSize = svgDocument.size();
                float w = svgSize.width;
                float h = svgSize.height;

                // scale proportionally
                if(size != null) {
                    w = svgSize.width * (size / h);
                    h = size;
                }

                BufferedImage image = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                double scaleX = w / svgSize.width;
                double scaleY = h / svgSize.height;
                g.scale(scaleX, scaleY);
                svgDocument.render(null, g);
                g.dispose();
                return image;
            }
        }
        return null;
    }

    /**
     * Overwrites the specified IconCache.Icon's underlying ImageIcon and BufferedImage with an opaque version
     *
     * @param i       the IconCache.Icon
     * @param bgColor the java Color used for the transparent pixels
     */
    public void setBgColor(Icon i, Color bgColor) {
        for (String id : i.getIds()) {
            ImageIcon imageIcon = new ImageIcon(toOpaqueImage(getIcon(id), bgColor));
            images.put(id, toBufferedImage(imageIcon.getImage(), TRANSPARENT));
            imageIcons.put(id, imageIcon);
        }
    }

    /**
     * Shrink and center an image the specified percentage
     */
    public BufferedImage padImage(BufferedImage image, float percent) {
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
     * Creates an opaque icon image by setting transparent pixels to the specified bgColor
     *
     * @param icon The original transparency-enabled image
     * @return The image overlaid on the appropriate background color
     */
    public static BufferedImage toOpaqueImage(ImageIcon icon, Color bgColor) {
        return toBufferedImage(icon.getImage(), bgColor);
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(Image img, Color bgColor) {
        if (img instanceof BufferedImage && bgColor == TRANSPARENT) {
            return (BufferedImage)img;
        }

        // Create a buffered image with transparency
        BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bi.createGraphics();
        bGr.drawImage(img, 0, 0, bgColor, null);
        bGr.dispose();

        // Return the buffered image
        return bi;
    }

    /**
     * For rebranded installs, calculates the "BRAND_COLOR" by inspecting the center of the specified <code>IconCache.Icon</code>.
     * For "QZ" brained installs, returns Constants.BRAND_COLOR
     * @return String value representing the brand color.
     */
    public static String getHtmlColorFromIcon(IconCache.Icon icon, String fallback) {
        BufferedImage bi = new IconCache().getImage(icon); // FIXME:  Create IconCache instance instead
        if(bi == null) {
            return fallback;
        }
        int pixel = bi.getRGB(bi.getWidth() / 2, bi.getHeight() / 2);
        return String.format("#%06X", (0xFFFFFF & pixel));
    }
}
