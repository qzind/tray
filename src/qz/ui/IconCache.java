/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by Tres Finocchiaro on 12/12/2014.
 */
public class IconCache {

    private static final Logger log = LoggerFactory.getLogger(IconCache.class);

    // Internal Jar path containing the images
    static String RESOURCES_DIR = "/qz/ui/resources/";

    /**
     * Stores Icon paths
     */
    public enum Icon {
        // Tray icons
        DEFAULT_ICON("qz-default.png", "qz-default-20.png", "qz-default-24.png", "qz-default-32.png"),
        WARNING_ICON("qz-warning.png", "qz-warning-20.png", "qz-warning-24.png", "qz-warning-32.png"),
        DANGER_ICON("qz-danger.png", "qz-danger-20.png", "qz-danger-24.png", "qz-danger-32.png"),

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
        VERIFIED_ICON("qz-trusted.png"),
        UNVERIFIED_ICON("qz-untrusted.png"),
        FIELD_ICON("qz-field.png"),
        DELETE_ICON("qz-delete.png"),
        QUESTION_ICON("qz-question.png"),

        // Banner
        BANNER_ICON("qz-banner.png");

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
            InputStream is = IconCache.class.getResourceAsStream(imagePath);
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
