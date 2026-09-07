package qz.utils.linux.wm;

import static qz.utils.linux.wm.ExecutorList.GET_THEME;

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

    default boolean setTheme(boolean isDark) {
        boolean success = false;
        for(Executor executor : ExecutorList.SET_THEME.getExecutors()) {
            if(executor.executeWithParam(getThemeName(isDark, executor))) {
                success = true;
            }
        }
        return success;
    }

    default boolean setTheme(String themeName) {
        boolean success = false;
        for(Executor executor : ExecutorList.SET_THEME.getExecutors()) {
            if(executor.executeWithParam(themeName)) {
                success = true;
            }
        }
        return success;
    }
}
