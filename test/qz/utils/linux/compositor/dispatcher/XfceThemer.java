package qz.utils.linux.compositor.dispatcher;

public class XfceThemer extends Xfce implements Themer {
    @Override
    protected void addMatchers() {
        super.addMatchers();

        addMatcher(ExecutorCache.GET_THEME, "([A-Za-z0-9_-]+)",
                   "xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName");

        addMatcher(ExecutorCache.SET_THEME, ".*",
                   "xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName", "-s");
    }

    @Override
    public String getThemeName(boolean isDark, Executor unused) {
        return isDark ? "Greybird-dark" : "Greybird";
    }
}
