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

import static qz.ui.component.IconCache.Icon.Type.*;

public class IconCache {
    private static final Logger log = LogManager.getLogger(IconCache.class);
    private static IconCache instance;
    private final Path resourcesPath;

    final HashMap<String, Path> foundResources = new HashMap<>();
    final ConcurrentHashMap<String, Path> extractedImages;

    /**
     * Enum for building and tracking icon keys for PNG (pre-rasterized) or SVG (runtime rasterized) images
     */
    public enum Icon implements Sluggable {
        // System tray
        DEFAULT_MASK_ICON(SYSTEM_TRAY, "tray-ready", "qz-mask"),
        DANGER_MASK_ICON(SYSTEM_TRAY, "tray-loading"),

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
            SVG, PNG; // order sets precedent

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

            public static Theme parse(String id) {
                if(id == null) {
                    return LIGHT;
                }
                if (id.equalsIgnoreCase(DARK.slug()) ||
                        id.contains("-" + DARK.slug() + "-") ||
                        id.endsWith("-" + DARK.slug())) {
                    return DARK;
                }
                return LIGHT;
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

        String[] getIds() {
            return getIds(Theme.values());
        }

        String getId(Theme theme) {
            return getId(theme, 0);
        }

        static String idSwap(String id, Theme from, Theme to) {
            return id.replace("-" + from.slug(), "-" + to.slug());
        }

        static String idSwap(String id, Icon from, Icon to) {
            return id.replace(from.slug(), to.slug());
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

        public static Icon fromId(String toMatch) {
            for(Icon icon : Icon.values()) {
                for(String id : icon.getIds()) {
                    if(id.equals(toMatch)) {
                        return icon;
                    }
                }
            }
            return null;
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
        this.extractedImages = new ConcurrentHashMap<>();
    }

    public IconCache() {
        this(Paths.get("resources"));
    }

    Map<String, BufferedImage> buildImageCache() {
        Map<String, BufferedImage> imageMap = new LinkedHashMap<>();
        for(Icon i : Icon.values()) {
            for(int size : i.getSizes()) {
                theme:
                for(Theme theme : Theme.values()) {
                    String id = i.getId(theme, size);
                    imageMap.put(id, null); // placeholder
                    // for backwards-compat we have to support historical filenames too
                    for(String name : i.names) {
                        for(IconCache.Icon.Format format : IconCache.Icon.Format.values()) {
                            String quantifiedResource = quantifiedResource(name, i, theme, format, size);
                            Path imagePath = FileUtilities.resourcePath(resourcesPath.resolve(quantifiedResource));
                            BufferedImage image = ImageUtilities.imageFromResource(imagePath, size);
                            if(image != null) {
                                imageMap.put(id, image);
                                String cachedId = format == Icon.Format.SVG ? i.getId(theme, 0) : id;
                                foundResources.putIfAbsent(cachedId, imagePath);
                                continue theme;
                            }
                        }
                    }
                }
            }
        }

        // Fixup our images
        for(HashMap.Entry<String, BufferedImage> entry : imageMap.entrySet()) {
            String key = entry.getKey();
            BufferedImage value = imageMap.get(entry.getKey());

            // Ensure we can reverse-lookup by id
            Icon icon = Icon.fromId(key);
            if(icon == null) {
                throw new UnsupportedOperationException("Icon " + entry.getKey() + " is unknown");
            }

            if(value == null) {
                Theme theme = Theme.parse(key);
                if(theme == Theme.LIGHT) {
                    // Handle missing "light" images
                    if (icon == Icon.DANGER_MASK_ICON) {
                        // tray-loading-light-16 = tray-ready-light-16 @ 50%, etc
                        String readyKey = Icon.idSwap(key, Icon.DANGER_MASK_ICON, Icon.DEFAULT_MASK_ICON);
                        imageMap.put(key, ImageUtilities.addTransparency(imageMap.get(readyKey), 0.50f));
                    } else {
                        throw new UnsupportedOperationException("Mandatory icon " + key + " cannot be null");
                    }
                } else if(theme == Theme.DARK) {
                    // Handle missing "dark" images
                    // tray-ready-color-dark-16 = tray-ready-color-light-16
                    String lightKey = Icon.idSwap(key, Theme.DARK, Theme.LIGHT);

                    BufferedImage darkImage = icon == Icon.DANGER_MASK_ICON && ThemeUtilities.needsInversion(icon) ?
                            ImageUtilities.invert(imageMap.get(lightKey)) :  // mask icons require inverting
                            imageMap.get(lightKey);
                    imageMap.put(key, darkImage);
                }
            } else {
                // Handle undocumented 25% padding for macOS system tray
                if (SystemUtilities.isMac() && icon.getType() == SYSTEM_TRAY) {
                    imageMap.put(key, ImageUtilities.padImage(value, 0.25f));
                }
            }
        }

        return imageMap;
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

    Path extractImage(Icon.Format format, Icon i, boolean isDark, int size) throws IOException {
        String id = i.getId(Theme.get(isDark), size);
        if(extractedImages.containsKey(id)) {
            return extractedImages.get(id);
        }

        Theme theme = Theme.get(isDark);
        if(format == Icon.Format.SVG) {
            Path imagePath = foundResources.get(id);

            Path svgPath = Files.createTempFile(String.format("%s-", id), Icon.Format.SVG.extension());
            svgPath.toFile().deleteOnExit();

            if(imagePath == null) {
                switch(i) {
                    case DANGER_MASK_ICON:
                        if(theme == Theme.DARK) {
                            throw new UnsupportedOperationException("No way to convert " + format + " from light to dark");
                        }
                        // We don't require tray-loading; try making one on-the-fly instead
                        id = Icon.idSwap(id, Icon.DANGER_MASK_ICON, Icon.DEFAULT_MASK_ICON);
                        String xmlContent = ImageUtilities.addTransparency(foundResources.get(id), 0.5f);
                        Files.writeString(svgPath, xmlContent);
                        break;
                    case DEFAULT_ICON:
                    default:
                        if(theme == Theme.DARK) {
                            // Most dark icons can just fall back to their light equivalents
                            return extractImage(format, i, false, size);
                        }
                        // We should never get here
                        throw new UnsupportedEncodingException("No cached path stored for '" + id + "'");
                }
            } else {
                FileUtilities.configureAssetToFile(ThemeUtilities.class, imagePath.toString(), new HashMap<>(), svgPath.toFile());
            }
            extractedImages.put(id, svgPath);
            return svgPath;
        } else {
            Path rasterPath = Files.createTempFile(String.format("%s-", id), format.extension());
            rasterPath.toFile().deleteOnExit();
            if (ImageIO.write(getImage(i, theme, size), format.slug(), rasterPath.toFile())) {
                extractedImages.put(id, rasterPath);
                return rasterPath;
            }
            throw new IOException("Unable to write png file: '" + rasterPath + "'");
        }
    }

    public Path extractPng(Icon i, boolean isDark, int size) throws IOException {
        return extractImage(Icon.Format.PNG, i, isDark, size);
    }

    public Path extractSvg(Icon i, boolean isDark) throws IOException {
        return extractImage(Icon.Format.SVG, i,  isDark, 0);
    }

    private static String quantifiedResource(String fileName, Icon i, Theme theme, Icon.Format format, int size) {
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
