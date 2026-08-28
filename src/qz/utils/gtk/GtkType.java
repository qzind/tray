package qz.utils.gtk;

enum GtkType {
    // GTK4("gtk-4", Gtk4.class),
    GTK3("gtk-3", Gtk3.class),
    GTK2("gtk-x11-2.0", Gtk2.class);

    final String lib;
    final Class<? extends Gtk> type;

    GtkType(String lib, Class<? extends Gtk> type) {
        this.lib = lib;
        this.type = type;
    }
}
