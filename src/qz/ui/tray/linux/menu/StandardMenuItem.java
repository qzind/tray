package qz.ui.tray.linux.menu;

import java.util.List;
import java.util.function.BooleanSupplier;

 class StandardMenuItem implements MenuNode {

    private static final Runnable NO_ACTION = () -> {
    };
    private static final BooleanSupplier ENABLED = () -> true;

    private final int id;
    private final String label;
    private final Runnable action;
    private final BooleanSupplier enabled;
    private final List<MenuNode> children;

    private StandardMenuItem(int id, String label, Runnable action, BooleanSupplier enabled, List<MenuNode> children) {
        this.id = id;
        this.label = label;
        this.action = action;
        this.enabled = enabled;
        this.children = List.copyOf(children);
    }

     static StandardMenuItem item(int id, String label, Runnable action) {
        return new StandardMenuItem(id, label, action, ENABLED, List.of());
    }

     static StandardMenuItem submenu(int id, String label, List<MenuNode> children) {
        return new StandardMenuItem(id, label, NO_ACTION, ENABLED, children);
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

     boolean isEnabled() {
        return enabled.getAsBoolean();
    }

     void activate() {
        if(isEnabled()) {
            action.run();
        }
    }
}
