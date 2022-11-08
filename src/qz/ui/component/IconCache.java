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

import com.github.zafarkhaja.semver.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.utils.ColorUtilities;
import qz.utils.SystemUtilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * Created by Tres Finocchiaro on 12/12/2014.
 */
public class IconCache {

    private static final Logger log = LogManager.getLogger(IconCache.class);

    // Internal Jar path containing the images
    static String RESOURCES_DIR = "/qz/ui/resources/";

    /**
     * Stores Icon paths
     */
    public enum Icon {
        // Tray icons
        DEFAULT_ICON("qz-default.png", "qz-default-20.png", "qz-default-24.png", "qz-default-32.png", "qz-default-40.png", "qz-default-48.png"),
        WARNING_ICON("qz-warning.png", "qz-warning-20.png", "qz-warning-24.png", "qz-warning-32.png", "qz-warning-40.png", "qz-warning-48.png"),
        DANGER_ICON("qz-danger.png", "qz-danger-20.png", "qz-danger-24.png", "qz-danger-32.png", "qz-danger-40.png", "qz-danger-48.png"),
        MASK_ICON("qz-mask.png", "qz-mask-20.png", "qz-mask-24.png", "qz-mask-32.png", "qz-mask-40.png", "qz-mask-48.png"),

        // Task bar icons - Appending "#" allows hashing under unique id
        TASK_BAR_ICON("qz-default.png#", "qz-default-20.png#", "qz-default-24.png#", "qz-default-32.png#", "qz-default-40.png#", "qz-default-48.png#"),

        // Menu Item icons
        EXIT_ICON("qz-exit.png"),
        RELOAD_ICON("qz-reload.png"),
        ABOUT_ICON("qz-about.png"),
        DESKTOP_ICON("qz-desktop.png"),
        SAVED_ICON("qz-saved.png"),
        LOG_ICON("qz-log.png"),
        FOLDER_ICON("qz-folder.png"),
        SETTINGS_ICON("qz-settings.png"),

        // Dialog icons
        ALLOW_ICON("qz-allow.png"),
        BLOCK_ICON("qz-block.png"),
        CANCEL_ICON("qz-cancel.png"),
        TRUST_VERIFIED_ICON("qz-trust-verified.png"),
        TRUST_ISSUE_ICON("qz-trust-issue.png"),
        TRUST_MISSING_ICON("qz-trust-missing.png"),
        FIELD_ICON("qz-field.png"),
        DELETE_ICON("qz-delete.png"),
        QUESTION_ICON("qz-question.png"),

        // Banner
        LOGO_ICON("qz-logo.png"),
        BANNER_ICON("qz-banner.png");

        private boolean padded = false;
        final String[] fileNames;

        /**
         * Default constructor
         *
         * @param fileNames path(s) to image
         */
        Icon(String ... fileNames) { this.fileNames = fileNames; }

