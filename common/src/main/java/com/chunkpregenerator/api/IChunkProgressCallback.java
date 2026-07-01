package com.chunkpregenerator.api;

import com.chunkpregenerator.api.shape.ChunkPos;

import javax.annotation.Nonnull;

/**
 * 区块进度回调接口。
 *
 * <p>引擎在区块生成完成或失败时调用此接口。
 * 回调在引擎工作线程池中执行，实现必须保证线程安全性。
 *
 * @see IPregenerationEngine#registerProgressCallback(IChunkProgressCallback)
 */
@FunctionalInterface
public interface IChunkProgressCallback {

    /**
     * 当单个区块的处理状态发生变化时调用。
     *
     * @param taskId  所属任务的ID
     * @param pos     区块位置
     * @param status  新的状态
     * @param message 附加信息（可选，如失败原因）
     */
    void onChunkProcessed(@Nonnull String taskId, @Nonnull ChunkPos pos,
                          @Nonnull ChunkStatus status, @Nonnull String message);

    /**
     * 区块处理状态。
     */
    enum ChunkStatus {
        /** 开始生成 */
        GENERATING,
        /** 生成成功 */
        COMPLETED,
        /** 生成失败 */
        FAILED,
        /** 跳过（已存在） */
        SKIPPED,
        /** 被取消 */
        CANCELLED
    }
}
