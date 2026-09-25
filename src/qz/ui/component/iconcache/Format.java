package qz.ui.component.iconcache;

import qz.common.Sluggable;

import java.nio.file.Path;

public enum Format implements Sluggable {
    SVG, PNG, UNKNOWN; // order sets precedent

    static Format parse(Path path) {
        for(Format format : Format.values()) {
            String extension = format.extension();
            if(extension != null && path.toString().endsWith(extension)) {
                return format;
            }
        }
        return UNKNOWN;
    }

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
