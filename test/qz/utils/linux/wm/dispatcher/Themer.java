package qz.utils.linux.wm.dispatcher;

import static qz.utils.linux.wm.dispatcher.ExecutorCache.GET_THEME;

/**
 * Interface and utility class for getting and setting themes
 */
public interface Themer {
    String getThemeName(boolean isDark, Executor executor);

    default String getTheme() {
        Executor executor = GET_THEME.getExecutor();
        if(executor != null) {
            String themeName = executor.getString();
            if (themeName != null) {
                return themeName;
            }
        }
        return null;
    }

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
}
