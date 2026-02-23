package qz.ui.component.table;

import qz.common.Sluggable;

import java.awt.*;

import static qz.common.Constants.TRUSTED_COLOR;
import static qz.common.Constants.WARNING_COLOR;

public enum FieldStyle implements Sluggable { NORMAL, WARNING, TRUSTED;

    @Override
    public String slug() {
        return Sluggable.slugOf(this);
    }

    public boolean isBold() {
        return this != NORMAL;
    }

    public Color getColor() {
        return getColor(null);
    }

    public Color getColor(Color fallback) {
        switch(this) {
            case WARNING:
                return WARNING_COLOR;
            case TRUSTED:
                return TRUSTED_COLOR;
            case NORMAL:
            default:
        }
        return fallback;
    }
}
