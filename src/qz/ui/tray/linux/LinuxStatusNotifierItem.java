package qz.ui.tray.linux;

public class LinuxStatusNotifierItem implements StatusNotifierItem {

    private static final String OBJECT_PATH = "/StatusNotifierItem";

    @Override
    public String getObjectPath() {
        return OBJECT_PATH;
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
