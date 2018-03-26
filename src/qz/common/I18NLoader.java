package qz.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18NLoader {
    private final static String I18N_LOCATION = "qz.common.resources.messages";

    private static final Logger log = LoggerFactory.getLogger(I18NLoader.class);

    private static ResourceBundle msg = Utf8ResourceBundle.getBundle(I18N_LOCATION);

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

    /**
     * Change Locale for the i18n system
     * @param locale Locale to change to
     */
    public static void changeLocale(Locale locale) {
        msg = Utf8ResourceBundle.getBundle(I18N_LOCATION, locale);
    }
}
