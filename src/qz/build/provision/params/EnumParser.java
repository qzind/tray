package qz.build.provision.params;

import java.util.EnumSet;
import java.util.HashSet;

public interface EnumParser {
    /**
     * Basic enum parser
     */

    static <T extends Enum<T>> T parse(Class<T> clazz, String s) {
        return parse(clazz, s, null);
    }

    static <T extends Enum<T>> T parse(Class<T> clazz, String s, T fallbackValue) {
        if(s != null) {
            for(T en : EnumSet.allOf(clazz)) {
                if (en.name().equalsIgnoreCase(s)) {
                    return en;
                }
            }
        }
        return fallbackValue;
    }

    static <T extends Enum<T>> T parseStrict(Class<T> clazz, String s, T ... blocklist) throws UnsupportedOperationException {
        if(s != null) {
            HashSet<T> matched = parseSet(clazz, null, s);
            if (matched.size() == 1) {
                T returnVal = matched.iterator().next();
                boolean blocked = false;
                for(T block : blocklist) {
                    if(returnVal == block) {
                        blocked = true;
                        break;
                    }
                }
                if(!blocked) {
                    return returnVal;
                }
            }
        }
        throw new UnsupportedOperationException(String.format("%s value '%s' failed to match one and only one item", clazz.getSimpleName(), s));
    }

    static <T extends Enum<T>> HashSet<T> parseSet(Class<T> clazz, T all, String s) {
        HashSet<T> matched = new HashSet<>();
        if(s != null) {
            // Handle ALL="*"
            if (all != null && s.equals("*")) {
                matched.add(all);
            }

            String[] parts = s.split("\\|");
            for(String part : parts) {
                T parsed = parse(clazz, part);
                if (parsed != null) {
                    matched.add(parsed);
                }
            }
        }
        return matched;
    }
}
