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

import qz.common.Sluggable;
import qz.ui.component.iconcache.*;
import qz.utils.ImageUtilities;
import qz.utils.SystemUtilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static qz.ui.component.iconcache.Type.*;
import static qz.ui.component.iconcache.Saturation.*;

public class IconCache {
    private static IconCache instance;
    private final Path resourcesPath;

    /**
     * Enum for building and tracking icon keys for PNG (pre-rasterized) or SVG (runtime rasterized) images
     */
    public enum Icon implements Sluggable {
        // System tray (preferred)
        TRAY_READY(SYSTEM_TRAY, MASK,"tray-ready", "qz-mask"),
        TRAY_LOADING(SYSTEM_TRAY, MASK,"tray-loading"),

        // System tray color (legacy)
        TRAY_READY_COLOR(SYSTEM_TRAY, "tray-ready-color", "qz-default"),
        TRAY_LOADING_COLOR(SYSTEM_TRAY, "tray-loading-color", "qz-danger"),

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
        private final Saturation saturation;
        private final String slug;
        private final String[] names;

        Icon(Type type, Saturation saturation, String ... names) {
            this.type = type;
            this.saturation = saturation;
            this.names = names;
            this.slug = Sluggable.slugOf(this).replace("_icon", ""); // TODO: Remove "_ICON" suffix
        }

        Icon(Type type, String ... names) {
            this(type, Saturation.COLOR, names);
        }

        @Override
        public String toString() { return name(); }

        @Override
        public String slug() {
            return slug;
        }

        public Icon getIcon(Saturation sat) {
            return switch(sat) {
                case MASK -> this;
                default -> switch(this) {
                    case TRAY_READY -> TRAY_READY_COLOR;
                    case TRAY_LOADING -> TRAY_LOADING_COLOR;
                    default -> this;
                };
            };
        }

        public boolean isMaskIcon() {
            return saturation == Saturation.MASK;
        }

        public Type getType() {
            return type;
        }

        public String[] getNames() {
            return names;
        }
    }

    public final HashMap<String, Cache> cacheMap;

    /**
     * Builds a cache of Image and ImageIcon resources by iterating through all IconCache.Icon types
     */
    IconCache(Path resourcesPath) {
        this.resourcesPath = resourcesPath;
        this.cacheMap = initMap();
        this
                .addMissingLoadingIcons()
                .addMissingThemeIcons()
                .setMaskColor()
                .padMacIcons();
    }

    private IconCache setMaskColor() {
        // Ensure default mask/template icons are 100% black/white respectively
        cacheMap.entrySet().stream()
                .filter(e -> e.getValue().getIcon().isMaskIcon())
                .forEach(e -> e.getValue().setMaskColor());
        return this;
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

    IconCache addMissingLoadingIcons() {
        // mask icons appear "loading" at 50% transparency
        cacheMap.entrySet().stream()
                .filter(e -> e.getValue().missing())
                .filter(e -> e.getValue().getTheme() == Theme.LIGHT)
                .filter(e -> e.getValue().getIcon() == Icon.TRAY_LOADING)
                .forEach(e -> e.getValue().fadeImage(
                        cacheMap.get(e.getValue().swapedKey(Icon.TRAY_READY)), 0.5f)
                );
        return this;
    }

    IconCache addMissingThemeIcons() {
        cacheMap.entrySet().stream()
                .filter(e -> e.getValue().missing())
                .filter(e -> e.getValue().getTheme() == Theme.DARK)
                .forEach(e -> e.getValue().setImages(
                        cacheMap.get(e.getValue().themedKey(Theme.LIGHT))
                ));
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    IconCache padMacIcons() {
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
     * @param isDark Whether to return the dark themed version of this resource
     * @return the ImageIcon in the cache
     */
    public ImageIcon getIcon(Icon i, boolean isDark) {
        return getCache(i, Theme.parse(isDark)).getImageIcon();
    }

    public ImageIcon getIcon(Icon i) {
        return getIcon(i, false);
    }

    ImageIcon getIcon(Icon i, Theme theme, int size) {
        return getCache(i, theme, size).getImageIcon();
    }

    /**
     * Returns the Image from cache
     *
     * @param i an IconCache.Icon
     * @param isDark Whether to return the dark themed version of this resource
     * @return the Image in the cache
     */
    public BufferedImage getImage(Icon i, boolean isDark) {
        return getCache(i, Theme.parse(isDark)).getBufferedImage();
    }

    public BufferedImage getImage(Icon i) {
        return getImage(i, false);
    }

    public BufferedImage getImage(Icon i, Dimension size, boolean isDark) {
        return getCache(i, Theme.parse(isDark), (int)size.getHeight()).getBufferedImage();
    }

    BufferedImage getImage(Icon i, Theme theme, int size) {
        return getCache(i, theme, size).getBufferedImage();
    }

    List<BufferedImage> getImages(Icon i, Theme theme) {
        return Arrays.stream(i.getType().getSizes())
                .mapToObj(size -> getCache(i, theme, size).getBufferedImage())
                .collect(Collectors.toList());
    }

    public List<BufferedImage> getImages(Icon i) {
        return getImages(i, false);
    }

    public List<BufferedImage> getImages(Icon i, boolean isDark) {
        return getImages(i, Theme.parse(isDark));
    }

    public Path extract(Icon i, Theme theme) throws IOException {
        return getCache(i, theme, i.getType().getSizes()[0]).extract();
    }

    public Cache getCache(Icon i, Theme theme, int size) {
        return cacheMap.get(Cache.getKey(i, theme, size));
    }

    public Cache getCache(Icon i, Theme theme) {
        return getCache(i, theme, i.getType().getSizes()[0]);
    }

    public synchronized static IconCache getInstance() {
        if(instance == null) {
            instance = new IconCache();
        }
        return instance;
    }
}
