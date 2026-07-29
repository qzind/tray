package qz.ui.tray.linux;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.errors.PropertyReadOnly;
import org.freedesktop.dbus.errors.UnknownInterface;
import org.freedesktop.dbus.errors.UnknownProperty;
import org.freedesktop.dbus.types.Variant;

import java.util.LinkedHashMap;
import java.util.Map;

public class LinuxStatusNotifierItem implements KdeStatusNotifierItem, FreedesktopStatusNotifierItem {

    private static final String KDE_INTERFACE = "org.kde.StatusNotifierItem";
    private static final String FREEDESKTOP_INTERFACE = "org.freedesktop.StatusNotifierItem";
    private static final String OBJECT_PATH = "/StatusNotifierItem";
    private static final DBusPath MENU_PATH = new DBusPath("/MenuBar");
    private static final String CATEGORY = "ApplicationStatus";
    private static final String ID = "qz-tray";
    private static final String TITLE = "QZ Tray";
    private static final String STATUS = "Active";
    // This must match the generated icon theme name exactly
    private static final String THEMED_ICON_NAME = "qz-tray-symbolic";

    private final String iconThemePath;
    private final String iconName;

    LinuxStatusNotifierItem(String iconThemePath, String iconName) {
        this.iconThemePath = iconThemePath;
        this.iconName = iconName;
    }

    @Override
    public String getObjectPath() {
        return OBJECT_PATH;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> A Get(String interfaceName, String propertyName) {
        Variant<?> value = getAllProperties(interfaceName).get(propertyName);
        if(value == null) {
            throw new UnknownProperty("Unknown StatusNotifier property " + propertyName);
        }
        return (A)value;
    }

    @Override
    public <A> void Set(String interfaceName, String propertyName, A value) {
        validateInterface(interfaceName);
        if(!getAllProperties(interfaceName).containsKey(propertyName)) {
            throw new UnknownProperty("Unknown StatusNotifier property " + propertyName);
        }
        throw new PropertyReadOnly("StatusNotifier property " + propertyName + " is read-only");
    }

    @Override
    public Map<String, Variant<?>> GetAll(String interfaceName) {
        return getAllProperties(interfaceName);
    }

    @Override
    public void contextMenu(int x, int y) {}

    @Override
    public void activate(int x, int y) {}

    @Override
    public void secondaryActivate(int x, int y) {}

    @Override
    public void scroll(int delta, String orientation) {}

    static String getThemedIconName() {
        return THEMED_ICON_NAME;
    }

    private Map<String, Variant<?>> getAllProperties(String interfaceName) {
        validateInterface(interfaceName);

        Map<String, Variant<?>> properties = new LinkedHashMap<>();
        properties.put("Category", new Variant<>(CATEGORY));
        properties.put("Id", new Variant<>(ID));
        properties.put("Title", new Variant<>(TITLE));
        properties.put("Status", new Variant<>(STATUS));
        properties.put("IconName", new Variant<>(iconName));
        properties.put("IconThemePath", new Variant<>(iconThemePath));
        // Ubuntu GNOME requires a non-empty Menu path before retaining the item
        properties.put("Menu", new Variant<>(MENU_PATH));
        return properties;
    }

    private void validateInterface(String interfaceName) {
        if(!KDE_INTERFACE.equals(interfaceName) && !FREEDESKTOP_INTERFACE.equals(interfaceName)) {
            throw new UnknownInterface("Unknown StatusNotifier interface " + interfaceName);
        }
    }
}
