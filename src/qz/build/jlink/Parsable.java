package qz.build.jlink;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * A special template class for handling enums with varargs needing string matches.
 *
 * Parsable enums must declare <code>public static void String[] matches;</code>
 * in the constructor, which <code>parse(Class enumType, </T>String value) will
 * call using reflection.
 *
 * Enums are inherently static in Java and cannot extend superclasses.  The
 * workaround to avoid code duplication is to leverage reflection and generics in
 * static utility functions.
 *
 * The downsides of this are:
 * - Reflection is slow
 * - Static helpers must be explicitly class-type-aware*
 *
 * *Non-static methods may be implicit, but create anti-patterns for static helpers
 * such as <code>parse(String value)</code> as they would exist at
 * <code>ENUM_ENTRY.parse(...)</code> rather than <code>EnumClass.parse(...)</code>.
 */
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

    static <T extends Enum<T>> T parse(Class<T> enumType, String value, T fallback, boolean silent) {
        if(value != null && !value.trim().isEmpty()) {
            return parse(enumType, value);
        }
        if(!silent) {
            log.warn("No {} specified, assuming '{}'", enumType.getSimpleName(), ((Parsable)fallback).value());
        }
        return fallback;
    }

    static <T extends Enum<T>> T parse(Class<T> enumType, String value, T fallback) {
        return parse(enumType, value, fallback, false);
    }

    default String value() {
        return ((T)this).toString().toLowerCase(Locale.ENGLISH);
    }
}
