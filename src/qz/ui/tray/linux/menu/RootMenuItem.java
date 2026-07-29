package qz.ui.tray.linux.menu;

import java.util.List;

 class RootMenuItem implements MenuNode {

    private final int id;
    private final List<MenuNode> children;

    RootMenuItem(int id, List<MenuNode> children) {
        this.id = id;
        this.children = List.copyOf(children);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public List<MenuNode> getChildren() {
        return children;
    }
}