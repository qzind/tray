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
import qz.ui.component.IconCache.Icon.Theme;
import qz.utils.ColorUtilities;
import qz.utils.ImageUtilities;
import qz.utils.SystemUtilities;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static qz.ui.component.IconCache.Icon.Type.*;

public class IconCache {
    private static final Logger log = LogManager.getLogger(IconCache.class);

    private static final Path RESOURCES_PATH = Paths.get("../resources");

    private static IconCache instance;

    /**
     * Enum for building and tracking icon keys for PNG (pre-rasterized) or SVG (runtime rasterized) images
     */
    public enum Icon implements Sluggable {
        // System tray
        DEFAULT_ICON(SYSTEM_TRAY, "tray-default", "qz-default"),
        WARNING_ICON(SYSTEM_TRAY, "tray-warning", "qz-warning"),
        DANGER_ICON(SYSTEM_TRAY, "tray-danger", "qz-danger"),
        MASK_ICON(SYSTEM_TRAY, "tray-mask", "qz-mask"),

        // Task bar
        TASK_BAR_ICON(TASK_BAR, "tray-default", "qz-default"),

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

        enum Format implements Sluggable {
            PNG, SVG; // order sets precedent

            static Format parse(Path path) {
                for(Format format : Format.values()) {
                    if(path.toString().endsWith(String.format(".%s", format.slug()))) {
                        return format;
                    }
                }
                return PNG;
            }

            @Override
            public String slug() {
                return Sluggable.slugOf(this);
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

        private final String name;
        private final Type type;
        private final Format format;
        private final String slug;

        Icon(Type type, String ... names) {
            this.type = type;
            Path path = findFile(names);
            this.name = path.getFileName().toString().split("\\.", 2)[0];
            this.format = Format.parse(path);
            this.slug = Sluggable.slugOf(this).replace("_icon", ""); // TODO: Remove "_ICON" suffix
        }

        String getFileName(Theme theme, int size) {
            if(format == Format.PNG && size != type.sizes[0]) {
                // expect custom sizes to be appended to the filename
                return String.format(theme == Theme.DARK ? "%s-%s-dark.%s" : "%s-%s.%s", name, size, format.slug());
            }
            // one size fits all
            return  String.format(theme == Theme.DARK ? "%s-dark.%s" : "%s.%s", name, format.slug());
        }

        Path getPath(Theme theme, int size) {
            return RESOURCES_PATH.resolve(getFileName(theme, size));
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

        String getId(Theme theme, int size) {
            return String.format("%s-%s-%s", slug, theme.slug(), size);
        }

        Format getFormat() {
            return format;
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
         * Crawls resource path to find the first file
         */
        static Path findFile(String ... names) {
            ArrayList<String> attempted = new ArrayList<>();
            for(Format format : Format.values()) {
                for(String name : names) {
                    Path file = RESOURCES_PATH.resolve(String.format("%s.%s", name, format.slug()));
                    attempted.add(file.toString());
                    try(InputStream is = IconCache.class.getResourceAsStream(file.toString())) {
                        if (is != null) {
                            return file;
                        }
                    }
                    catch(IOException ignore) {}
                }
            }
            throw new UnsupportedOperationException("Could not find a mandatory resource under any of the following names: '" + String.join("', '", attempted) + "'");
        }
    }

    private final Map<String,BufferedImage> images;
    private final Map<String,ImageIcon> imageIcons;

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
            for(int size : i.getSizes()) {
                BufferedImage lightImage = null;
                for(Theme theme : Theme.values()) {
                    Path path = i.getPath(theme, size);
                    BufferedImage image = switch(i.getFormat()) {
                        case PNG -> ImageUtilities.imageFromResource(path, this);
                        case SVG -> ImageUtilities.imageFromSvgResource(path, size, this);
                    };

                    // Handle undocumented macOS Sytem Tray padding
                    if (SystemUtilities.isMac() && i.getType() == SYSTEM_TRAY) {
                        image = ImageUtilities.padImage(image, 25);
                    }

                    log.debug("Trying {}", path);

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

    public static IconCache getInstance() {
        if(instance == null) {
            instance = new IconCache();
        }
        return instance;
    }
}
