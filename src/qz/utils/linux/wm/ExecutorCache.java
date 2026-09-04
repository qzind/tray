package qz.utils.linux.wm;

import java.util.ArrayList;
import java.util.List;

public enum ExecutorCache {
    DARK_MODE,
    SCALE_FACTOR,
    DPI,
    GET_THEME, // unit tests only
    SET_THEME; // unit tests only

    final List<Executor> executorList;
    Executor executor;

    ExecutorCache() {
        executorList = new ArrayList<>();
        executor = null;
    }

    void add(Executor match) {
        executorList.add(match);
    }

    Executor getExecutor() {
        if(executor != null) {
            return executor;
        }

        for(Executor match : executorList) {
            if(match.getString() != null) {
                return executor = match;
            }
        }
        return null;
    }

    List<Executor> getExecutors() {
        return executorList;
    }
}

