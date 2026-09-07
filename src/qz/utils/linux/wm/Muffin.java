package qz.utils.linux.wm;

import static qz.utils.linux.wm.ExecutorList.*;

public class Muffin extends Dispatcher {
    @Override
    protected void addMatchers() {
        addMatcher(SCALE_FACTOR, "\\(\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*(\\d+(?:\\.\\d+)?)\\s*,\\s*uint32\\s+\\d+\\s*,\\s*true\\b",
                   "gdbus", "call", "--session",
                   "--dest", "org.cinnamon.Muffin.DisplayConfig",
                   "--object-path", "/org/cinnamon/Muffin/DisplayConfig",
                   "--method", "org.cinnamon.Muffin.DisplayConfig.GetCurrentState");
    }
}
