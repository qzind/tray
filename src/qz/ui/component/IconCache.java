/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2014 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package qz.ui.component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Sluggable;
import qz.ui.ThemeUtilities;
import qz.ui.component.iconcache.*;
import qz.utils.FileUtilities;
import qz.utils.ImageUtilities;
import qz.utils.SystemUtilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static qz.ui.component.iconcache.Type.*;

public class IconCache {
    private static final Logger log = LogManager.getLogger(IconCache.class);
    private static IconCache instance;
    private final Path resourcesPath;

    final ConcurrentHashMap<String, Path> extractedImages;

    /**
     * Enum for building and tracking icon keys for PNG (pre-rasterized) or SVG (runtime rasterized) images
     */
    public enum Icon implements Sluggable {
        // System tray (color)
        DEFAULT_ICON(SYSTEM_TRAY, "tray-ready-color", "qz-default"),
        DANGER_ICON(SYSTEM_TRAY, "tray-loading-color", "qz-danger"),

        // System tray (mask)
        DEFAULT_MASK_ICON(SYSTEM_TRAY, DEFAULT_ICON, "tray-ready", "qz-mask"),
        DANGER_MASK_ICON(SYSTEM_TRAY, DANGER_ICON, "tray-loading"),

        // Task bar
        TASK_BAR_ICON(TASK_BAR, "tray-ready", "qz-default"),

        // Menus, buttons, fields
        ABOUT_ICON(MENU,"about"),
        COPY_ICON(MENU,"copy"),
        DESKTOP_ICON(MENU,"desktop"),
        EXIT_ICON(MENU,"exit"),
        FOLDER_ICON(MENU,"folder"),
        LOG_ICON(MENU,"log"),
        RELOAD_ICON(MENU,"reload"),
        SAVED_ICON(MENU,"saved"),
        SETTINGS_ICON(MENU,"settings"),

        ALLOW_ICON(MENU,"allow"),
        BLOCK_ICON(MENU,"block"),
        CANCEL_ICON(MENU,"cancel"),

        DELETE_ICON(MENU,"delete"),
        FIELD_ICON(MENU,"field"),

        // Dialogs
        TRUST_VERIFIED_ICON(DIALOG, "trust-verified"),
        TRUST_SPONSORED_ICON(DIALOG,"trust-sponsored"),
        TRUST_ISSUE_ICON(DIALOG,"trust-issue"),
        TRUST_MISSING_ICON(DIALOG,"trust-missing"),
        QUESTION_ICON(DIALOG,"question"),

        // Banner logo
        LOGO_ICON(LOGO, "logo", "qz-logo");

        private final Type type;
        private final String slug;
        private final String[] names;
        private final Icon maskFor;

        Icon(Type type, Icon maskFor, String ... names) {
            this.type = type;
            this.maskFor = maskFor;
            this.names = names;
            this.slug = Sluggable.slugOf(this).replace("_icon", ""); // TODO: Remove "_ICON" suffix
        }

        Icon(Type type, String ... names) {
            this(type, null, names);
        }

        @Override
        public String toString() { return name(); }

        @Override
        public String slug() {
            return slug;
        }

        public Saturation getSaturation() {
            return maskFor == null ? Saturation.COLOR : Saturation.MASK;
        }

        public Icon getIcon(Saturation sat) {
            return sat == Saturation.MASK ? getMaskIcon() : this;
        }

        /**
         * Fetching a masked/templated/symbolic of the specified icon
         */
        public Icon getMaskIcon() {
            return maskFor != null ? maskFor : this;
        }

        public Type getType() {
            return type;
        }

        public String[] getNames() {
            return names;
        }
    }

    private final HashMap<String, Cache> cacheMap;

    /**
     * Builds a cache of Image and ImageIcon resources by iterating through all IconCache.Icon types
     */
    IconCache(Path resourcesPath) {
        this.resourcesPath = resourcesPath;
        this.cacheMap = initMap();
        this
                .fixLoadingIcons()
                .fixMissingDarkIcons()
                .fixInvertedIcons()
                .fixMacTrayIcons();
        this.extractedImages = new ConcurrentHashMap<>();
    }

    public IconCache() {
        this(Paths.get("resources"));
    }

    HashMap<String, Cache> initMap() {
        HashMap<String, Cache> images = new LinkedHashMap<>();
        for(Icon i : Icon.values()) {
            for(int size : i.getType().getSizes()) {
                for(Theme theme : Theme.values()) {
                    Cache cache = new Cache(i, theme, size);
                    String key = cache.getKey();
                    cache.load(resourcesPath);
                    images.put(key, cache);
                }
            }
        }
        return images;
    }

    IconCache fixLoadingIcons() {
        // mask icons appear "loading" at 50% transparency
        cacheMap.entrySet().stream()
                .filter(e -> e.getValue().missing())
                .filter(e -> e.getValue().getTheme() == Theme.LIGHT)
                .filter(e -> e.getValue().getIcon() == Icon.DANGER_MASK_ICON)
                .forEach(e -> e.getValue().fadeImage(
                        cacheMap.get(e.getValue().swapedKey(Icon.DEFAULT_MASK_ICON)), 0.5f)
                );
        return this;
    }

    IconCache fixMissingDarkIcons() {
        cacheMap.entrySet().stream()
                .filter(e -> e.getValue().missing())
                .filter(e -> e.getValue().getTheme() == Theme.DARK)
                .forEach(e -> e.getValue().setImages(
                        cacheMap.get(e.getValue().themedKey(Theme.LIGHT))
                ));
        return this;
    }

