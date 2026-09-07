package qz.utils.linux.wm;

import static qz.utils.linux.wm.ExecutorList.*;

public class Mate extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "^.*?(\\d+(?:\\.\\d+)?).*$",
                   "gsettings", "get", "org.mate.interface", "window-scaling-factor");
    }
}