        /**
         * Returns whether or not this icon is used for the SystemTray
         *
         * @return true if this icon is used for the SystemTray
         */
        public boolean isTrayIcon() {
            switch(this) {
                case DEFAULT_ICON:
                case WARNING_ICON:
                case DANGER_ICON:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public String toString() { return name(); }

        /**
         * Returns the full path to the Icon resource
         *
         * @return full path to Icon resource
         */
        public String getPath() { return RESOURCES_DIR + getId(); }

        /**
         * Returns the full path to the Icon resource with the specified width suffix.
         * Width is determined solely by filename suffix.  e.g. foo-32.png
         *
         * @param size size of desired image
         * @return icon file name
         */
        public String getId(Dimension size) {
            if (size != null) {
                for(String fileName : fileNames) {
                    if (fileName.endsWith("-" + size.width + ".png")) {
                        return fileName;
                    }
                }
            }
            return getId();
        }

        public String getId() {
            return fileNames[0];
        }

        public String[] getIds() { return fileNames; }
    }

    private final HashMap<String,ImageIcon> imageIcons;
    private final HashMap<String,BufferedImage> images;
    private static final Color TRANSPARENT = new Color(0,0,0,0);

    /**
     * Default constructor.
     * Builds a cache of Image and ImageIcon resources by iterating through all IconCache.Icon types
     */
    public IconCache() {
        imageIcons = new HashMap<>();
        images = new HashMap<>();
        buildIconCache();
    }

    /**
     * Populates the internal HashMaps containing the cache
     * of ImageIcons and BufferedImages
     */
    private void buildIconCache() {
        for(Icon i : Icon.values()) {
            for (String id : i.getIds()) {
                BufferedImage bi = getImageResource(RESOURCES_DIR + id);
                imageIcons.put(id, new ImageIcon(bi));
                images.put(id, bi);
            }
        }
    }

    /**
     * Returns the ImageIcon from cache
     *
     * @param i an IconCache.Icon
     * @return the ImageIcon in the cache
     */
    public ImageIcon getIcon(Icon i) {
        return imageIcons.get(i.getId());
    }

    public ImageIcon getIcon(String id) {
        return imageIcons.get(id);
    }

    public ImageIcon getIcon(Icon i, Dimension size) {
        return imageIcons.get(i.getId(size));
    }

    /**
     * Returns the Image from cache
     *
     * @param i an IconCache.Icon
     * @return the Image in the cache
     */
    public BufferedImage getImage(Icon i) {
        return images.get(i.getId());
    }

    public List<BufferedImage> getImages(Icon i) {
        ArrayList<BufferedImage> icons = new ArrayList<>();
        for(String id : i.getIds()) {
            icons.add(images.get(id));
        }
        return icons;
    }

    public BufferedImage getImage(Icon i, Dimension size) {
        return images.get(i.getId(size));
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
     * reside in the RESOURCES_DIR declared above. Images are assumed to be
     * bundled into the jar resource.
     *
     * @param imagePath The file name of the image to load
     * @return The BufferedImage representing the data
     */
    private static BufferedImage getImageResource(String imagePath) {
        try {
            InputStream is = IconCache.class.getResourceAsStream(imagePath.replaceAll("#", ""));
            if (is != null) {
                return ImageIO.read(is);
            } else {
                log.warn("Cannot find {}", imagePath);
            }
        }
        catch(IOException e) {
            log.error("Cannot find {}", imagePath, e);
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
     * Replaces the cached tray icons with corrected versions if necessary
     * e.g.
     *  - Ubuntu transparency
     *  - macOS masked icons
     *  - macOS 10.14+ dark mode support
     */
    public void fixTrayIcons(boolean darkTaskbar) {
        // Handle mask-style tray icons
        if (SystemUtilities.prefersMaskTrayIcon()) {
            // Clone the mask icon
            for (String id : Icon.MASK_ICON.getIds()) {
                BufferedImage clone = clone(images.get(id));
                // Even on lite mode desktops, white tray icons were the norm until Windows 10 update 1903, (1903 is build 18362.X)
                if (SystemUtilities.isWindows() && SystemUtilities.getOsVersion().lessThan(Version.valueOf("10.0.18362"))) {
                    darkTaskbar = true;
                }
                if (darkTaskbar) {
                    clone = ColorUtilities.invert(clone);
                }
                images.put(id.replaceAll("mask", "default"), clone);
                imageIcons.put(id.replaceAll("mask", "default"), new ImageIcon(clone));
            }
        }

        // Handle undocumented macOS tray icon padding
        for(IconCache.Icon i : IconCache.getTypes()) {
            // See also JXTrayIcon.getSize()
            if (i.isTrayIcon() && SystemUtilities.isMac()) {
                // Prevent padding from happening twice
                if (!i.padded) {
                    padIcon(i, 25);
                }
            }
        }
    }

    public static BufferedImage clone(BufferedImage src) {
        Image tmp = src.getScaledInstance(src.getWidth(), src.getHeight(), src.getType());
        BufferedImage dest = new BufferedImage(tmp.getWidth(null), tmp.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics g = dest.createGraphics();
        g.drawImage(tmp, 0, 0, null);
        g.dispose();
        return dest;
    }

    public void padIcon(Icon icon, int percent) {
        for (String id : icon.getIds()) {
            // Calculate padding percentage
            int w = images.get(id).getWidth();
            int h = images.get(id).getHeight();
            int wPad = (int)((percent/100.0) * w);
            int hPad = (int)((percent/100.0) * h);

            BufferedImage padded = new BufferedImage(w + wPad, h + hPad, BufferedImage.TYPE_INT_ARGB);
            Graphics g = padded.getGraphics();

            // Pad all sides (by half)
            g.drawImage(images.get(id), wPad/2, hPad/2, null);
            g.dispose();

            images.put(id, padded);
            imageIcons.put(id, new ImageIcon(padded));
            icon.padded = true;
        }
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
}
