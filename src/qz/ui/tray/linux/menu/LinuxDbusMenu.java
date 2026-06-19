package qz.ui.tray.linux.menu;

import com.github.zafarkhaja.semver.Version;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import qz.common.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class LinuxDbusMenu implements CanonicalDbusMenu {

    private static final String OBJECT_PATH = "/MenuBar";
    private static final int ROOT_ID = 0;
    // DBusMenu protocol/interface version exposed through the Version property
    private static final UInt32 VERSION = new UInt32(3);
    // Layout revision returned by GetLayout
    // This menu is static, so revision
    // 1 is enough until menu items or properties become dynamic
    private static final UInt32 REVISION = new UInt32(1);
    private static final String STATUS = "normal";
    private static final String EVENT_CLICKED = "clicked";

    private final Map<Integer, MenuNode> nodes = new LinkedHashMap<>();
    private int nextItemId = 1;

    public LinuxDbusMenu(LinuxDbusMenuActions actions) {
        List<MenuNode> diagnosticChildren = new ArrayList<>(Arrays.asList(
                item("Browse App folder...", actions::browseAppDirectory),
                item("Browse User folder...", actions::browseUserDirectory),
                item("Browse Shared folder...", actions::browseSharedDirectory),
                separator(),
                checkbox("Show all notifications", actions::areNotificationsEnabled,
                        actions::setNotificationsEnabled)
        ));
        if(Constants.JAVA_VERSION.greaterThanOrEqualTo(Version.valueOf("11.0.0"))) {
            diagnosticChildren.add(checkbox("Use Monocle for HTML", actions::isMonocleEnabled,
                    actions::setMonocleEnabled));
        }
        diagnosticChildren.add(separator());
        diagnosticChildren.add(item("View logs (live feed)...", actions::showLogs));
        diagnosticChildren.add(item("Zip logs (to Desktop)", actions::zipLogs));

        List<MenuNode> advancedChildren = new ArrayList<>();
        if(Constants.ENABLE_DIAGNOSTICS) {
            advancedChildren.add(submenu("Diagnostic", diagnosticChildren));
            advancedChildren.add(separator());
        }
        advancedChildren.add(item("Site Manager...", actions::showSiteManager));
        advancedChildren.add(item("Create Desktop shortcut", actions::createDesktopShortcut));
        advancedChildren.add(separator());
        advancedChildren.add(checkbox("Block anonymous requests", actions::areAnonymousRequestsBlocked,
                actions::setAnonymousRequestsBlocked));

        MenuNode root = new RootMenuItem(ROOT_ID, Arrays.asList(
                submenu("Advanced", advancedChildren),
                item("Reload", actions::reload),
                item("About...", actions::showAbout),
                checkbox("Automatically start", actions::isAutoStartEnabled,
                        actions::setAutoStartEnabled, actions::canAutoStart),
                separator(),
                item("Exit", actions::exit)
        ));
        index(root);
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
        MenuNode node = nodes.get(id);
        if(!EVENT_CLICKED.equals(eventId)) {
            return;
        }
        if(node instanceof StandardMenuItem) {
            ((StandardMenuItem)node).activate();
        } else if(node instanceof CheckboxMenuItem) {
            ((CheckboxMenuItem)node).activate();
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
        MenuNode node = nodes.get(id);
        if(node == null) {
            return new LinuxDbusMenuLayoutItem(id, new LinkedHashMap<>(), new Variant<?>[0]);
        }

        List<Variant<?>> children = new ArrayList<>();

        if(recursionDepth != 0) {
            int childDepth = recursionDepth < 0 ? recursionDepth : recursionDepth - 1;
            for(MenuNode child : node.getChildren()) {
                children.add(new Variant<>(getItem(child.getId(), childDepth, propertyNames)));
            }
        }

        return new LinuxDbusMenuLayoutItem(
                node.getId(),
                getProperties(node, propertyNames),
                children.toArray(new Variant<?>[0])
        );
    }

    private Map<String, Variant<?>> getProperties(int id, String[] propertyNames) {
        MenuNode node = nodes.get(id);
        return node == null ? new LinkedHashMap<>() : getProperties(node, propertyNames);
    }

    private Map<String, Variant<?>> getProperties(MenuNode node, String[] propertyNames) {
        // DBusMenu item properties are a{sv}, so dbus-java represents each value
        // with Variant<?> while Java keeps the dictionary as a Map
        Map<String, Variant<?>> properties = new LinkedHashMap<>();

        if(node instanceof SeparatorMenuItem) {
            addProperty(properties, propertyNames, "type", "separator");
            addProperty(properties, propertyNames, "visible", true);
            return properties;
        }

        if(!node.getChildren().isEmpty()) {
            addProperty(properties, propertyNames, "children-display", "submenu");
        }

        if(node instanceof StandardMenuItem) {
            StandardMenuItem item = (StandardMenuItem)node;
            addProperty(properties, propertyNames, "label", item.getLabel());
            addProperty(properties, propertyNames, "enabled", item.isEnabled());
        } else if(node instanceof CheckboxMenuItem) {
            CheckboxMenuItem checkbox = (CheckboxMenuItem) node;
            addProperty(properties, propertyNames, "label", checkbox.getLabel());
            addProperty(properties, propertyNames, "toggle-type", "checkmark");
            addProperty(properties, propertyNames, "toggle-state", checkbox.isChecked() ? 1 : 0);
            addProperty(properties, propertyNames, "enabled", checkbox.isEnabled());
        } else {
            addProperty(properties, propertyNames, "enabled", true);
        }

        addProperty(properties, propertyNames, "visible", true);
        return properties;
    }

    private void index(MenuNode node) {
        if(nodes.containsKey(node.getId())) {
            throw new IllegalArgumentException("Duplicate DBusMenu id " + node.getId());
        }
        nodes.put(node.getId(), node);
        for(MenuNode child : node.getChildren()) {
            index(child);
        }
    }

    private int nextId() {
        return nextItemId++;
    }

    private MenuNode item(String label, Runnable action) {
        return StandardMenuItem.item(nextId(), label, action);
    }

    private MenuNode checkbox(String label, BooleanSupplier checked, Consumer<Boolean> action) {
        return checkbox(label, checked, action, () -> true);
    }

    private MenuNode checkbox(String label, BooleanSupplier checked, Consumer<Boolean> action,
                              BooleanSupplier enabled) {
        return new CheckboxMenuItem(nextId(), label, checked, action, enabled);
    }

    private MenuNode separator() {
        return new SeparatorMenuItem(nextId());
    }

    private MenuNode submenu(String label, List<MenuNode> children) {
        return StandardMenuItem.submenu(nextId(), label, children);
    }

    private void addProperty(Map<String, Variant<?>> properties, String[] propertyNames, String name, Object value) {
        if(propertyNames == null || propertyNames.length == 0 || Arrays.asList(propertyNames).contains(name)) {
            properties.put(name, new Variant<>(value));
        }
    }

}
