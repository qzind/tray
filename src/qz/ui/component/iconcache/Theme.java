package qz.ui.component.iconcache;

import qz.common.Sluggable;

public enum Theme implements Sluggable {
    LIGHT, DARK; // order required for fallback

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
}
