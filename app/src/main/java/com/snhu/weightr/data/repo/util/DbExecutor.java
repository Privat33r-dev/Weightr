package com.snhu.weightr.data.repo.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared thread manager for DB operations. */
public final class DbExecutor {
    private static final int NUMBER_OF_THREADS = 4;
    private static final ExecutorService INSTANCE =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static ExecutorService get() {
        return INSTANCE;
    }
}