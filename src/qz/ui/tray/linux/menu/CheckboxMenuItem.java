package qz.ui.tray.linux.menu;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

 class CheckboxMenuItem implements MenuNode {

    private final int id;
    private final String label;
     private final BooleanSupplier checked;
     private final Consumer<Boolean> action;
     private final BooleanSupplier enabled;

     CheckboxMenuItem(int id, String label, BooleanSupplier checked, Consumer<Boolean> action, BooleanSupplier enabled) {
        this.id = id;
        this.label = label;
        this.checked = checked;
         this.action = action;
         this.enabled = enabled;
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
         return checked.getAsBoolean();
     }

     boolean isEnabled() {
         return enabled.getAsBoolean();
    }

     void activate() {
         if (isEnabled()) {
             action.accept(!isChecked());
         }
     }
 }