package qz.common;

import java.util.Locale;

public interface Sluggable {
    String slug();

    static String slugOf(Enum<?> e) {
        return slugOf(e.name());
    }
    static String slugOf(String unslugged) {
        if(unslugged == null || unslugged.isBlank()) return null;
        return unslugged.toLowerCase(Locale.ENGLISH).replace('_', '-').replaceAll("\\s", "");
    }
}
