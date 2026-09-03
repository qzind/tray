package qz.utils.linux.compositor.dispatcher;

import static qz.utils.linux.compositor.dispatcher.ExecutorCache.*;

public class Mate extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "^.*?(\\d+(?:\\.\\d+)?).*$",
                   "gsettings", "get", "org.mate.interface", "window-scaling-factor");
    }
}
