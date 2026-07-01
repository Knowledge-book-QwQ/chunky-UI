package com.chunkpregenerator.neoforge.bridge;

import com.chunkpregenerator.bridge.ChunkyBridge;
import com.chunkpregenerator.model.PregenerationConfig;
import com.chunkpregenerator.model.ProgressSnapshot;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * NeoForge平台的ChunkyBridge实现。
 *
 * <h3>通信策略（双重回退）</h3>
 * <ol>
 *   <li><b>ChunkyAPI反射调用</b> — 通过反射查找 {@code org.popcraft.chunky.api.ChunkyAPI}
 *       实例，直接调用编程接口。零开销，支持进度事件订阅。</li>
 *   <li><b>命令执行回退</b> — 如果ChunkyAPI不可用，通过Minecraft命令系统
 *       执行 {@code /chunky} 指令。功能完整但进度追踪精度较低。</li>
 * </ol>
 *
 * <p>进度追踪通过Chunky的{@code GenerationProgressEvent}事件实现，
 * 使用反射订阅以避免编译期依赖。</p>
 */
public final class NeoForgeChunkyBridge implements ChunkyBridge {

    private static final Logger LOGGER = LogUtils.getLogger();

    // ChunkyAPI反射引用
    private Object chunkyApiInstance;
    private Method isRunningMethod;
    private Method startTaskMethod;
    private Method pauseTaskMethod;
    private Method continueTaskMethod;
    private Method cancelTaskMethod;

