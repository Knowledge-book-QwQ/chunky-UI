package com.chunkpregenerator.api;

import com.chunkpregenerator.api.config.IPregenerationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 区块预生成引擎核心接口。
 *
 * <p>这是整个模组的入口点。引擎负责管理所有预生成任务的
 * 生命周期，包括调度、执行、暂停、恢复和取消。
 *
 * <p><b>线程安全：</b>所有实现必须是线程安全的。
 * 引擎内部应使用独立的线程池处理区块生成，避免阻塞游戏主线程。
 *
 * <p>使用示例：
 * <pre>{@code
 * IPregenerationEngine engine = PregenerationEngineProvider.get();
 * engine.start(config)
 *      .thenRun(() -> LOGGER.info("预生成完成"))
 *      .exceptionally(ex -> { LOGGER.error("预生成失败", ex); return null; });
 * }</pre>
 */
public interface IPregenerationEngine {

    /**
     * 日志记录器，子类可直接使用。
     */
    Logger LOGGER = LoggerFactory.getLogger(IPregenerationEngine.class);

    /**
     * 获取引擎的当前状态。
     *
     * @return 当前状态，不会为 null
     */
    @Nonnull
    EngineState getState();

    /**
     * 使用给定配置启动区块预生成。
     *
     * <p>如果引擎正在运行，此调用会先优雅停止当前任务再启动新任务。
     * 引擎会校验配置的有效性，无效配置会立即返回失败的 CompletableFuture。
     *
     * @param config 预生成配置，包含区域、形状、并发度等信息
     * @return CompletableFuture，成功时返回 {@code Void}，失败时携带异常
     * @throws NullPointerException 如果 config 为 null
     */
    @Nonnull
    CompletableFuture<Void> start(@Nonnull IPregenerationConfig config);

    /**
     * 暂停当前正在运行的预生成任务。
     *
     * <p>暂停后，引擎会等待当前正在生成的区块完成，
     * 然后将未完成的区块队列持久化到磁盘。
     * 调用 {@link #resume()} 可恢复。
     *
     * @return Optional.empty() 如果没有任务在运行
     */
    @Nonnull
    Optional<PauseResult> pause();

    /**
     * 恢复之前暂停的预生成任务。
     *
     * @return true 如果成功恢复，false 如果没有暂停的任务
     */
    boolean resume();

    /**
     * 取消所有正在运行的预生成任务并清理资源。
     *
     * <p>取消操作不可逆。已生成的区块不会被删除。
     * 引擎会发送中断信号到工作线程，但不保证立即停止。
     *
     * @return CompletableFuture，在完全停止后完成
     */
    @Nonnull
    CompletableFuture<Void> cancel();

    /**
     * 注册进度回调。
     *
     * <p>回调会在引擎工作线程中调用，实现必须保证回调的线程安全性。
     * 推荐回调中只做轻量操作（如更新UI），复杂逻辑请异步处理。
     *
     * @param callback 进度回调
     * @throws NullPointerException 如果 callback 为 null
     */
    void registerProgressCallback(@Nonnull IChunkProgressCallback callback);

    /**
     * 移除之前注册的进度回调。
     *
     * @param callback 要移除的回调
     */
    void unregisterProgressCallback(@Nonnull IChunkProgressCallback callback);

    /**
     * 获取当前进度信息的快照。
     *
     * @return 包含当前进度的 Optional（如果没有活动任务则为空）
     */
    @Nonnull
    Optional<ProgressSnapshot> getProgressSnapshot();

    /**
     * 获取当前活跃的预生成任务。
     *
     * @return 活跃任务（如果引擎正在运行）
     */
    @Nonnull
    Optional<IPregenerationTask> getActiveTask();

    /**
     * 引擎状态枚举。
     */
    enum EngineState {
        /** 引擎已创建，尚未开始任何任务 */
        IDLE,
        /** 正在运行预生成任务 */
        RUNNING,
        /** 已暂停 */
        PAUSED,
        /** 正在停止过程中 */
        STOPPING,
        /** 已停止 */
        STOPPED,
        /** 发生了不可恢复的错误 */
        ERROR
    }

    /**
     * 暂停操作的返回值。
     */
    final class PauseResult {
        private final int completedChunks;
        private final int remainingChunks;
        private final long elapsedMillis;

        public PauseResult(int completedChunks, int remainingChunks, long elapsedMillis) {
            this.completedChunks = completedChunks;
            this.remainingChunks = remainingChunks;
            this.elapsedMillis = elapsedMillis;
        }

        public int completedChunks() { return completedChunks; }
        public int remainingChunks() { return remainingChunks; }
        public long elapsedMillis() { return elapsedMillis; }
    }

    /**
     * 进度快照。
     */
    final class ProgressSnapshot {
        private final int totalChunks;
        private final int completedChunks;
        private final int failedChunks;
        private final double percentage;
        private final long estimatedMillisRemaining;

        public ProgressSnapshot(int totalChunks, int completedChunks,
                                 int failedChunks, long estimatedMillisRemaining) {
            this.totalChunks = totalChunks;
            this.completedChunks = completedChunks;
            this.failedChunks = failedChunks;
            this.percentage = totalChunks > 0
                ? (double) completedChunks / totalChunks * 100.0
                : 0.0;
            this.estimatedMillisRemaining = estimatedMillisRemaining;
        }

        public int totalChunks() { return totalChunks; }
        public int completedChunks() { return completedChunks; }
        public int failedChunks() { return failedChunks; }
        public double percentage() { return percentage; }
        public long estimatedMillisRemaining() { return estimatedMillisRemaining; }
    }
}
