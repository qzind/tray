package qz.utils.linux.compositor.dispatcher;

import static qz.utils.linux.compositor.dispatcher.ExecutorCache.*;

public class Hyprland extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "(?s)Monitor\\s+.*?\\bscale:\\s*([0-9.]+)",
                   "hyprctl", "monitors");
    }
}
