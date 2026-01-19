package qz.common;

import java.util.Locale;

public interface Sluggable {
    String slug();

    static String slugOf(Enum<?> e) {
        return e.name().toLowerCase(Locale.ENGLISH).replace('_', '-');
    }
}
