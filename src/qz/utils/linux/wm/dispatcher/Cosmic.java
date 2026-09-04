package qz.utils.linux.wm.dispatcher;

import static qz.utils.linux.wm.dispatcher.ExecutorCache.*;

public class Cosmic extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "output\\s+\"[^\"]+\"\\s+enabled=#true\\b.*?\\bscale\\s+(\\d+(?:\\.\\d+)?)",
                   "cosmic-randr", "list", "--kdl");
    }
}
