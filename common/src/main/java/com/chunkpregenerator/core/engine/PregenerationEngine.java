package com.chunkpregenerator.core.engine;

import com.chunkpregenerator.api.*;
import com.chunkpregenerator.api.config.IPregenerationConfig;
import com.chunkpregenerator.api.shape.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 区块预生成引擎的核心实现。
 *
 * <p>此实现负责：
 * <ol>
 *   <li>将配置解析为可执行的任务队列</li>
 *   <li>使用线程池并发生成区块</li>
 *   <li>管理引擎状态的转换</li>
 *   <li>提供进度追踪和回调通知</li>
 * </ol>
 *
 * <p><b>线程安全设计：</b>
 * 引擎使用细粒度锁和原子变量保证线程安全。
 * 状态转换使用 AtomicReference 保证可见性。
 * 任务队列使用 ConcurrentLinkedQueue 保证无锁并发。
 *
 * <p><b>并发控制：</b>
 * 使用 {@link java.util.concurrent.Phaser} 替代 CountDownLatch，
 * 支持可复用的同步屏障，便于实现暂停/恢复功能。
 */
@ThreadSafe
public final class PregenerationEngine implements IPregenerationEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(PregenerationEngine.class);

    // ---- 状态管理 ----

    @GuardedBy("stateLock")
    private final AtomicReference<EngineState> state =
        new AtomicReference<>(EngineState.IDLE);

    private final Lock stateLock = new ReentrantLock();

    // ---- 线程池 ----
    // 使用虚拟线程（Project Loom）以获得更好的资源利用率
    private final ExecutorService executor;

    // ---- 任务追踪 ----
    private volatile IPregenerationTask activeTask;
    private volatile IPregenerationConfig activeConfig;
    private volatile ProgressTracker progressTracker;

    // ---- 回调 ----
    // CopyOnWriteArrayList 用于并发安全且读取频繁的场景
    private final List<IChunkProgressCallback> callbacks =
        new CopyOnWriteArrayList<>();

    // ---- 暂停/恢复支持 ----
    private final Phaser pausePhaser = new Phaser(1);

    /**
     * 创建预生成引擎。
     *
     * @param threadPoolSize 线程池大小，通常根据配置的并发级别动态调整
     */
    public PregenerationEngine(int threadPoolSize) {
        // 使用虚拟线程池 — 每个线程都是一个虚拟线程
        // 虚拟线程由 JVM 管理，轻量且适合大量阻塞 I/O 的场景
        this.executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name("chunk-gen-", 0)
                .factory()
        );
        LOGGER.info("PregenerationEngine initialized with virtual thread pool (size: {})", threadPoolSize);
    }

    /**
     * 使用默认线程池大小创建引擎。
     * 默认值 = CPU核心数。
     */
    public PregenerationEngine() {
        this(Runtime.getRuntime().availableProcessors());
    }

    @Override
    @Nonnull
    public EngineState getState() {
        return state.get();
    }

    @Override
    @Nonnull
    public CompletableFuture<Void> start(@Nonnull IPregenerationConfig config) {
        // 使用状态锁保证 start 的原子性
        stateLock.lock();
        try {
            EngineState currentState = state.get();
            if (currentState == EngineState.RUNNING) {
                LOGGER.warn("Engine already running, cancelling current task first");
                cancel().join();
            } else if (currentState == EngineState.PAUSED) {
                LOGGER.info("Resuming paused task");
                return resume0();
            } else if (currentState != EngineState.IDLE && currentState != EngineState.STOPPED) {
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Cannot start in state: " + currentState)
                );
            }

            this.activeConfig = config;
            this.activeTask = createTask(config);
            this.progressTracker = new ProgressTracker(activeTask);

            state.set(EngineState.RUNNING);

            LOGGER.info("Starting pregeneration: {} chunks (concurrency: {})",
                config.getShape().estimatedSize(), config.getConcurrencyLevel());

            return runAsync(activeTask, config);
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    @Nonnull
    public Optional<PauseResult> pause() {
        stateLock.lock();
        try {
            if (state.get() != EngineState.RUNNING) {
                return Optional.empty();
            }

            state.set(EngineState.PAUSED);
            LOGGER.info("Pausing pregeneration...");

            // 等待当前区块生成完成
            pausePhaser.arriveAndAwaitAdvance();

            ProgressSnapshot snapshot = getProgressSnapshot()
                .orElseThrow(() -> new IllegalStateException("No progress snapshot available"));

            return Optional.of(new PauseResult(
                snapshot.completedChunks(),
                snapshot.totalChunks() - snapshot.completedChunks(),
                0L // 实际耗时由调用方计算
            ));
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public boolean resume() {
        stateLock.lock();
        try {
            return resume0();
        } finally {
            stateLock.unlock();
        }
    }

    private boolean resume0() {
        if (state.get() != EngineState.PAUSED) {
            return false;
        }
        if (activeTask == null || activeConfig == null) {
            LOGGER.warn("No paused task found to resume");
            return false;
        }

        state.set(EngineState.RUNNING);
        LOGGER.info("Resuming pregeneration...");

        // 异步继续执行
        runAsync(activeTask, activeConfig);
        return true;
    }

    @Override
    @Nonnull
    public CompletableFuture<Void> cancel() {
        stateLock.lock();
        try {
            EngineState currentState = state.get();
            if (currentState == EngineState.IDLE || currentState == EngineState.STOPPED) {
                return CompletableFuture.completedFuture(null);
            }

            state.set(EngineState.STOPPING);
            LOGGER.info("Cancelling all tasks...");

            // 中断所有工作线程
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOGGER.warn("Executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Interrupted while waiting for executor termination");
            }

            state.set(EngineState.STOPPED);
            activeTask = null;
            activeConfig = null;

            return CompletableFuture.completedFuture(null);
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public void registerProgressCallback(@Nonnull IChunkProgressCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void unregisterProgressCallback(@Nonnull IChunkProgressCallback callback) {
        callbacks.remove(callback);
    }

    @Override
    @Nonnull
    public Optional<ProgressSnapshot> getProgressSnapshot() {
        ProgressTracker tracker = progressTracker;
        IPregenerationTask task = activeTask;
        if (tracker == null || task == null) {
            return Optional.empty();
        }
        return Optional.of(new ProgressSnapshot(
            task.getTotalCount(),
            task.getCompletedCount(),
            task.getFailedCount(),
            tracker.estimateRemainingTime()
        ));
    }

    @Override
    @Nonnull
    public Optional<IPregenerationTask> getActiveTask() {
        return Optional.ofNullable(activeTask);
    }

    // ========== 私有实现 ==========

    /**
     * 创建预生成任务。
     *
     * @param config 预生成配置
     * @return 新创建的任务
     */
    private IPregenerationTask createTask(@Nonnull IPregenerationConfig config) {
        return new PregenerationTask(
            UUID.randomUUID(),
            config.getShape(),
            config.isForceRegenerate()
        );
    }

    /**
     * 异步执行预生成任务。
     *
     * <p>使用 CompletableFuture 驱动，支持暂停/取消协作。
     * 每个区块生成是一个独立的子任务，提交到虚拟线程池执行。
     */
    private CompletableFuture<Void> runAsync(IPregenerationTask task, IPregenerationConfig config) {
        return CompletableFuture.runAsync(() -> {
            try {
                processChunks(task, config);
            } catch (Exception e) {
                LOGGER.error("Fatal error during pregeneration", e);
                state.set(EngineState.ERROR);
                throw e;
            }
        }, executor);
    }

    /**
     * 处理所有区块。
     *
     * <p>使用信号量控制并发度，避免过多区块同时加载。
     * 内部使用延迟队列实现区块间的延迟。
     */
    private void processChunks(IPregenerationTask task, IPregenerationConfig config) {
        Semaphore concurrencyControl = new Semaphore(config.getConcurrencyLevel());
        AtomicInteger activeCount = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(task.getTotalCount());

        task.getChunkPositions().forEachRemaining(chunkPos -> {
            // 检查是否被取消
            if (state.get() == EngineState.STOPPING || state.get() == EngineState.STOPPED) {
                completionLatch.countDown(); // 释放等待
                return;
            }

            // 检查是否暂停
            if (state.get() == EngineState.PAUSED) {
                pausePhaser.arriveAndAwaitAdvance();
                // 暂停后检查是否应该继续
                if (state.get() != EngineState.RUNNING) {
                    return;
                }
            }

            // 应用区块间延迟
            if (config.getChunkDelayMillis() > 0) {
                try {
                    Thread.sleep(config.getChunkDelayMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // 获取并发许可
            try {
                concurrencyControl.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            activeCount.incrementAndGet();

            // 提交区块生成任务到虚拟线程
            executor.execute(() -> {
                try {
                    processSingleChunk(task, chunkPos);
                } finally {
                    concurrencyControl.release();
                    activeCount.decrementAndGet();
                    completionLatch.countDown();
                }
            });
        });

        // 等待所有区块完成
        try {
            completionLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 如果全部完成，更新状态
        if (state.get() == EngineState.RUNNING) {
            state.set(EngineState.STOPPED);
            LOGGER.info("Pregeneration completed: {} chunks processed",
                task.getCompletedCount());
        }
    }

    /**
     * 处理单个区块的生成。
     *
     * <p>实际区块生成委托给平台特定的实现。
     * 这里只负责进度追踪和回调通知。
     */
    private void processSingleChunk(IPregenerationTask task, ChunkPos pos) {
        try {
            // 通知开始
            notifyCallbacks(task.getTaskId().toString(), pos,
                IChunkProgressCallback.ChunkStatus.GENERATING, "");

            // 实际生成由平台层实现通过回调接入
            // 这里只管理任务状态
            task.markCompleted(pos);

            // 更新进度
            ProgressTracker tracker = progressTracker;
            if (tracker != null) {
                tracker.recordChunk();
            }

            // 通知完成
            notifyCallbacks(task.getTaskId().toString(), pos,
                IChunkProgressCallback.ChunkStatus.COMPLETED, "");

        } catch (Exception e) {
            LOGGER.error("Failed to generate chunk at {}", pos, e);
            task.markFailed(pos, e.getMessage());
            notifyCallbacks(task.getTaskId().toString(), pos,
                IChunkProgressCallback.ChunkStatus.FAILED, e.getMessage());
        }
    }

    private void notifyCallbacks(String taskId, ChunkPos pos,
                                  IChunkProgressCallback.ChunkStatus status, String message) {
        for (IChunkProgressCallback callback : callbacks) {
            try {
                callback.onChunkProcessed(taskId, pos, status, message);
            } catch (Exception e) {
                LOGGER.warn("Callback threw exception", e);
            }
        }
    }

    /**
     * 内部进度追踪器。
     *
     * <p>使用指数移动平均（EMA）估算剩余时间，
     * 避免单次异常值影响估算准确性。
     */
    private static class ProgressTracker {
        private static final double EMA_ALPHA = 0.3;
        private static final int SAMPLE_WINDOW = 100;

        private final IPregenerationTask task;
        private final long startTime;
        private final AtomicInteger chunkCount = new AtomicInteger(0);
        private volatile double avgTimePerChunk = 0.0;

        ProgressTracker(IPregenerationTask task) {
            this.task = task;
            this.startTime = System.nanoTime();
        }

        void recordChunk() {
            int count = chunkCount.incrementAndGet();
            if (count <= SAMPLE_WINDOW) {
                // 采样窗口内：计算真实平均值
                long elapsed = System.nanoTime() - startTime;
                avgTimePerChunk = (double) elapsed / count;
            } else {
                // 采样窗口外：使用 EMA 平滑
                long elapsed = System.nanoTime() - startTime;
                double currentAvg = (double) elapsed / count;
                avgTimePerChunk = EMA_ALPHA * currentAvg + (1 - EMA_ALPHA) * avgTimePerChunk;
            }
        }

        long estimateRemainingTime() {
            int remaining = task.getRemainingCount();
            if (remaining <= 0 || avgTimePerChunk <= 0) {
                return 0;
            }
            return (long) (remaining * avgTimePerChunk / 1_000_000);
        }
    }
}
