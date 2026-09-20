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

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qz.common.Sluggable;
import qz.ui.ThemeUtilities;
import qz.ui.component.IconCache.Icon.Theme;
import qz.utils.FileUtilities;
import qz.utils.ImageUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static qz.ui.component.IconCache.Icon.Type.*;

public class IconCache {
    private static final Logger log = LogManager.getLogger(IconCache.class);
    private static IconCache instance;
    private final Path resourcesPath;

    final ConcurrentHashMap<String, Path> extractedSvgs;

    /**
     * Enum for building and tracking icon keys for PNG (pre-rasterized) or SVG (runtime rasterized) images
     */
    public enum Icon implements Sluggable {
        // System tray
        DEFAULT_MASK_ICON(SYSTEM_TRAY, "tray-ready", "qz-mask"),
        DANGER_MASK_ICON(SYSTEM_TRAY, "tray-loading", "qz-danger"),

        // System tray (legacy color fallback)
        DEFAULT_ICON(SYSTEM_TRAY, "tray-ready-color", "qz-default"),
        DANGER_ICON(SYSTEM_TRAY, "tray-loading-color", "qz-danger"),

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

        enum Type {
            SYSTEM_TRAY(16, 20, 24, 32, 40, 48),
            TASK_BAR(16, 20, 24, 32, 40, 48),
            DIALOG(45),
            LOGO(260),
            MENU(16);

            final int[] sizes;

            Type(int ... sizes) {
                this.sizes = sizes;
            }
        }

        enum Format implements Sluggable {
            PNG, SVG; // order sets precedent

            static Format parse(Path path) {
                for(Format format : Format.values()) {
                    if(path.toString().endsWith(String.format(format.extension()))) {
                        return format;
                    }
                }
                return PNG;
            }

            @Override
            public String slug() {
                return Sluggable.slugOf(this);
            }

            String extension() {
                return String.format(".%s", slug());
            }
        }

        enum Theme implements Sluggable {
            LIGHT, DARK; // order required for fallback

            public static Theme get(boolean isDark) {
                return isDark ? DARK : LIGHT;
            }

            @Override
            public String slug() {
                return Sluggable.slugOf(this);
            }
        }

        private final Type type;
        private final String slug;
        private final String[] names;

        Icon(Type type, String ... names) {
            this.type = type;
            this.names = names;
            this.slug = Sluggable.slugOf(this).replace("_icon", ""); // TODO: Remove "_ICON" suffix
        }

        @Override
        public String toString() { return name(); }

        /**
         * Hashable ids created by this resource in format name-theme[-size]
         */
        String[] getIds(Theme ... themes) {
            List<String> ids = new ArrayList<>();

            // each resource can be light/dark themed
            Arrays.stream(themes).forEach(theme -> {
                Arrays.stream(type.sizes).mapToObj(width -> getId(theme, width)).forEach(ids::add);
            });

            return ids.toArray(new String[0]);
        }

        String getId(Theme theme) {
            return getId(theme, 0);
        }

        String getId(Theme theme, int size) {
            if(size < 1) {
                return String.format("%s-%s", slug, theme.slug());
            }
            return String.format("%s-%s-%s", slug, theme.slug(), size);
        }

        Type getType() {
            return type;
        }

        int[] getSizes() {
            return type.sizes;
        }

        public int getSize() {
            return getSizes()[0];
        }

        @Override
        public String slug() {
            return slug;
        }

        /**
         * Fetching a masked/templated/symbolic of the specified icon
         */
        public Icon getIcon(boolean wantsColor) {
            return switch(this) {
                case DEFAULT_ICON -> wantsColor ? DEFAULT_ICON : DEFAULT_MASK_ICON;
                case DANGER_ICON -> wantsColor ? DANGER_ICON : DANGER_MASK_ICON;
                default -> this;
            };
        }
    }

    private final Map<String,BufferedImage> images;
    private final Map<String,ImageIcon> imageIcons;

