package com.chunkpregenerator.ui.screen;

import com.chunkpregenerator.api.IPregenerationEngine;
import com.chunkpregenerator.api.shape.ChunkPos;
import com.chunkpregenerator.api.shape.IShapeProvider;
import com.chunkpregenerator.ui.renderer.ChunkHeatmapRenderer;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * 区块预生成主屏幕的抽象接口。
 *
 * <p>定义跨平台 UI 屏幕的生命周期和交互契约。
 * 各平台（NeoForge/Forge/Fabric）实现此接口以渲染具体的屏幕。
 *
 * <p>屏幕布局：
 * <pre>
 * ┌──────────────────────────────┐
 * │  [状态栏] 进度: 45%  ETA: 2m  │
 * ├──────────────────────────────┤
 * │                              │
 * │     [区块热力地图]           │
 * │     显示已生成/待生成区块    │
 * │                              │
 * ├──────────────────────────────┤
 * │  [控制面板]                  │
 * │  形状: █ 圆形 ○ 矩形 □      │
 * │  半径: [===50===]           │
 * │  [开始] [暂停] [取消]       │
 * └──────────────────────────────┘
 * </pre>
 */
public interface IPregenerationScreen {

    /**
     * 屏幕初始化回调。
     *
     * <p>在此方法中设置 UI 组件的初始状态，
     * 绑定事件处理器，注册引擎回调。
     */
    void init();

    /**
     * 每帧渲染回调。
     *
     * <p>UI 系统每帧调用此方法更新屏幕内容。
     * 应在此处更新进度条、热力地图等动态元素。
     *
     * @param partialTick 部分刻时间（用于平滑动画）
     */
    void render(float partialTick);

    /**
     * 当屏幕关闭或被覆盖时调用。
     *
     * <p>清理资源、取消注册的回调等。
     */
    void onClose();

    /**
     * 获取屏幕关联的引擎引用。
     */
    @Nonnull
    IPregenerationEngine getEngine();

    /**
     * 获取热力图渲染器。
     */
    @Nonnull
    ChunkHeatmapRenderer getHeatmapRenderer();

    /**
     * 获取当前选中的区域形状。
     */
    @Nonnull
    Optional<IShapeProvider> getSelectedShape();

    /**
     * 设置选中的区域形状。
     */
    void setSelectedShape(@Nonnull IShapeProvider shape);

    /**
     * 屏幕状态枚举。
     */
    enum ScreenState {
        /** 配置模式：用户设置区域和参数 */
        CONFIGURING,
        /** 正在生成区块 */
        GENERATING,
        /** 已暂停 */
        PAUSED,
        /** 生成完成 */
        COMPLETED,
        /** 出现错误 */
        ERROR
    }
}
