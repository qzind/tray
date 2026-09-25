package qz.ui.component.iconcache;

import qz.ui.component.IconCache;
import qz.utils.FileUtilities;
import qz.utils.ImageUtilities;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;

import static qz.utils.ImageUtilities.*;

public class Cache {
    final IconCache.Icon icon;
    final Theme theme;
    final int size;

    Path basePath;
    BufferedImage bufferedImage;
    ImageIcon imageIcon;
    Format format;

    public Cache(IconCache.Icon icon, Theme theme, int size) {
        this.icon = icon;
        this.theme = theme;
        this.size = size;
    }

    public boolean load(Path resources) {
        for(String name : icon.getNames()) {
            for(Format format : Format.values()) {
                if(!format.validExtension()) {
                    continue;
                }
                Path basePath = FileUtilities.resourcePath(resources.resolve(getBaseName(name, format)));
                BufferedImage bufferedImage = ImageUtilities.imageFromResource(basePath, size);

                if (bufferedImage != null) {
                    this.basePath = basePath;
                    this.format = format;
                    setImages(bufferedImage);
                    return true;
                }
            }
        }
        return false;
    }

    void setImages(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage);
        this.bufferedImage = bufferedImage;
        this.imageIcon = new ImageIcon(bufferedImage);
    }

    public void setImages(Cache cache) {
        Objects.requireNonNull(cache);
        this.basePath = cache.basePath;
        this.bufferedImage = Objects.requireNonNull(cache.bufferedImage);
        this.imageIcon = Objects.requireNonNull(cache.imageIcon);
        this.format = Objects.requireNonNull(cache.format);
    }

    public void invertImage() {
        this.basePath = null; // can't trust origin after mutation
        setImages(ImageUtilities.invert(bufferedImage));
    }

    public void fadeImage(Cache cache, float amount) {
        setImages(cache);
        this.basePath = null; // we have no basepath for cloned images :/
        setImages(addTransparency(bufferedImage, amount));
    }

    public void padImage(float amount) {
        setImages(addPadding(bufferedImage, amount));
    }

    /**
     * Get the expected filename
     */
    public String getBaseName(String name, Format format) {
        if (format == Format.SVG || size == icon.getType().getSizes()[0]) {
            // size is not part of filenames
            return String.format(theme == Theme.DARK? "%s-dark.%s":"%s.%s", name, format.slug());
        }
        // size is part of png filenames
        return String.format(theme == Theme.DARK? "%s-dark-%s.%s":"%s-%s.%s", name, size, format.slug());
    }

    public Theme getTheme() {
        return theme;
    }

    public IconCache.Icon getIcon() {
        return icon;
    }

    public boolean missing() {
        return bufferedImage == null;
    }

    public ImageIcon getImageIcon() {
        return imageIcon;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public Path getBasePath() {
        return basePath;
    }

    public String getKey() {
        return getKey(icon, theme, size);
    }

    public String themedKey(Theme theme) {
        return getKey(icon, theme, size);
    }

    public String swapedKey(IconCache.Icon icon) {
        return getKey(icon, theme, size);
    }

    public static String getKey(IconCache.Icon icon, Theme theme, int size) {
        if(size < 1) {
            size = icon.getType().getSizes()[0];
        }
        return String.format("%s-%s-%s", icon.slug(), theme.slug(), size);
    }
}