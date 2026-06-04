package qz.ui.tray.linux;

import org.freedesktop.dbus.DBusPath;

public class LinuxStatusNotifierItem implements KdeStatusNotifierItem, FreedesktopStatusNotifierItem {

    private static final String OBJECT_PATH = "/StatusNotifierItem";
    private static final DBusPath MENU_PATH = new DBusPath("/MenuBar");
    private static final String CATEGORY = "ApplicationStatus";
    private static final String ID = "qz-tray";
    private static final String TITLE = "QZ Tray";
    private static final String STATUS = "Active";

    private final String iconName;
    private final String iconThemePath;

    LinuxStatusNotifierItem(String iconName, String iconThemePath) {
        this.iconName = iconName;
        this.iconThemePath = iconThemePath;
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
    public String getIconThemePath() {
        return iconThemePath;
    }

    @Override
    public DBusPath getMenu() {
        // Ubuntu GNOME requires a non-empty Menu object path before
        // it considers a StatusNotifierItem ready for display
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
}
