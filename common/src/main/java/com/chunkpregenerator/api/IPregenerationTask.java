package com.chunkpregenerator.api;

import com.chunkpregenerator.api.shape.ChunkPos;
import com.chunkpregenerator.api.shape.IShapeProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.UUID;

/**
 * 单个预生成任务。
 *
 * <p>代表一个可独立调度和追踪的区块预生成工作单元。
 * 任务包含了要生成的所有区块位置、当前进度和状态。
 * 每个任务有唯一 ID，可用于暂停恢复后的任务追踪。
 *
 * <p><b>设计考量：</b>任务不直接持有引擎引用，而是通过回调
 * 与引擎通信。这使得任务可序列化，支持断点续传。
 */
public interface IPregenerationTask {

    /**
     * 获取任务的唯一标识符。
     *
     * @return UUID，在保存/恢复时用于匹配任务
     */
    @Nonnull
    UUID getTaskId();

    /**
     * 获取任务中所有待生成的区块位置迭代器。
     *
     * <p>迭代器应支持跳过已生成的区块（调用 {@link #markCompleted(ChunkPos)} 后）。
     * 迭代器不保证线程安全，外部需同步。
     *
     * @return 区块位置迭代器
     */
    @Nonnull
    Iterator<ChunkPos> getChunkPositions();

    /**
     * 标记指定区块为已完成。
     *
     * @param pos 区块位置
     */
    void markCompleted(@Nonnull ChunkPos pos);

    /**
     * 标记指定区块为失败。
     *
     * @param pos 区块位置
     * @param reason 失败原因（可选）
     */
    void markFailed(@Nonnull ChunkPos pos, @Nullable String reason);

    /**
     * 检查指定区块是否已完成。
     *
     * @param pos 区块位置
     * @return true 如果该区块已完成或失败
     */
    boolean isCompleted(@Nonnull ChunkPos pos);

    /**
     * 获取已完成区块数量。
     */
    int getCompletedCount();

    /**
     * 获取失败区块数量。
     */
    int getFailedCount();

    /**
     * 获取总区块数量。
     */
    int getTotalCount();

    /**
     * 获取剩余区块数量（估算）。
     */
    int getRemainingCount();

    /**
     * 获取任务关联的形状提供者。
     */
    @Nonnull
    IShapeProvider getShapeProvider();

    /**
     * 获取任务状态。
     */
    @Nonnull
    TaskState getState();

    /**
     * 获取任务创建的毫秒时间戳。
     */
    long getCreatedAt();

    /**
     * 任务状态枚举。
     */
    enum TaskState {
        PENDING,
        RUNNING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