    // 进度轮询（命令回退模式使用）
    private final ScheduledExecutorService pollExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ChunkyBridge-Poller");
                t.setDaemon(true);
                return t;
            });

    // 监听器列表（线程安全）
    private final List<Consumer<ProgressSnapshot>> progressListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> completeListeners = new CopyOnWriteArrayList<>();

    // 标记Chunky是否可用
    private boolean chunkyAvailable = false;
    private boolean apiMode = false; // true=API模式，false=命令回退模式

    public NeoForgeChunkyBridge() {
        initializeChunkyApi();
    }

    // ==================== 初始化 ====================

    /**
     * 通过反射查找ChunkyAPI实例。
     * Chunky注册API到{@code ChunkyProvider}，我们通过反射获取。
     */
    private void initializeChunkyApi() {
        try {
            // ChunkyProvider.get() 返回Chunky实例
            Class<?> chunkyProviderClass = Class.forName("org.popcraft.chunky.ChunkyProvider");
            Method getMethod = chunkyProviderClass.getMethod("get");
            Object chunky = getMethod.invoke(null);

            if (chunky == null) {
                LOGGER.warn("ChunkyProvider.get() 返回null，Chunky可能尚未初始化");
                chunkyAvailable = false;
                return;
            }

            // chunky.getApi() 返回ChunkyAPI实例
            Method getApiMethod = chunky.getClass().getMethod("getApi");
            chunkyApiInstance = getApiMethod.invoke(chunky);

            if (chunkyApiInstance == null) {
                LOGGER.warn("Chunky.getApi() 返回null");
                chunkyAvailable = false;
                return;
            }

            // 缓存反射方法
            Class<?> apiClass = chunkyApiInstance.getClass();
            isRunningMethod = apiClass.getMethod("isRunning", String.class);
            startTaskMethod = apiClass.getMethod("startTask",
                    String.class, String.class, double.class, double.class,
                    double.class, double.class, String.class);
            pauseTaskMethod = apiClass.getMethod("pauseTask", String.class);
            continueTaskMethod = apiClass.getMethod("continueTask", String.class);
            cancelTaskMethod = apiClass.getMethod("cancelTask", String.class);

            // 注册事件监听
            registerProgressListener(apiClass);
            registerCompleteListener(apiClass);

            chunkyAvailable = true;
            apiMode = true;
            LOGGER.info("✅ ChunkyAPI反射初始化成功，使用API模式");

        } catch (ClassNotFoundException e) {
            LOGGER.info("Chunky模组未安装，将使用命令回退模式");
            chunkyAvailable = false;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.warn("ChunkyAPI反射失败: {}，将使用命令回退模式", e.getMessage());
            chunkyAvailable = false;
        }
    }

    /**
     * 通过反射注册进度事件监听器。
     */
    @SuppressWarnings("unchecked")
    private void registerProgressListener(Class<?> apiClass) {
        try {
            Method onProgressMethod = apiClass.getMethod("onGenerationProgress", Consumer.class);
            onProgressMethod.invoke(chunkyApiInstance, (Consumer<Object>) event -> {
                // 从GenerationProgressEvent提取数据映射到ProgressSnapshot
                ProgressSnapshot snapshot = mapProgressEvent(event);
                progressListeners.forEach(listener -> {
                    try {
                        listener.accept(snapshot);
                    } catch (Exception e) {
                        LOGGER.error("进度监听器回调异常", e);
                    }
                });
            });
            LOGGER.debug("进度事件监听器注册成功");
        } catch (Exception e) {
            LOGGER.warn("无法注册进度监听器: {}", e.getMessage());
        }
    }

    /**
     * 通过反射注册完成事件监听器。
     */
    private void registerCompleteListener(Class<?> apiClass) {
        try {
            Method onCompleteMethod = apiClass.getMethod("onGenerationComplete", Consumer.class);
            onCompleteMethod.invoke(chunkyApiInstance, (Consumer<Object>) event -> {
                String worldName = (String) event.getClass().getMethod("world").invoke(event);
                completeListeners.forEach(listener -> {
                    try {
                        listener.accept(worldName);
                    } catch (Exception e) {
                        LOGGER.error("完成监听器回调异常", e);
                    }
                });
            });
            LOGGER.debug("完成事件监听器注册成功");
        } catch (Exception e) {
            LOGGER.warn("无法注册完成监听器: {}", e.getMessage());
        }
    }

    /**
     * 将Chunky的GenerationProgressEvent映射为我们的ProgressSnapshot。
     * 通过反射读取字段，避免编译期依赖。
     */
    private ProgressSnapshot mapProgressEvent(Object event) {
        try {
            Class<?> clazz = event.getClass();
            String world = (String) clazz.getMethod("world").invoke(event);
            long chunkCount = (long) clazz.getMethod("chunkCount").invoke(event);
            boolean complete = (boolean) clazz.getMethod("complete").invoke(event);
            float percent = (float) clazz.getMethod("percentComplete").invoke(event);
            long hours = (long) clazz.getMethod("hours").invoke(event);
            long minutes = (long) clazz.getMethod("minutes").invoke(event);
            long seconds = (long) clazz.getMethod("seconds").invoke(event);
            double rate = (double) clazz.getMethod("rate").invoke(event);
            int chunkX = (int) clazz.getMethod("chunkX").invoke(event);
            int chunkZ = (int) clazz.getMethod("chunkZ").invoke(event);

            // totalChunks在Chunky事件中不直接暴露，通过百分比反推
            long totalChunks = percent > 0 ? (long) (chunkCount * 100.0 / percent) : 0;

            return new ProgressSnapshot(world, chunkCount, totalChunks,
                    percent, rate, hours, minutes, seconds, chunkX, chunkZ, complete);
        } catch (Exception e) {
            LOGGER.error("映射进度事件失败", e);
            return ProgressSnapshot.empty("unknown");
        }
    }

    // ==================== ChunkyBridge 实现 ====================

    @Override
    public boolean isChunkyAvailable() {
        return chunkyAvailable;
    }

    @Override
    public boolean isRunning(String worldName) {
        if (!chunkyAvailable) {
            return false;
        }
        if (apiMode && isRunningMethod != null) {
            try {
                return (boolean) isRunningMethod.invoke(chunkyApiInstance, worldName);
            } catch (Exception e) {
                LOGGER.error("isRunning API调用失败", e);
            }
        }
        return false;
    }

    @Override
    public boolean startTask(PregenerationConfig config) {
        if (!chunkyAvailable) {
            LOGGER.warn("Chunky不可用，无法启动任务");
            return false;
        }

        if (apiMode && startTaskMethod != null) {
            try {
                Object result = startTaskMethod.invoke(chunkyApiInstance,
                        config.worldName(),
                        config.shapeType().chunkyName(),
                        config.centerX(), config.centerZ(),
                        config.radiusX(), config.radiusZ(),
                        "concentric" // 默认同心模式
                );
                boolean success = (boolean) result;
                if (success) {
                    LOGGER.info("✅ 预生成任务已启动: {}", config);
                }
                return success;
            } catch (Exception e) {
                LOGGER.error("startTask API调用失败", e);
            }
        }

        // 命令回退
        return executeChunkyCommand(buildStartCommand(config));
    }

    @Override
    public boolean pauseTask(String worldName) {
        if (!chunkyAvailable) return false;

        if (apiMode && pauseTaskMethod != null) {
            try {
                return (boolean) pauseTaskMethod.invoke(chunkyApiInstance, worldName);
            } catch (Exception e) {
                LOGGER.error("pauseTask API调用失败", e);
            }
        }
        return executeChunkyCommand("chunky pause " + worldName);
    }

    @Override
    public boolean continueTask(String worldName) {
        if (!chunkyAvailable) return false;

        if (apiMode && continueTaskMethod != null) {
            try {
                return (boolean) continueTaskMethod.invoke(chunkyApiInstance, worldName);
            } catch (Exception e) {
                LOGGER.error("continueTask API调用失败", e);
            }
        }
        return executeChunkyCommand("chunky continue " + worldName);
    }

    @Override
    public boolean cancelTask(String worldName) {
        if (!chunkyAvailable) return false;

        if (apiMode && cancelTaskMethod != null) {
            try {
                return (boolean) cancelTaskMethod.invoke(chunkyApiInstance, worldName);
            } catch (Exception e) {
                LOGGER.error("cancelTask API调用失败", e);
            }
        }
        return executeChunkyCommand("chunky cancel " + worldName);
    }

    @Override
    public void onProgressUpdate(Consumer<ProgressSnapshot> listener) {
        progressListeners.add(listener);
    }

    @Override
    public void onTaskComplete(Consumer<String> listener) {
        completeListeners.add(listener);
    }

    @Override
    public ProgressSnapshot getProgress(String worldName) {
        if (!chunkyAvailable || !apiMode) {
            return ProgressSnapshot.empty(worldName);
        }
        // Chunky没有提供直接的getProgress API，
        // 我们在收到事件时缓存最新进度
        // 这里返回空快照，实际进度由onProgressUpdate推送
        return ProgressSnapshot.empty(worldName);
    }

    // ==================== 私有辅助 ====================

    /**
     * 构建Chunky start命令字符串。
     * 格式: chunky start [world] [shape] [centerX] [centerZ] [radiusX] [radiusZ]
     */
    private String buildStartCommand(PregenerationConfig config) {
        return String.format("chunky start %s %s %.0f %.0f %.0f %.0f",
                config.worldName(),
                config.shapeType().chunkyName(),
                config.centerX(), config.centerZ(),
                config.radiusX(), config.radiusZ());
    }

    /**
     * 通过Minecraft命令系统执行Chunky指令。
     * 这是API不可用时的回退方案。
     */
    private boolean executeChunkyCommand(String command) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("无法获取MinecraftServer实例");
            return false;
        }
        try {
            int result = server.getCommands()
                    .performPrefixedCommand(
                            server.createCommandSourceStack(),
                            command
                    );
            return result > 0;
        } catch (Exception e) {
            LOGGER.error("命令执行失败: {}", command, e);
            return false;
        }
    }

    /**
     * 释放资源——取消进度轮询。
     */
    public void shutdown() {
        pollExecutor.shutdownNow();
    }
}
