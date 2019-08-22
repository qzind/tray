package qz.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

public class I18NLoader {
    private static final Logger log = LoggerFactory.getLogger(I18NLoader.class);

    private static LanguageBundle msg;
    private static PropertyHelper prefs;

    private static List<Consumer<Locale>> localeChangeListeners = new LinkedList<>();

    /**
     * Get localized strings
     *
     * @param id String ID to look up
     * @return Localized version of the given string id
     */
    public static String gettext(String id) {
        if (msg == null) return id;
        try {
            return msg.getString(id);
        }
        catch(MissingResourceException e) {
            // fail-safe action, we'll just return original string
            log.error(e.toString());
            return id;
        }
    }

    public static Locale getCurrentLocale() {
        return Locale.forLanguageTag(prefs.getProperty(Constants.I18N_LOCALE_PROPERTY, Constants.DEFAULT_LOCALE));
    }

    /**
     * Change Locale for the i18n system
     *
     * @param locale Locale to change to
     */
    public static void changeLocale(Locale locale) {
        prefs.setProperty(Constants.I18N_LOCALE_PROPERTY, locale.toLanguageTag());
        prefs.save();

        setBundle(Constants.I18N_LOCATION, locale);

        Constants.updateLocalizedConstants();
        localeChangeListeners.forEach(listener -> listener.accept(locale));
    }

    /**
     * set global user properties helper
     *
     * @param propertyHelper PropertyHelper
     */
    public static void setup(PropertyHelper propertyHelper) {
        prefs = propertyHelper;

        setBundle(Constants.I18N_LOCATION, getCurrentLocale());
    }

    public static void addLocaleChangeListener(Consumer<Locale> listener) {
        localeChangeListeners.add(listener);
    }

    private static void setBundle(String bundleDirectory, Locale locale) {
        try {
            msg = (locale == Constants.BUILT_IN_LOCALE) ? null : new LanguageBundle(bundleDirectory, locale);
        }
        catch(Exception e) {
            msg = null;
            log.error("Error loading language pack '{}' from {}", getCurrentLocale(), Constants.I18N_LOCATION);
        }
    }
}

