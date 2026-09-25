package qz.ui.component.iconcache;

import qz.common.Sluggable;

import java.awt.*;

public enum Theme implements Sluggable {
    LIGHT(Color.BLACK), DARK(Color.WHITE); // order required for fallback

    private final Color fillColor;

    Theme(Color fillColor) {
        this.fillColor = fillColor;
    }

    public static Theme get(boolean isDark) {
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

    @Override
    public String slug() {
        return Sluggable.slugOf(this);
    }

    public Color getFillColor() {
        return fillColor;
    }
}
