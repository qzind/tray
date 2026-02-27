package qz.common;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public interface Sluggable {
    String slug();

    static String slugOf(Enum<?> e) {
        return e.name().toLowerCase(Locale.ENGLISH).replace('_', '-');
    }

    /**
     * Useful for logging or serializing
     */
    static String sluggedArrayString(Sluggable ... values) {
        return "['" + Arrays.stream(values)
                .map(Sluggable::slug)
                .collect(Collectors.joining("', '")) + "']";
    }

}