    IconCache fixInvertedIcons() {
        // Handle dark icons on OSs that don't support template icons
        cacheMap.entrySet().stream()
                .filter(e -> ThemeUtilities.needsInversion(e.getValue().getIcon()))
                .forEach(e -> e.getValue().invertImage());
        return this;
    }

    IconCache fixMacTrayIcons() {
        if(SystemUtilities.isMac()) {
            // Handle undocumented 25% padding for macOS system tray
            cacheMap.entrySet().stream()
                    .filter(e -> e.getValue().getIcon().getType() == SYSTEM_TRAY)
                    .forEach(e -> e.getValue().padImage(0.25f));
        }
        return this;
    }

    /**
     * Returns the ImageIcon from cache
     *
     * @param i an IconCache.Icon
     * @param theme Theme.LIGHT or Theme.DARK
     * @return the ImageIcon in the cache
     */
    ImageIcon getIcon(Icon i, Theme theme, int size) {
        return getCache(i, theme, size).getImageIcon();
    }

    Cache getCache(Icon i, Theme theme, int size) {
        return cacheMap.get(Cache.getKey(i, theme, size));
    }

    Cache getCache(Icon i, Theme theme) {
        return getCache(i, theme, i.getType().getSizes()[0]);
    }

    public ImageIcon getIcon(Icon i, boolean isDark) {
        return getCache(i, Theme.get(isDark)).getImageIcon();
    }

    public ImageIcon getIcon(Icon i) {
        return getIcon(i, false);
    }

    BufferedImage getImage(Icon i, Theme theme, int size) {
        return getCache(i, theme, size).getBufferedImage();
    }

    public BufferedImage getImage(Icon i, Dimension d, boolean isDark) {
        return getCache(i, Theme.get(isDark), (int)d.getHeight()).getBufferedImage();
    }

    public BufferedImage getImage(Icon i, boolean isDark) {
       return getCache(i, Theme.get(isDark)).getBufferedImage();
    }

    public BufferedImage getImage(Icon i) {
        return getImage(i, false);
    }

    List<BufferedImage> getImages(Icon i, Theme theme) {
        return Arrays.stream(i.getType().getSizes())
                .mapToObj(size -> getCache(i, theme, size).getBufferedImage())
                .collect(Collectors.toList());
    }

    public List<BufferedImage> getImages(Icon i, boolean isDark) {
        return getImages(i, Theme.get(isDark));
    }

    public List<BufferedImage> getImages(Icon i) {
        return getImages(i, false);
    }

    Path extractImage(Format format, Icon i, boolean isDark, int size) throws IOException {
        String key = Cache.getKey(i, Theme.get(isDark), size);
        String extractKey = key  + "-" + format.slug();

        if(extractedImages.containsKey(extractKey)) {
            return extractedImages.get(extractKey);
        }

        Path extractLocation = Files.createTempFile(key, format.extension());
        extractLocation.toFile().deleteOnExit();

        Cache cache = cacheMap.get(key);
        Theme theme = cache.getTheme();
        if(format == Format.UNKNOWN) {
            throw new UnsupportedOperationException("No way to extract " + format + " to file");
        } else if(format == Format.SVG) {
            Path imagePath = cache.getBasePath();
            if(imagePath == null) {
                switch(i) {
                    case DANGER_MASK_ICON:
                        if(theme == Theme.DARK) {
                            throw new UnsupportedOperationException("No way to convert " + format + " from light to dark");
                        }
                        // We don't require tray-loading; try making one on-the-fly instead
                        Path opaqueSvg = cacheMap.get(cache.swapedKey(Icon.DEFAULT_MASK_ICON)).getBasePath();
                        String xmlContent = ImageUtilities.addTransparency(opaqueSvg, 0.5f);
                        Files.writeString(extractLocation, xmlContent);
                        break;
                    case DEFAULT_ICON:
                    default:
                        if(theme == Theme.DARK) {
                            // Recurse: Most dark icons can just fall back to their light equivalents
                            extractLocation = extractImage(format, i, false, size);
                            extractedImages.put(extractKey, extractLocation);
                            return extractLocation;
                        }
                        // We should never get here
                        throw new UnsupportedEncodingException(String.format("No file found for %s %s %s %s", format, i, Theme.get(isDark), size));
                }
            } else {
                FileUtilities.configureAssetToFile(ThemeUtilities.class, imagePath.toString(), new HashMap<>(), extractLocation.toFile());
            }
            extractedImages.put(extractKey, extractLocation);
            return extractLocation;
        } else {
            if (ImageIO.write(getImage(i, theme, size), format.slug(), extractLocation.toFile())) {
                extractedImages.put(extractKey, extractLocation);
                return extractLocation;
            }
            throw new IOException("Unable to write png file: '" + extractLocation + "'");
        }
    }

    public Path extractPng(Icon i, boolean isDark, int size) throws IOException {
        return extractImage(Format.PNG, i, isDark, size);
    }

    public Path extractSvg(Icon i, boolean isDark) throws IOException {
        return extractImage(Format.SVG, i,  isDark, 0);
    }

    public synchronized static IconCache getInstance() {
        if(instance == null) {
            instance = new IconCache();
        }
        return instance;
    }
}
