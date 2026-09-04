package qz.utils.linux.wm.dispatcher;

import static qz.utils.linux.wm.dispatcher.ExecutorCache.*;

public class Hyprland extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "(?s)Monitor\\s+.*?\\bscale:\\s*([0-9.]+)",
                   "hyprctl", "monitors");
    }
}
