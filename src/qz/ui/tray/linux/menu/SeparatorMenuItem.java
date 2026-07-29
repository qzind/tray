package qz.ui.tray.linux.menu;

import java.util.List;

 class SeparatorMenuItem implements MenuNode {

    private final int id;

    SeparatorMenuItem(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public List<MenuNode> getChildren() {
        return List.of();
    }
}