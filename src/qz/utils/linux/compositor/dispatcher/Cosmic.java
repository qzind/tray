package qz.utils.linux.compositor.dispatcher;

import static qz.utils.linux.compositor.dispatcher.ExecutorCache.*;

public class Cosmic extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "output\\s+\"[^\"]+\"\\s+enabled=#true\\b.*?\\bscale\\s+(\\d+(?:\\.\\d+)?)",
                   "cosmic-randr", "list", "--kdl");
    }
}
