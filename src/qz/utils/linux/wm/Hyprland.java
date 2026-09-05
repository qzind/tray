package qz.utils.linux.wm;

import static qz.utils.linux.wm.ExecutorList.*;

public class Hyprland extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "(?s)Monitor\\s+.*?\\bscale:\\s*([0-9.]+)",
                   "hyprctl", "monitors");
    }
}
