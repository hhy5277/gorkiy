package io.fabric.sdk.android.services.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PriorityThreadPoolExecutor extends ThreadPoolExecutor {
    private static final int CORE_POOL_SIZE;
    private static final int CPU_COUNT;
    private static final long KEEP_ALIVE = 1;
    private static final int MAXIMUM_POOL_SIZE;

    static {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        CPU_COUNT = availableProcessors;
        CORE_POOL_SIZE = availableProcessors + 1;
        MAXIMUM_POOL_SIZE = (availableProcessors * 2) + 1;
    }

    <T extends Runnable & Dependency & Task & PriorityProvider> PriorityThreadPoolExecutor(int i, int i2, long j, TimeUnit timeUnit, DependencyPriorityBlockingQueue<T> dependencyPriorityBlockingQueue, ThreadFactory threadFactory) {
        super(i, i2, j, timeUnit, dependencyPriorityBlockingQueue, threadFactory);
        prestartAllCoreThreads();
    }

    public static <T extends Runnable & Dependency & Task & PriorityProvider> PriorityThreadPoolExecutor create(int i, int i2) {
        return new PriorityThreadPoolExecutor(i, i2, 1, TimeUnit.SECONDS, new DependencyPriorityBlockingQueue(), new PriorityThreadFactory(10));
    }

    public static PriorityThreadPoolExecutor create(int i) {
        return create(i, i);
    }

    public static PriorityThreadPoolExecutor create() {
        return create(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE);
    }

    /* access modifiers changed from: protected */
    public <T> RunnableFuture<T> newTaskFor(Runnable runnable, T t) {
        return new PriorityFutureTask(runnable, t);
    }

    /* access modifiers changed from: protected */
    public <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new PriorityFutureTask(callable);
    }

    public void execute(Runnable runnable) {
        if (PriorityTask.isProperDelegate(runnable)) {
            super.execute(runnable);
        } else {
            super.execute(newTaskFor(runnable, null));
        }
    }

    /* access modifiers changed from: protected */
    public void afterExecute(Runnable runnable, Throwable th) {
        Task task = (Task) runnable;
        task.setFinished(true);
        task.setError(th);
        getQueue().recycleBlockedQueue();
        super.afterExecute(runnable, th);
    }

    public DependencyPriorityBlockingQueue getQueue() {
        return (DependencyPriorityBlockingQueue) super.getQueue();
    }

    protected static final class PriorityThreadFactory implements ThreadFactory {
        private final int threadPriority;

        public PriorityThreadFactory(int i) {
            this.threadPriority = i;
        }

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setPriority(this.threadPriority);
            thread.setName("Queue");
            return thread;
        }
    }
}
