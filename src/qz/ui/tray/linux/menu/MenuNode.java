package qz.ui.tray.linux.menu;

import java.util.List;

interface MenuNode {

    int getId();

    List<MenuNode> getChildren();
}