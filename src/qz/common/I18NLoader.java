package qz.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;


public class I18NLoader {
    private final static String I18N_LOCATION = "qz.common.resources.messages";
    private final static String I18N_LOCALE_PROPERTY = "locale";
    private final static String DEFAULT_LOCALE = "en";

    private static final Logger log = LoggerFactory.getLogger(I18NLoader.class);

    private static ResourceBundle msg;
    private static PropertyHelper prefs;

    private static List<Consumer<Locale>> localeChangeListeners = new LinkedList<>();

    public static final List<Locale> SUPPORTED_LOCALES = Collections.unmodifiableList(Arrays.asList(
            Locale.forLanguageTag("de"),
            Locale.forLanguageTag("en"),
            Locale.forLanguageTag("fr"),
            Locale.forLanguageTag("zh-CN"),
            Locale.forLanguageTag("zh-TW")
    ));

    /**
     * Get localized strings
     *
     * @param id String ID to look up
     * @return Localized version of the given string id
     */

    public static String gettext(String id) {
        try {
            return msg.getString(id);
        }
        catch(MissingResourceException e) {
            // fail-safe action, we'll just return original string
            log.error(String.format("Cannot find translation for `%s`", id));
            return id;
        }
    }

    public static Locale getCurrentLocale() {
        return Locale.forLanguageTag(prefs.getProperty(I18N_LOCALE_PROPERTY, DEFAULT_LOCALE));
    }

    /**
     * Change Locale for the i18n system
     *
     * @param locale Locale to change to
     */
    public static void changeLocale(Locale locale) {
        prefs.setProperty(I18N_LOCALE_PROPERTY, locale.toLanguageTag());
        prefs.save();

        msg = Utf8ResourceBundle.getBundle(I18N_LOCATION, getCurrentLocale());

        localeChangeListeners.forEach(listener -> listener.accept(locale));
    }

    /**
     * set global user properties helper
     *
     * @param propertyHelper PropertyHelper
     */
    public static void setup(PropertyHelper propertyHelper) {
        prefs = propertyHelper;
        msg = Utf8ResourceBundle.getBundle(I18N_LOCATION, getCurrentLocale());
    }

    public static void addLocaleChangeListener(Consumer<Locale> listener) {
        localeChangeListeners.add(listener);
    }
}

