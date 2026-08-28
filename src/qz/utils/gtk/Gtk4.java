package qz.utils.gtk;

import com.sun.jna.Pointer;

public interface Gtk4 extends Gtk3 {
    Pointer gdk_display_get_monitors(Pointer display);

    Pointer g_list_model_get_item(Pointer listModel, int position);

    @Override
    default Pointer getMonitor() {
        Pointer display = gdk_display_get_default();
        if (display != null) {
            Pointer monitors = gdk_display_get_monitors(display);
            if (monitors != null) {
                return g_list_model_get_item(monitors, 0);
            }
        }
        return null;
    }
}
