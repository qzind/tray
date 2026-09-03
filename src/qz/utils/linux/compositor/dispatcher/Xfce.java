package qz.utils.linux.compositor.dispatcher;

import static qz.utils.linux.compositor.dispatcher.ExecutorCache.*;

public class Xfce extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "^.*?(\\d+(?:\\.\\d+)?).*$",
                   "xfconf-query", "-c", "xsettings", "-p", "/Gdk/WindowScalingFactor");

        addMatcher(DARK_MODE, "([A-Za-z0-9_-]+)", "xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName");
    }
}
