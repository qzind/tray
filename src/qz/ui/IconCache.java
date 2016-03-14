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
        DEFAULT_ICON("qz-default.png"),
        WARNING_ICON("qz-warning.png"),
        DANGER_ICON("qz-danger.png"),

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
        VERIFIED_ICON("qz-trusted.png"),
        UNVERIFIED_ICON("qz-untrusted.png"),
        FIELD_ICON("qz-field.png"),
        DELETE_ICON("qz-delete.png"),
        QUESTION_ICON("qz-question.png"),

        // Logo
        LOGO_ICON("qz-logo.png");

        final String fileName;

        /**
         * Default constructor
         *
         * @param fileName path to image
         */
        Icon(String fileName) { this.fileName = fileName; }

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

        /**
         * Returns whether or not this icon requires scaling to be used in the GUI
         *
         * @return true if this icon requires scaling
         */
        public boolean isScaled() {
            switch(this) {
                case LOGO_ICON:
                case VERIFIED_ICON:
                case UNVERIFIED_ICON:
                case QUESTION_ICON:
                    return false;
                default:
                    return true;
            }
        }

        @Override
        public String toString() { return name(); }

        /**
         * Returns the full path to the Icon resource
         *
         * @return full path to Icon resource
         */
        public String getPath() { return RESOURCES_DIR + fileName; }

        /**
         * Returns the file name of the Icon resource
         *
         * @return file name of Icon resource
         */
        public String getFileName() { return fileName; }
    }

    private final HashMap<Icon,ImageIcon> imageIcons;
    private final HashMap<Icon,BufferedImage> images;
    private final Dimension scaleSize;

    /**
     * Default constructor.
     * Builds a cache of Image and ImageIcon resources by iterating through all IconCache.Icon types
     */
    public IconCache() {
        imageIcons = new HashMap<>();
        images = new HashMap<>();
        scaleSize = null;
        buildIconCache();
    }

    /**
     * Creates an icon cache, scaling icons to the specified Dimension scaleSize.
     * Only icons which return true for IconCache.Icon.isScaled() will be scaled.  Others will be left alone.
     *
     * @param scaleSize The size to scale each appropriate image to.  See IconCache.Icon.isScaled()
     */
    public IconCache(Dimension scaleSize) {
        imageIcons = new HashMap<>();
        images = new HashMap<>();
        this.scaleSize = scaleSize;
        buildIconCache();
    }

    /**
     * Populates the internal HashMaps containing the cache
     * of ImageIcons and BufferedImages
     */
    private void buildIconCache() {
        for(Icon i : Icon.values()) {
            BufferedImage bi = getImageResource(i);
            imageIcons.put(i, new ImageIcon(bi));
            images.put(i, bi);
        }
    }

    /**
     * Adds/overwrites an ImageIcon in the cache
     *
     * @param i         an IconCache.Icon
     * @param imageIcon an ImageIcon
     * @return the ImageIcon that was added
     */
    public ImageIcon putIcon(Icon i, ImageIcon imageIcon) {
        images.put(i, toBufferedImage(imageIcon.getImage()));
        return imageIcons.put(i, imageIcon);
    }

    /**
     * Returns the ImageIcon from cache
     *
     * @param i an IconCache.Icon
     * @return the ImageIcon in the cache
     */
    public ImageIcon getIcon(Icon i) {
        return imageIcons.get(i);
    }

    /**
     * Returns the Image from cache
     *
     * @param i an IconCache.Icon
     * @return the Image in the cache
     */
    public BufferedImage getImage(Icon i) {
        return images.get(i);
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
     * Returns a BufferedImage representing the IconCache.Icon specified taking scaleSize into account
     *
     * @param i the IconCache.Icon containing an image path
     * @return a BufferedImage, scaled as needed
     */
    private BufferedImage getImageResource(Icon i) {
        BufferedImage bi = getImageResource(i.getPath());
        if (bi != null && i.isScaled() && scaleSize != null) {
            return toBufferedImage(bi.getScaledInstance(scaleSize.width, -1, Image.SCALE_SMOOTH));
        }

        return bi;
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
            InputStream is = IconCache.class.getClass().getResourceAsStream(imagePath);
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
    public void toOpaqueImage(Icon i, Color bgColor) {
        putIcon(i, new ImageIcon(toOpaqueImage(getIcon(i), bgColor)));
    }

    /**
     * Creates an opaque icon image by setting transparent pixels to the specified bgColor
     *
     * @param img The original transparency-enabled image
     * @return The image overlaid on the appropriate background color
     */
    public static BufferedImage toOpaqueImage(ImageIcon img, Color bgColor) {
        BufferedImage bi = new BufferedImage(img.getIconWidth(), img.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        bi.createGraphics().drawImage(img.getImage(), 0, 0, bgColor, null);
        return bi;
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage)img;
        }

        // Create a buffered image with transparency
        BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bi.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bi;
    }
}
