package qz.utils.linux.wm;

import java.util.ArrayList;
import java.util.List;

public enum ExecutorList {
    DARK_MODE,
    SCALE_FACTOR,
    DPI,
    GET_THEME, // unit tests only
    SET_THEME; // unit tests only

    final List<Executor> executorList;
    Executor executor;

    ExecutorList() {
        executorList = new ArrayList<>();
        executor = null;
    }

    void add(Executor executor) {
        executorList.add(executor);
    }

    Executor getExecutor() {
        if(executor != null) {
            return executor;
        }

        for(Executor executor : executorList) {
            if(executor.getString() != null) {
                return this.executor = executor;
            }
        }
        return null;
    }

    List<Executor> getExecutors() {
        return executorList;
    }
}

