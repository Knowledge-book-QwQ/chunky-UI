package com.chunkpregenerator.model;

/**
 * 预生成任务的实时进度快照。
 * 从Chunky的GenerationProgressEvent映射而来，UI层订阅此数据更新界面。
 *
 * @param worldName 世界名称
 * @param chunkCount 已生成区块数
 * @param totalChunks 总区块数
 * @param percentComplete 完成百分比 (0-100)
 * @param rate 生成速率（区块/秒）
 * @param hours 预估剩余小时
 * @param minutes 预估剩余分钟
 * @param seconds 预估剩余秒数
 * @param currentChunkX 当前处理区块X
 * @param currentChunkZ 当前处理区块Z
 * @param isComplete 是否已完成
 */
public record ProgressSnapshot(
        String worldName,
        long chunkCount,
        long totalChunks,
        float percentComplete,
        double rate,
        long hours,
        long minutes,
        long seconds,
        int currentChunkX,
        int currentChunkZ,
        boolean isComplete
) {
    /** 空进度（无任务运行时使用） */
    public static ProgressSnapshot empty(String worldName) {
        return new ProgressSnapshot(worldName, 0, 0, 0f, 0d, 0, 0, 0, 0, 0, false);
    }

    /** 已完成进度 */
    public static ProgressSnapshot completed(String worldName, long totalChunks) {
        return new ProgressSnapshot(worldName, totalChunks, totalChunks, 100f, 0d, 0, 0, 0, 0, 0, true);
    }

    /** 格式化预估剩余时间 */
    public String formattedTimeRemaining() {
        if (isComplete) {
            return "已完成";
        }
        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }

    /** 格式化速率 */
    public String formattedRate() {
        return String.format("%.1f 区块/秒", rate);
    }
}
