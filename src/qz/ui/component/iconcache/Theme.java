package qz.ui.component.iconcache;

import qz.common.Sluggable;

import java.awt.*;

public enum Theme implements Sluggable {
    LIGHT(Color.BLACK),
    DARK(Color.WHITE);

    private final Color fillColor;

    Theme(Color fillColor) {
        this.fillColor = fillColor;
    }

    public static Theme parse(boolean isDark) {
        return isDark ? DARK : LIGHT;
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

    public boolean isDark() {
        return this == DARK;
    }

    @Override
    public String slug() {
        return Sluggable.slugOf(this);
    }

    /**
     * Returns the fill color (e.g. <code>Color.BLACK</code>, <code>Color.WHITE</code>) that
     * this theme would generally need for maximum compatibility with the given theme.
     */
    public Color getFill() {
        return fillColor;
    }
}
