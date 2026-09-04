package qz.utils.linux.wm.dispatcher;

import static qz.utils.linux.wm.dispatcher.ExecutorCache.*;

public class Xfce extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "<(\\d+)>", "gdbus", "call", "--session",
                   "--dest", "org.xfce.Xfconf",
                   "--object-path", "/org/xfce/Xfconf",
                   "--method", "org.xfce.Xfconf.GetProperty",
                   "xsettings", "/Gdk/WindowScalingFactor");

        addMatcher(SCALE_FACTOR, "^.*?(\\d+(?:\\.\\d+)?).*$",
                   "xfconf-query", "-c", "xsettings", "-p", "/Gdk/WindowScalingFactor");

        addMatcher(DARK_MODE, "([A-Za-z0-9_-]+)", "xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName");
    }
}
