package uk.co.hsim.assetaudit.app;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private final Executor diskIO;
    private final Executor computation;
    private final Handler mainHandler;

    public AppExecutors() {
        this(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(2), new Handler(Looper.getMainLooper()));
    }

    public AppExecutors(Executor diskIO, Executor computation, Handler mainHandler) {
        this.diskIO = diskIO;
        this.computation = computation;
        this.mainHandler = mainHandler;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor computation() {
        return computation;
    }

    public void mainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
