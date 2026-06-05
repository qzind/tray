package qz.ui.tray.linux;

import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

class LinuxDbusMenu implements CanonicalDbusMenu {

    private static final String OBJECT_PATH = "/MenuBar";
    private static final int ROOT_ID = 0;
    private static final int ABOUT_ID = 1;
    // DBusMenu protocol/interface version exposed through the Version property
    private static final UInt32 VERSION = new UInt32(3);
    // Layout revision returned by GetLayout
    // This POC menu is static, so revision
    // 1 is enough until menu items or properties become dynamic
    private static final UInt32 REVISION = new UInt32(1);
    private static final String STATUS = "normal";
    private static final String EVENT_CLICKED = "clicked";

    private final LinuxTrayAboutAction aboutAction;

    LinuxDbusMenu(LinuxTrayAboutAction aboutAction) {
        this.aboutAction = aboutAction;
    }

    @Override
    public String getObjectPath() {
        return OBJECT_PATH;
    }

    @Override
    public UInt32 getVersion() {
        return VERSION;
    }

    @Override
    public String getStatus() {
        return STATUS;
    }

    @Override
    public LinuxDbusMenuLayout getLayout(int parentId, int recursionDepth, String[] propertyNames) {
        // dbus-java maps Java arrays to D-Bus arrays for this exported interface,
        // which keeps the DBusMenu signatures aligned with libdbusmenu:
        // https://hypfvieh.github.io/dbus-java/variant-handling.html
        LinuxDbusMenuLayoutItem layout = parentId == ABOUT_ID
                ? aboutItem(propertyNames)
                : rootItem(recursionDepth, propertyNames);
        return new LinuxDbusMenuLayout(REVISION, layout);
    }

    @Override
    public LinuxDbusMenuPropertyGroup[] getGroupProperties(int[] ids, String[] propertyNames) {
        LinuxDbusMenuPropertyGroup[] groups = new LinuxDbusMenuPropertyGroup[ids.length];

        for(int i = 0; i < ids.length; i++) {
            groups[i] = new LinuxDbusMenuPropertyGroup(ids[i], getProperties(ids[i], propertyNames));
        }

        return groups;
    }

    @Override
    public Variant<?> getProperty(int id, String name) {
        Variant<?> value = getProperties(id, new String[] { name }).get(name);
        return value == null ? new Variant<>("") : value;
    }

    @Override
    public void event(int id, String eventId, Variant<?> data, UInt32 timestamp) {
        // DBusMenu sends user activation through Event/EventGroup
        // This POC only handles the single action "About" meanwhile
        if(id == ABOUT_ID && EVENT_CLICKED.equals(eventId)) {
            aboutAction.show();
        }
    }

    @Override
    public int[] eventGroup(LinuxDbusMenuEvent[] events) {
        for(LinuxDbusMenuEvent event : events) {
            event(event.getId(), event.getEventId(), event.getData(), event.getTimestamp());
        }
        return new int[0];
    }

    @Override
    public boolean aboutToShow(int id) {
        return false;
    }

    @Override
    public LinuxDbusMenuAboutToShowGroup aboutToShowGroup(int[] ids) {
        return new LinuxDbusMenuAboutToShowGroup(new int[0], new int[0]);
    }

    private LinuxDbusMenuLayoutItem rootItem(int recursionDepth, String[] propertyNames) {
        // A recursion depth of 0 asks for the root item only
        // Any other value used by tested hosts should expose
        // the one "About" item
        Variant<?>[] children = recursionDepth == 0
                ? new Variant<?>[0]
                : new Variant<?>[] { new Variant<>(aboutItem(propertyNames)) };
        return new LinuxDbusMenuLayoutItem(ROOT_ID, getProperties(ROOT_ID, propertyNames), children);
    }

    private LinuxDbusMenuLayoutItem aboutItem(String[] propertyNames) {
        return new LinuxDbusMenuLayoutItem(ABOUT_ID, getProperties(ABOUT_ID, propertyNames), new Variant<?>[0]);
    }

    private Map<String, Variant<?>> getProperties(int id, String[] propertyNames) {
        // DBusMenu item properties are a{sv}, so dbus-java represents each value
        // with Variant<?> while Java keeps the dictionary as a Map
        Map<String, Variant<?>> properties = new LinkedHashMap<>();

        if(id == ABOUT_ID) {
            addProperty(properties, propertyNames, "label", "About");
            addProperty(properties, propertyNames, "enabled", true);
            addProperty(properties, propertyNames, "visible", true);
        } else if(id == ROOT_ID) {
            addProperty(properties, propertyNames, "children-display", "submenu");
            addProperty(properties, propertyNames, "visible", true);
        }

        return properties;
    }

    private void addProperty(Map<String, Variant<?>> properties, String[] propertyNames, String name, Object value) {
        if(propertyNames == null || propertyNames.length == 0 || Arrays.asList(propertyNames).contains(name)) {
            properties.put(name, new Variant<>(value));
        }
    }
}
