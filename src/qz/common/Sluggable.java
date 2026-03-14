package qz.common;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public interface Sluggable {
    String slug();

    static String slugOf(Enum<?> e) {
        return slugOf(e.name());
    }
    static String slugOf(String unslugged) {
        if(unslugged == null || unslugged.isBlank()) return null;
        return unslugged.toLowerCase(Locale.ENGLISH).replace('_', '-').replaceAll("\\s", "");
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
