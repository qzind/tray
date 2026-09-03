package qz.utils.linux.compositor.dispatcher;

import static qz.utils.linux.compositor.dispatcher.ExecutorCache.*;

public class Mutter extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*uint32\\s+\\d+\\s*,\\s*true\\b",
                   "gdbus", "call", "--session",
                   "--dest", "org.gnome.Mutter.DisplayConfig",
                   "--object-path", "/org/gnome/Mutter/DisplayConfig",
                   "--method", "org.gnome.Mutter.DisplayConfig.GetCurrentState");

        // Broken as of Ubuntu 24.04, keep for legacy fallback
        addMatcher(SCALE_FACTOR, "(?:uint32\\s+)?(\\d+(?:\\.\\d+)?)",
                   "gsettings", "get", "org.gnome.desktop.interface", "scaling-factor");

        addMatcher(DARK_MODE, "([A-Za-z0-9_-]+)",
                   "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme");
    }
}
