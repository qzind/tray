package qz.ui.tray.linux.menu;

import java.util.List;

 class StandardMenuItem implements MenuNode {

    private static final Runnable NO_ACTION = () -> {
    };

    private final int id;
    private final String label;
    private final Runnable action;
    private final List<MenuNode> children;

    private StandardMenuItem(int id, String label, Runnable action, List<MenuNode> children) {
        this.id = id;
        this.label = label;
        this.action = action;
        this.children = List.copyOf(children);
    }

     static StandardMenuItem item(int id, String label) {
        return new StandardMenuItem(id, label, NO_ACTION, List.of());
    }

     static StandardMenuItem item(int id, String label, Runnable action) {
        return new StandardMenuItem(id, label, action, List.of());
    }

     static StandardMenuItem submenu(int id, String label, List<MenuNode> children) {
        return new StandardMenuItem(id, label, NO_ACTION, children);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public List<MenuNode> getChildren() {
        return children;
    }

     String getLabel() {
        return label;
    }

     void activate() {
        action.run();
    }
}