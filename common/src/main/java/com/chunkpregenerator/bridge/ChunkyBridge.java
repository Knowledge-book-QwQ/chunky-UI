package com.chunkpregenerator.bridge;

import com.chunkpregenerator.model.PregenerationConfig;
import com.chunkpregenerator.model.ProgressSnapshot;
import java.util.function.Consumer;

/**
 * 与Chunky模组通信的桥接接口。
 *
 * 实现策略：
 * 1. 优先通过ChunkyAPI接口（反射加载）直接调用
 * 2. 回退到执行 /chunky 命令并通过事件监听获取进度
 *
 * 各平台（NeoForge/Forge/Fabric）提供各自的实现。
 */
public interface ChunkyBridge {

    /**
     * 检查Chunky是否可用。
     *
     * @return true 如果Chunky模组已安装且可通信
     */
    boolean isChunkyAvailable();

    /**
     * 检查指定世界是否有生成任务在运行。
     *
     * @param worldName 世界名称
     * @return true 如果该世界有活跃任务
     */
    boolean isRunning(String worldName);

    /**
     * 启动预生成任务。
     *
     * @param config 预生成配置
     * @return true 如果任务成功启动
     */
    boolean startTask(PregenerationConfig config);

    /**
     * 暂停指定世界的生成任务。
     *
     * @param worldName 世界名称
     * @return true 如果成功暂停
     */
    boolean pauseTask(String worldName);

    /**
     * 继续指定世界的生成任务。
     *
     * @param worldName 世界名称
     * @return true 如果成功继续
     */
    boolean continueTask(String worldName);

    /**
     * 取消指定世界的生成任务。
     *
     * @param worldName 世界名称
     * @return true 如果成功取消
     */
    boolean cancelTask(String worldName);

    /**
     * 注册进度监听器。
     * 当Chunky推送GenerationProgressEvent时回调。
     *
     * @param listener 进度消费者
     */
    void onProgressUpdate(Consumer<ProgressSnapshot> listener);

    /**
     * 注册完成监听器。
     * 当Chunky推送GenerationCompleteEvent时回调。
     *
     * @param listener 完成消费者（参数为世界名称）
     */
    void onTaskComplete(Consumer<String> listener);

    /**
     * 获取当前任务的进度快照（用于UI初始化时查询）。
     *
     * @param worldName 世界名称
     * @return 当前进度快照，无任务时返回空快照
     */
    ProgressSnapshot getProgress(String worldName);
}
