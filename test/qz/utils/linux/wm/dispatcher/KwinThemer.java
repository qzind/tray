package qz.utils.linux.wm.dispatcher;

public class KwinThemer extends Kwin implements Themer {
    @Override
    protected void addMatchers() {
        super.addMatchers();

        addMatcher(ExecutorCache.GET_THEME, "([A-Za-z0-9_-]+)",
                   "kreadconfig6", "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme");

        addMatcher(ExecutorCache.GET_THEME, "([A-Za-z0-9_-]+)",
                   "kreadconfig5", "--file", "kdeglobals", "--group", "General", "--key", "ColorScheme");

        addMatcher(ExecutorCache.SET_THEME, ".*",
                   "plasma-apply-colorscheme");
    }

    @Override
    public String getThemeName(boolean isDark, Executor unused) {
        return isDark? "BreezeDark":"BreezeLight";
    }
}
