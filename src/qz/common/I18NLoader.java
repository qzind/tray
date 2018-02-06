package qz.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18NLoader {
    private final static String I18N_LOCATION = "qz.common.resources.messages";

    private static final Logger log = LoggerFactory.getLogger(I18NLoader.class);

    private static ResourceBundle msg = ResourceBundle.getBundle(I18N_LOCATION);

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
}
