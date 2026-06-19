package qz.ui.tray.linux;

import org.freedesktop.dbus.DBusPath;

public class LinuxStatusNotifierItem implements KdeStatusNotifierItem, FreedesktopStatusNotifierItem {

    private static final String OBJECT_PATH = "/StatusNotifierItem";
    private static final DBusPath MENU_PATH = new DBusPath("/MenuBar");
    private static final LinuxSniPixmap[] NO_ICON_PIXMAPS = new LinuxSniPixmap[0];
    private static final String CATEGORY = "ApplicationStatus";
    private static final String ID = "qz-tray";
    private static final String TITLE = "QZ Tray";
    private static final String STATUS = "Active";
    // This must match the generated icon theme name exactly
    private static final String THEMED_ICON_NAME = "kdeconnectindicatordark";

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

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getStatus() {
        return STATUS;
    }

    @Override
    public String getIconName() {
        return iconName;
    }

    @Override
    public LinuxSniPixmap[] getIconPixmap() {
        // IconName remains authoritative while satisfying hosts that query both properties
        return NO_ICON_PIXMAPS;
    }

    @Override
    public String getIconThemePath() {
        return iconThemePath;
    }

    @Override
    public boolean isMenu() {
        // QZ exposes tray actions rather than a primary activation target
        return true;
    }

    @Override
    public DBusPath getMenu() {
        // Without this, Ubuntu GNOME dropped the registered item until it received
        // a non-empty Menu object path before it would consider the tray item as ready
        // for display. The POC exports a minimal com.canonical.dbusmenu object at
        // this path before registering the item.
        return MENU_PATH;
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
}
