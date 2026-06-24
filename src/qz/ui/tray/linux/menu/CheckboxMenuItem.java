package qz.ui.tray.linux.menu;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

 class CheckboxMenuItem implements MenuNode {

    private final int id;
    private final String label;
     private final BooleanSupplier checked;
     // The completion callback runs after the requested state change finishes
     private final BiConsumer<Boolean, Runnable> action;
     private final BooleanSupplier enabled;

     CheckboxMenuItem(int id, String label, BooleanSupplier checked, BiConsumer<Boolean, Runnable> action,
                      BooleanSupplier enabled) {
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

     void activate(Runnable completion) {
         if (isEnabled()) {
             // Request the inverse of the current authoritative state
             action.accept(!isChecked(), completion);
         }
     }
 }
