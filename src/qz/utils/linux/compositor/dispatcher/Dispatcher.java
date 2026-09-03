package qz.utils.linux.compositor.dispatcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

import static qz.utils.linux.compositor.dispatcher.ExecutorCache.*;

public abstract class Dispatcher {
    private static final Logger log = LogManager.getLogger(Dispatcher.class);

    protected abstract void addMatchers();

    public Dispatcher() {
        // Always try dbus freedesktop technique first
        addMatcher(ExecutorCache.DARK_MODE, "uint32\\s+(\\d+)", DBUS_COLOR_SCHEME);

        addMatchers();
    }

    private static final String[] DARK_SUFFIXES = {
            "dark",
            "mocha",
            "night",
            "storm"
    };

    private static final String[] LIGHT_SUFFIXES = {
            "light",
            "latte"
    };

    private final String[] DBUS_COLOR_SCHEME = {
            "gdbus", "call", "--session",
            "--dest", "org.freedesktop.portal.Desktop",
            "--object-path", "/org/freedesktop/portal/desktop",
            "--method", "org.freedesktop.portal.Settings.ReadOne",
            "org.freedesktop.appearance", "color-scheme"
    };

    protected void addMatcher(ExecutorCache type, String match, String... execute) {
        type.add(new Executor(match, execute));
    }

    public Double getScaleFactor() {
        Executor executor = SCALE_FACTOR.getExecutor();
        if(executor != null) {
            Double scaleFactor = executor.getDouble();
            if(scaleFactor != null) {
                return scaleFactor;
            }
        }

        log.warn("Can't detect scale factor, defaulting to 1.0");
        return 1.0;
    }

    public Boolean isDarkMode() {
        Executor executor = DARK_MODE.getExecutor();
        if(executor != null) {
            if(executor.execute == DBUS_COLOR_SCHEME) {
                // dbus uses integer
                return isDark(executor.getInteger());
            } else {
                // others use string
                String darkMode = executor.getString();
                if (darkMode != null) {
                    return isDark(darkMode);
                }
            }
        }
        log.warn("Can't detect dark mode, defaulting to light mode");
        return false;
    }

    /**
     * Detect if <code>themeValue</code> is dark
     * 0 = No preference; 1 = Prefer dark; 2 = Prefer light
     */
    static boolean isDark(Integer themeValue) {
        return switch(themeValue) {
            case 1 -> true;
            case 0, 2 -> false;
            default -> false;
        };
    }

    static boolean isDark(String themeName) {
        if(themeName == null || themeName.isEmpty()) {
            return false;
        }
        String theme = themeName.toLowerCase(Locale.ENGLISH);
        for(String suffix : LIGHT_SUFFIXES) {
            if(theme.endsWith(suffix)) {
                return false;
            }
        }
        for(String suffix : DARK_SUFFIXES) {
            if(theme.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
