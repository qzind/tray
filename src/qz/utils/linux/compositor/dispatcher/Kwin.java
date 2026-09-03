package qz.utils.linux.compositor.dispatcher;

import static qz.utils.linux.compositor.dispatcher.ExecutorCache.*;

public class Kwin extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR,"^.*?(\\d+(?:\\.\\d+)?).*$",
              "kreadconfig6", "--file", "kdeglobals", "--group", "KScreen", "--key", "ScaleFactor");

        addMatcher(SCALE_FACTOR,"^.*?(\\d+(?:\\.\\d+)?).*$",
                      "kreadconfig5", "--file", "kdeglobals", "--group", "KScreen", "--key", "ScaleFactor");

        // Debian KDE seems to set this
        addMatcher(SCALE_FACTOR, "(?:uint32\\s+)?(\\d+(?:\\.\\d+)?)",
                   "gsettings", "get", "org.gnome.desktop.interface", "scaling-factor");

        addMatcher(DARK_MODE, "([A-Za-z0-9_-]+)",
                   "kreadconfig6", "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme");

        addMatcher(DARK_MODE, "([A-Za-z0-9_-]+)",
                   "kreadconfig5", "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme");
    }
}
