package qz.ui.tray.linux;

import com.github.zafarkhaja.semver.Version;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import qz.common.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class LinuxDbusMenu implements CanonicalDbusMenu {

    private static final String OBJECT_PATH = "/MenuBar";
    private static final int ROOT_ID = 0;
    private static final int ADVANCED_ID = 100;
    private static final int DIAGNOSTIC_ID = 110;
    private static final int BROWSE_APP_ID = 111;
    private static final int BROWSE_USER_ID = 112;
    private static final int BROWSE_SHARED_ID = 113;
    private static final int DIAGNOSTIC_OPTIONS_SEPARATOR_ID = 114;
    private static final int NOTIFICATIONS_ID = 115;
    private static final int MONOCLE_ID = 116;
    private static final int DIAGNOSTIC_LOGS_SEPARATOR_ID = 117;
    private static final int VIEW_LOGS_ID = 118;
    private static final int ZIP_LOGS_ID = 119;
    private static final int ADVANCED_DIAGNOSTIC_SEPARATOR_ID = 120;
    private static final int SITE_MANAGER_ID = 121;
    private static final int DESKTOP_SHORTCUT_ID = 122;
    private static final int ADVANCED_OPTIONS_SEPARATOR_ID = 123;
    private static final int BLOCK_ANONYMOUS_ID = 124;
    private static final int RELOAD_ID = 200;
    private static final int ABOUT_ID = 201;
    private static final int AUTOSTART_ID = 202;
    private static final int EXIT_SEPARATOR_ID = 203;
    private static final int EXIT_ID = 204;
    // DBusMenu protocol/interface version exposed through the Version property
    private static final UInt32 VERSION = new UInt32(3);
    // Layout revision returned by GetLayout
    // This POC menu is static, so revision
    // 1 is enough until menu items or properties become dynamic
    private static final UInt32 REVISION = new UInt32(1);
    private static final String STATUS = "normal";
    private static final String EVENT_CLICKED = "clicked";

    private final LinuxTrayAboutAction aboutAction;
    private final LinuxTrayExitAction exitAction;

    LinuxDbusMenu(LinuxTrayAboutAction aboutAction, LinuxTrayExitAction exitAction) {
        this.aboutAction = aboutAction;
        this.exitAction = exitAction;
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
        LinuxDbusMenuLayoutItem layout = getItem(parentId, recursionDepth, propertyNames);
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
        // DBusMenu sends activation through Event/EventGroup.
        // Only the actions already proven by this POC are wired here.
        if(id == ABOUT_ID && EVENT_CLICKED.equals(eventId)) {
            aboutAction.show();
        } else if(id == EXIT_ID && EVENT_CLICKED.equals(eventId)) {
            exitAction.exit();
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

    private LinuxDbusMenuLayoutItem getItem(int id, int recursionDepth, String[] propertyNames) {
        List<Variant<?>> children = new ArrayList<>();

        if(recursionDepth != 0) {
            int childDepth = recursionDepth < 0 ? recursionDepth : recursionDepth - 1;
            for(int childId : getChildIds(id)) {
                children.add(new Variant<>(getItem(childId, childDepth, propertyNames)));
            }
        }

        return new LinuxDbusMenuLayoutItem(
                id,
                getProperties(id, propertyNames),
                children.toArray(new Variant<?>[0])
        );
    }

    private int[] getChildIds(int id) {
        if(id == ROOT_ID) {
            return new int[] {
                    ADVANCED_ID,
                    RELOAD_ID,
                    ABOUT_ID,
                    AUTOSTART_ID,
                    EXIT_SEPARATOR_ID,
                    EXIT_ID
            };
        }

        if(id == ADVANCED_ID) {
            List<Integer> children = new ArrayList<>();
            if(Constants.ENABLE_DIAGNOSTICS) {
                children.add(DIAGNOSTIC_ID);
                children.add(ADVANCED_DIAGNOSTIC_SEPARATOR_ID);
            }
            children.add(SITE_MANAGER_ID);
            children.add(DESKTOP_SHORTCUT_ID);
            children.add(ADVANCED_OPTIONS_SEPARATOR_ID);
            children.add(BLOCK_ANONYMOUS_ID);
            return children.stream().mapToInt(Integer::intValue).toArray();
        }

        if(id == DIAGNOSTIC_ID) {
            List<Integer> children = new ArrayList<>(Arrays.asList(
                    BROWSE_APP_ID,
                    BROWSE_USER_ID,
                    BROWSE_SHARED_ID,
                    DIAGNOSTIC_OPTIONS_SEPARATOR_ID,
                    NOTIFICATIONS_ID
            ));
            if(Constants.JAVA_VERSION.greaterThanOrEqualTo(Version.valueOf("11.0.0"))) {
                children.add(MONOCLE_ID);
            }
            children.add(DIAGNOSTIC_LOGS_SEPARATOR_ID);
            children.add(VIEW_LOGS_ID);
            children.add(ZIP_LOGS_ID);
            return children.stream().mapToInt(Integer::intValue).toArray();
        }

        return new int[0];
    }

    private Map<String, Variant<?>> getProperties(int id, String[] propertyNames) {
        // DBusMenu item properties are a{sv}, so dbus-java represents each value
        // with Variant<?> while Java keeps the dictionary as a Map
        Map<String, Variant<?>> properties = new LinkedHashMap<>();

        if(isSeparator(id)) {
            addProperty(properties, propertyNames, "type", "separator");
            addProperty(properties, propertyNames, "visible", true);
            return properties;
        }

        if(id == ROOT_ID || id == ADVANCED_ID || id == DIAGNOSTIC_ID) {
            addProperty(properties, propertyNames, "children-display", "submenu");
        }

        String label = getLabel(id);
        if(label != null) {
            addProperty(properties, propertyNames, "label", label);
        }

        if(isCheckbox(id)) {
            // These entries mirror TrayManager's checkbox presentation only.
            // Their live state and click behavior will be supplied by the action facade.
            addProperty(properties, propertyNames, "toggle-type", "checkmark");
            addProperty(properties, propertyNames, "toggle-state", 0);
        }

        addProperty(properties, propertyNames, "enabled", true);
        addProperty(properties, propertyNames, "visible", true);
        return properties;
    }

    private String getLabel(int id) {
        switch(id) {
            case ADVANCED_ID: return "Advanced";
            case DIAGNOSTIC_ID: return "Diagnostic";
            case BROWSE_APP_ID: return "Browse App folder...";
            case BROWSE_USER_ID: return "Browse User folder...";
            case BROWSE_SHARED_ID: return "Browse Shared folder...";
            case NOTIFICATIONS_ID: return "Show all notifications";
            case MONOCLE_ID: return "Use Monocle for HTML";
            case VIEW_LOGS_ID: return "View logs (live feed)...";
            case ZIP_LOGS_ID: return "Zip logs (to Desktop)";
            case SITE_MANAGER_ID: return "Site Manager...";
            case DESKTOP_SHORTCUT_ID: return "Create Desktop shortcut";
            case BLOCK_ANONYMOUS_ID: return "Block anonymous requests";
            case RELOAD_ID: return "Reload";
            case ABOUT_ID: return "About...";
            case AUTOSTART_ID: return "Automatically start";
            case EXIT_ID: return "Exit";
            default: return null;
        }
    }

    private boolean isCheckbox(int id) {
        return id == NOTIFICATIONS_ID
                || id == MONOCLE_ID
                || id == BLOCK_ANONYMOUS_ID
                || id == AUTOSTART_ID;
    }

    private boolean isSeparator(int id) {
        return id == DIAGNOSTIC_OPTIONS_SEPARATOR_ID
                || id == DIAGNOSTIC_LOGS_SEPARATOR_ID
                || id == ADVANCED_DIAGNOSTIC_SEPARATOR_ID
                || id == ADVANCED_OPTIONS_SEPARATOR_ID
                || id == EXIT_SEPARATOR_ID;
    }

    private void addProperty(Map<String, Variant<?>> properties, String[] propertyNames, String name, Object value) {
        if(propertyNames == null || propertyNames.length == 0 || Arrays.asList(propertyNames).contains(name)) {
            properties.put(name, new Variant<>(value));
        }
    }
}
