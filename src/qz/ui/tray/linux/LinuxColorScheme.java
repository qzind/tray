package qz.ui.tray.linux;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.types.Variant;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class LinuxColorScheme implements AutoCloseable {

    private static final Logger log = LogManager.getLogger(LinuxColorScheme.class);
    private static final String PORTAL_SERVICE = "org.freedesktop.portal.Desktop";
    private static final String PORTAL_PATH = "/org/freedesktop/portal/desktop";
    private static final String APPEARANCE_NAMESPACE = "org.freedesktop.appearance";
    private static final String COLOR_SCHEME_KEY = "color-scheme";

    enum Preference {
        UNAVAILABLE(-1),
        NO_PREFERENCE(0),
        PREFER_DARK(1),
        PREFER_LIGHT(2);

        private final int value;

        Preference(int value) {
            this.value = value;
        }

        static Preference fromVariant(Variant<?> variant) {
            Object value = unwrap(variant);
            if(value instanceof Number) {
                return fromInt(((Number)value).intValue());
            }
            if(value instanceof String) {
                try {
                    return fromInt(Integer.parseInt((String)value));
                }
                catch(NumberFormatException ignore) {
                    return NO_PREFERENCE;
                }
            }
            return NO_PREFERENCE;
        }

        private static Preference fromInt(int value) {
            for(Preference preference : values()) {
                if(preference.value == value) {
                    return preference;
                }
            }
            return NO_PREFERENCE;
        }

        private static Object unwrap(Object value) {
            Object current = value;
            while(current instanceof Variant<?>) {
                current = ((Variant<?>)current).getValue();
            }
            return current;
        }
    }

    private final AtomicReference<Preference> preference = new AtomicReference<>(Preference.UNAVAILABLE);
    private final Object startupLock = new Object();
    private AutoCloseable settingChangedRegistration;
    private boolean startupReadActive = true;
    private boolean startupSignalSeen;

    static LinuxColorScheme watch(DBusConnection connection, Consumer<Preference> listener) {
        LinuxColorScheme colorScheme = new LinuxColorScheme();
        colorScheme.settingChangedRegistration = colorScheme.registerSettingChanged(connection, listener);
        colorScheme.readStartupPreference(connection, listener);
        return colorScheme;
    }

    Preference getPreference() {
        return preference.get();
    }

    @Override
    public void close() {
        if(settingChangedRegistration != null) {
            try {
                settingChangedRegistration.close();
            }
            catch(Exception e) {
                log.warn("Unable to remove Linux color-scheme listener", e);
            }
        }
    }

    private void readInitialPreference(DBusConnection connection, Consumer<Preference> listener) {
        try {
            updatePreference(getSettings(connection).readOne(APPEARANCE_NAMESPACE, COLOR_SCHEME_KEY), listener);
        }
        catch(Exception e) {
            // XDG portal settings are optional outside sandboxed apps
            log.debug("Unable to read Linux color-scheme preference", e);
            listener.accept(Preference.UNAVAILABLE);
        }
    }

    private void readStartupPreference(DBusConnection connection, Consumer<Preference> listener) {
        do {
            synchronized(startupLock) {
                startupSignalSeen = false;
            }

            readInitialPreference(connection, listener);

            synchronized(startupLock) {
                if(!startupSignalSeen) {
                    startupReadActive = false;
                    return;
                }
            }
        } while(true);
    }

    private AutoCloseable registerSettingChanged(DBusConnection connection, Consumer<Preference> listener) {
        try {
            return connection.addSigHandler(FreedesktopPortalSettings.SettingChanged.class, signal -> {
                if(APPEARANCE_NAMESPACE.equals(signal.namespace) && COLOR_SCHEME_KEY.equals(signal.key)) {
                    synchronized(startupLock) {
                        if(startupReadActive) {
                            // Re-read after startup signals so the final value wins
                            startupSignalSeen = true;
                            return;
                        }
                    }
                    Preference preference = Preference.fromVariant(signal.value);
                    this.preference.set(preference);
                    listener.accept(preference);
                    log.info("Linux color-scheme preference changed to {}", preference);
                }
            });
        }
        catch(Exception e) {
            log.debug("Unable to watch Linux color-scheme preference", e);
            return null;
        }
    }

    private void updatePreference(Variant<?> value, Consumer<Preference> listener) {
        Preference newPreference = Preference.fromVariant(value);
        preference.set(newPreference);
        listener.accept(newPreference);
        log.info("Linux color-scheme preference is {}", newPreference);
    }

    private static FreedesktopPortalSettings getSettings(DBusConnection connection) throws Exception {
        return connection.getRemoteObject(
                PORTAL_SERVICE,
                PORTAL_PATH,
                FreedesktopPortalSettings.class,
                false
        );
    }
}
