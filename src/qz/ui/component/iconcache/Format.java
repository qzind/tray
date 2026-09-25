package qz.ui.component.iconcache;

import qz.common.Sluggable;

public enum Format implements Sluggable {
    SVG, PNG, UNKNOWN; // order sets precedent

    @Override
    public String slug() {
        return Sluggable.slugOf(this);
    }

    public String extension() {
        return validExtension() ? String.format(".%s", slug()) : null;
    }

    public boolean validExtension() {
        return this != UNKNOWN;
    }
}
