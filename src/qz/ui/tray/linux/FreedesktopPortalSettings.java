package qz.ui.tray.linux;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

@DBusInterfaceName("org.freedesktop.portal.Settings")
public interface FreedesktopPortalSettings extends DBusInterface {

    // ReadOne avoids the deprecated Read method's extra variant layer
    // https://flatpak.github.io/xdg-desktop-portal/docs/doc-org.freedesktop.portal.Settings.html
    @DBusMemberName("ReadOne")
    Variant<?> readOne(String namespace, String key);

    // Emitted when org.freedesktop.appearance color-scheme changes
    class SettingChanged extends DBusSignal {

        public final String namespace;
        public final String key;
        public final Variant<?> value;

        // Keep public for dbus-java signal materialization
        public SettingChanged(String path, String namespace, String key, Variant<?> value) throws DBusException {
            super(path, namespace, key, value);
            this.namespace = namespace;
            this.key = key;
            this.value = value;
        }
    }
}
