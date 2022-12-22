package qz.build.jlink;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Locale;

public interface Parsable<T extends Enum> {
    Logger log = LogManager.getLogger(Parsable.class);

    static <T extends Enum<T>> T parse(Class<T> enumType, String value) {
        if(value != null && !value.trim().isEmpty()) {
            for(T parsable : enumType.getEnumConstants()) {
                try {
                    Field matchesField = parsable.getClass().getDeclaredField("matches");
                    String[] matches = (String[])matchesField.get(parsable);
                    for(String match : matches) {
                        if (match.equalsIgnoreCase(value)) {
                            return parsable;
                        }
                    }
                } catch(NoSuchFieldException | IllegalAccessException | ClassCastException e) {
                    log.warn("Parsable enums must have a 'public String[] matches' field", e);
                }
            }
        }
        log.warn("Could not parse {} as a valid {}} value", value, enumType.getSimpleName());
        return null;
    }

    static <T extends Enum<T>> T parse(Class<T> enumType, String value, T fallback) {
        if(value != null && !value.trim().isEmpty()) {
            return parse(enumType, value);
        }
        return fallback;
    }

    static String value(Enum type) {
        return type.toString().toLowerCase(Locale.ENGLISH);
    }

    String value();
}