    /**
     * Builds a cache of Image and ImageIcon resources by iterating through all IconCache.Icon types
     */
    IconCache(Path resourcesPath) {
        this.resourcesPath = resourcesPath;
        this.images = buildImageCache();
        this.imageIcons = this.images.entrySet().stream().collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> new ImageIcon(entry.getValue())
                )
        );
        this.extractedSvgs = new ConcurrentHashMap<>();
    }

    public IconCache() {
        this(Paths.get("../resources"));
    }

    Map<String, BufferedImage> buildImageCache() {
        Map<String, BufferedImage> images = new HashMap<>();
        for(Icon i : Icon.values()) {
            for(int size : i.getSizes()) {
                BufferedImage lightImage = null;
                for(Theme theme : Theme.values()) {
                    Path found;
                    try {
                        found = findFile(i.names);
                    } catch(IOException e) {
                        if(i == Icon.DANGER_MASK_ICON) {
                            // "tray-loading" is just "tray-ready" at 50% transparency
                            images.put(i.getId(theme, size),
                                       ImageUtilities.transparent(
                                               images.get(Icon.DEFAULT_MASK_ICON.getId(theme, size)), 0.50f)
                            );
                            continue;
                        }
                        throw new RuntimeException(e);
                    }
                    Icon.Format format = Icon.Format.parse(found);
                    Path path = resourcesPath.resolve(quantifiedFileName(found.getFileName().toString(), i, theme, format, size));
                    BufferedImage image = switch(format) {
                        case PNG -> ImageUtilities.imageFromResource(path, getClass());
                        case SVG -> ImageUtilities.imageFromSvgResource(path, size, getClass());
                    };

                    // Handle undocumented macOS Sytem Tray padding
                    if (SystemUtilities.isMac() && i.getType() == SYSTEM_TRAY) {
                        image = ImageUtilities.padImage(image, 25);
                    }

                    // Handle dark fallback
                    if(theme == Theme.LIGHT) {
                        if(image != null) {
                            lightImage = image;
                        } else {
                            log.warn("No image found in {} for {} {}", resourcesPath, Arrays.toString(i.names), Arrays.toString(Icon.Format.values()));
                        }
                    } else {
                        if(image == null) {
                            // Duplicate and invert mask icons
                            image = ThemeUtilities.needsInversion(i) ? ImageUtilities.invert(lightImage) : lightImage;
                        }
                    }
                    images.put(i.getId(theme, size), image);
                }
            }
        }
        return images;
    }

    /**
     * Crawls resource path to find the first file
     */
    Path findFile(Icon.Format format, Theme theme, String ... names) {
        for(String name : names) {
            Path file;
            if(theme == Theme.DARK) {
                file = resourcesPath.resolve(String.format("%s-%s.%s", name, theme.slug(), format.slug()));
            } else {
                file = resourcesPath.resolve(String.format("%s.%s", name, format.slug()));
            }
            try(InputStream is = getClass().getResourceAsStream(file.toString())) {
                if (is != null) {
                    return file;
                }
            }
            catch(IOException ignore) {}
        }
        return null;
    }

    Path findFile(String ... names) throws IOException {
        Path found;
        for(Icon.Format format : Icon.Format.values()) {
            if((found = findFile(format, Theme.LIGHT, names)) != null) {
                return found;
            }
        }
        throw new IOException("Could not find a mandatory resource under any of the following names '" + String.join("', '", names) + "'" + Arrays.toString(Icon.Format.values()));
    }

    /**
     * Returns the ImageIcon from cache
     *
     * @param i an IconCache.Icon
     * @param theme Theme.LIGHT or Theme.DARK
     * @return the ImageIcon in the cache
     */
    ImageIcon getIcon(Icon i, Theme theme, int size) {
        return imageIcons.get(i.getId(theme, size));
    }

    public ImageIcon getIcon(Icon i, boolean isDark) {
        return getIcon(i, Theme.get(isDark), i.getSize());
    }

    public ImageIcon getIcon(Icon i) {
        return getIcon(i, false);
    }

    BufferedImage getImage(Icon i, Theme theme, int size) {
        return images.get(i.getId(theme, size));
    }

    public BufferedImage getImage(Icon i, Dimension d, boolean isDark) {
        return getImage(i, Theme.get(isDark), (int)d.getWidth());
    }

    public BufferedImage getImage(Icon i, boolean isDark) {
       return getImage(i, Theme.get(isDark), i.getSize());
    }

    public BufferedImage getImage(Icon i) {
        return  getImage(i, false);
    }

    List<BufferedImage> getImages(Icon i, Theme theme) {
        return Arrays.stream(i.getIds(theme)).map(images::get).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<BufferedImage> getImages(Icon i, boolean isDark) {
        return getImages(i, Theme.get(isDark));
    }

    public List<BufferedImage> getImages(Icon i) {
        return getImages(i, false);
    }

    public Path getSvgPath(Icon i, boolean isDark) throws IOException {
        String id = i.getId(Theme.get(isDark));
        if(extractedSvgs.containsKey(id)) {
            return extractedSvgs.get(id);
        }

        Path resource = null;
        if(isDark) {
            resource = findFile(Icon.Format.SVG, Theme.DARK, i.names);
        }
        if(resource == null) {
            resource = findFile(Icon.Format.SVG, Theme.LIGHT, i.names);
        }

        if(resource != null) {
            Path svgPath = Files.createTempFile(String.format("%s-", id), Icon.Format.SVG.extension());
            FileUtilities.configureAssetToFile(getClass(), resource.toString(), new HashMap<>(), svgPath.toFile());
            svgPath.toFile().deleteOnExit();
            extractedSvgs.put(id, svgPath);
            return svgPath;
        }
        throw new IOException("Unable to find svg resource file: '" + String.join("', '", i.names) + "'");
    }

    private static String quantifiedFileName(String fileName, Icon i, Theme theme, Icon.Format format, int size) {
        String baseName = FilenameUtils.getBaseName(fileName);
        if(format == Icon.Format.SVG || size == i.getSizes()[0]) {
            // size is not part of the filename
            return String.format(theme == Theme.DARK? "%s-dark.%s":"%s.%s", baseName, format.slug());
        }
        // size is appended to the filename
        return String.format(theme == Theme.DARK ? "%s-%s-dark.%s" : "%s-%s.%s", baseName, size, format.slug());
    }

    public synchronized static IconCache getInstance() {
        if(instance == null) {
            instance = new IconCache();
        }
        return instance;
    }
}
