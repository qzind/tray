package qz.ui.tray.linux.menu;

import java.util.List;

 class CheckboxMenuItem implements MenuNode {

    private final int id;
    private final String label;
    private final boolean checked;

    CheckboxMenuItem(int id, String label, boolean checked) {
        this.id = id;
        this.label = label;
        this.checked = checked;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public List<MenuNode> getChildren() {
        return List.of();
    }

    String getLabel() {
        return label;
    }

     boolean isChecked() {
        return checked;
    }
}