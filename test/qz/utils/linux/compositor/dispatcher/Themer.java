package qz.utils.linux.compositor.dispatcher;

/**
 * Interface and utility class for getting and setting themes
 */
public interface Themer {
    String getThemeName(boolean isDark, Executor executor);

    default void setTheme(boolean isDark) {
        for(Executor executor : ExecutorCache.SET_THEME.getExecutors()) {
            executor.executeWithParam(getThemeName(isDark, executor));
        }
    }

    default void setTheme(String themeName) {
        for(Executor executor : ExecutorCache.SET_THEME.getExecutors()) {
            executor.executeWithParam(themeName);
        }
    }

    default Dispatcher dispatcher() {
        if(this instanceof Dispatcher) {
            return ((Dispatcher)this);
        }
        throw new UnsupportedOperationException("Themer not instance of Dispatcher");
    }
}
