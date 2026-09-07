package qz.utils.linux.wm;

import static qz.utils.linux.wm.ExecutorList.*;

public class Kwin extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "Enabled:\\s*1.*?Scale:\\s*(\\d+(?:\\.\\d+)?)",
                   "gdbus", "call", "--session",
                   "--dest" , "org.kde.KWin",
                   "--object-path", "/KWin",
                   "--method", "org.kde.KWin.supportInformation");

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
