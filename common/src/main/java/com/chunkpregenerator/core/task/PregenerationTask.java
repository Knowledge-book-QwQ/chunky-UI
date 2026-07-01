package com.chunkpregenerator.core.task;

import com.chunkpregenerator.api.IPregenerationTask;
import com.chunkpregenerator.api.shape.ChunkPos;
import com.chunkpregenerator.api.shape.IShapeProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 预生成任务的默认实现。
 *
 * <p><b>设计要点：</b>
 * <ul>
 *   <li>使用 {@link ConcurrentHashMap} 追踪已完成区块，支持高并发标记</li>
 *   <li>任务状态使用 volatile + 锁保证可见性和原子性</li>
 *   <li>支持序列化为 JSON 以便暂停恢复</li>
 * </ul>
 *
 * <p>线程安全：本类所有公共方法都是线程安全的。
 */
@ThreadSafe
public final class PregenerationTask implements IPregenerationTask {

    private final UUID taskId;
    private final IShapeProvider shapeProvider;
    private final boolean forceRegenerate;

    // 使用 ConcurrentHashMap 作为并发安全的 Set
    // key = chunkPos.toString()，value = ChunkStatus
    @GuardedBy("itself")
    private final Map<String, ChunkStatus> chunkStatusMap = new ConcurrentHashMap<>();

    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger totalCount;

    private volatile TaskState state = TaskState.PENDING;
    private final Lock stateLock = new ReentrantLock();

    private final long createdAt;

    /**
     * 创建预生成任务。
     *
     * @param taskId           唯一标识
     * @param shapeProvider    区域形状
     * @param forceRegenerate  是否强制重新生成
     */
    public PregenerationTask(@Nonnull UUID taskId, @Nonnull IShapeProvider shapeProvider,
                              boolean forceRegenerate) {
        this.taskId = Objects.requireNonNull(taskId, "taskId must not be null");
        this.shapeProvider = Objects.requireNonNull(shapeProvider, "shapeProvider must not be null");
        this.forceRegenerate = forceRegenerate;
        this.totalCount = new AtomicInteger(shapeProvider.estimatedSize());
        this.createdAt = System.currentTimeMillis();
    }

    // ========== 状态管理 ==========

    @Override
    @Nonnull
    public UUID getTaskId() {
        return taskId;
    }

    @Override
    @Nonnull
    public Iterator<ChunkPos> getChunkPositions() {
        // 返回过滤后的迭代器：跳过已完成和失败的区块（除非强制重新生成）
        Iterator<ChunkPos> base = shapeProvider.iterator();
        return new Iterator<>() {
            private ChunkPos nextPos = null;

            @Override
            public boolean hasNext() {
                while (base.hasNext()) {
                    ChunkPos pos = base.next();
                    if (!isCompleted(pos) || forceRegenerate) {
                        nextPos = pos;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public ChunkPos next() {
                if (nextPos == null && !hasNext()) {
                    throw new NoSuchElementException("No more uncompleted chunks");
                }
                ChunkPos result = nextPos;
                nextPos = null;
                return result;
            }
        };
    }

    @Override
    public void markCompleted(@Nonnull ChunkPos pos) {
        String key = posKey(pos);
        // putIfAbsent 保证不会重复计数
        if (chunkStatusMap.put(key, ChunkStatus.COMPLETED) == null) {
            completedCount.incrementAndGet();
        }
    }

    @Override
    public void markFailed(@Nonnull ChunkPos pos, @Nullable String reason) {
        String key = posKey(pos);
        if (chunkStatusMap.put(key, ChunkStatus.FAILED) == null) {
            failedCount.incrementAndGet();
        }
    }

    @Override
    public boolean isCompleted(@Nonnull ChunkPos pos) {
        return chunkStatusMap.containsKey(posKey(pos));
    }

    @Override
    public int getCompletedCount() {
        return completedCount.get();
    }

    @Override
    public int getFailedCount() {
        return failedCount.get();
    }

    @Override
    public int getTotalCount() {
        return totalCount.get();
    }

    @Override
    public int getRemainingCount() {
        return totalCount.get() - completedCount.get() - failedCount.get();
    }

    @Override
    @Nonnull
    public IShapeProvider getShapeProvider() {
        return shapeProvider;
    }

    @Override
    @Nonnull
    public TaskState getState() {
        return state;
    }

    /**
     * 设置任务状态。由引擎在状态转换时调用。
     */
    public void setState(@Nonnull TaskState newState) {
        stateLock.lock();
        try {
            TaskState oldState = this.state;
            if (!isValidTransition(oldState, newState)) {
                throw new IllegalStateException(
                    "Invalid state transition: " + oldState + " -> " + newState);
            }
            this.state = newState;
        } finally {
            stateLock.unlock();
        }
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    // ========== 序列化支持 ==========

    /**
     * 将任务状态序列化为 JSON 友好的 Map。
     */
    @Nonnull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskId", taskId.toString());
        data.put("completedCount", completedCount.get());
        data.put("failedCount", failedCount.get());
        data.put("totalCount", totalCount.get());
        data.put("state", state.name());
        data.put("createdAt", createdAt);
        data.put("forceRegenerate", forceRegenerate);

        // 序列化已完成区块列表（用于恢复时跳过）
        List<String> completedChunks = new ArrayList<>();
        chunkStatusMap.forEach((key, status) -> {
            if (status == ChunkStatus.COMPLETED || status == ChunkStatus.FAILED) {
                completedChunks.add(key);
            }
        });
        data.put("completedChunks", completedChunks);
        return data;
    }

    // ========== 内部方法 ==========

    private static String posKey(ChunkPos pos) {
        return pos.x() + "," + pos.z();
    }

    private static boolean isValidTransition(TaskState from, TaskState to) {
        return switch (from) {
            case PENDING -> to == TaskState.RUNNING || to == TaskState.CANCELLED;
            case RUNNING -> to == TaskState.PAUSED || to == TaskState.COMPLETED
                || to == TaskState.FAILED || to == TaskState.CANCELLED;
            case PAUSED -> to == TaskState.RUNNING || to == TaskState.CANCELLED;
            case COMPLETED, FAILED, CANCELLED -> false; // 终态
        };
    }

    /**
     * 内部区块状态枚举。
     */
    private enum ChunkStatus {
        COMPLETED,
        FAILED
    }
}
