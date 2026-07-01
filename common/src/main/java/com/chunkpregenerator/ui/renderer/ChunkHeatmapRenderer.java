package com.chunkpregenerator.ui.renderer;

import com.chunkpregenerator.api.shape.ChunkPos;
import com.chunkpregenerator.ui.widget.ChunkHeatmapData;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 区块热力图渲染器。
 *
 * <p>将 {@link ChunkHeatmapData} 中的状态数据映射为可视化颜色。
 * 支持两种渲染模式：
 * <ul>
 *   <li><b>独立区块模式：</b>每个区块渲染为带边框的矩形</li>
 *   <li><b>热力模式：</b>通过颜色渐变显示密度分布</li>
 * </ul>
 *
 * <p>颜色映射：
 * <pre>
 *  未加载 → 深灰 (#404040)
 *  加载中 → 黄色 (#FFD700)
 *  已生成 → 绿色 (#00AA00)
 *  失败   → 红色 (#FF4444)
 *  跳过   → 浅灰 (#808080)
 * </pre>
 */
public final class ChunkHeatmapRenderer {

    // ARGB 颜色常量（预计算，避免每帧计算）
    private static final int COLOR_UNLOADED  = 0xFF404040;
    private static final int COLOR_LOADING   = 0xFFFFD700;
    private static final int COLOR_GENERATED = 0xFF00AA00;
    private static final int COLOR_FAILED    = 0xFFFF4444;
    private static final int COLOR_SKIPPED   = 0xFF808080;
    private static final int COLOR_GRID_LINE = 0xFF555555;

    // 渲染配置
    private int chunkPixelSize = 4;        // 每个区块占用的像素数
    private boolean showGrid = true;       // 是否显示网格线
    private boolean showCoordinates = false; // 是否显示坐标

    private int viewX = 0;    // 当前视口中心 X
    private int viewZ = 0;    // 当前视口中心 Z
    private int zoomLevel = 0; // 缩放级别（0=默认）

    /**
     * 获取区块在某缩放级别下的像素大小。
     */
    public int getChunkPixelSize() {
        return Math.max(1, chunkPixelSize + zoomLevel);
    }

    /**
     * 将区块坐标转换为屏幕坐标（相对于视口中心）。
     *
     * @param chunkX    区块 X
     * @param chunkZ    区块 Z
     * @param centerX   屏幕中心 X
     * @param centerY   屏幕中心 Y
     * @param pixelSize 每个区块的像素大小
     * @return 包含 (screenX, screenY) 的数组
     */
    @Nonnull
    public int[] chunkToScreen(int chunkX, int chunkZ,
                                int centerX, int centerY, int pixelSize) {
        int dx = (chunkX - viewX) * (pixelSize + (showGrid ? 1 : 0));
        int dz = (chunkZ - viewZ) * (pixelSize + (showGrid ? 1 : 0));
        return new int[]{centerX + dx, centerY + dz};
    }

    /**
     * 将屏幕坐标转换为区块坐标。
     *
     * @param screenX   屏幕 X
     * @param screenY   屏幕 Y
     * @param centerX   屏幕中心 X
     * @param centerY   屏幕中心 Y
     * @return 对应的区块坐标
     */
    @Nonnull
    public ChunkPos screenToChunk(int screenX, int screenY,
                                   int centerX, int centerY) {
        int pixelSize = getChunkPixelSize();
        int step = pixelSize + (showGrid ? 1 : 0);
        int dx = screenX - centerX;
        int dz = screenY - centerY;
        return ChunkPos.of(
            viewX + dx / step,
            viewZ + dz / step
        );
    }

    /**
     * 获取区块状态对应的颜色。
     */
    public int getColorForState(byte state) {
        return switch (state) {
            case ChunkHeatmapData.STATE_UNLOADED -> COLOR_UNLOADED;
            case ChunkHeatmapData.STATE_LOADING -> COLOR_LOADING;
            case ChunkHeatmapData.STATE_GENERATED -> COLOR_GENERATED;
            case ChunkHeatmapData.STATE_FAILED -> COLOR_FAILED;
            case ChunkHeatmapData.STATE_SKIPPED -> COLOR_SKIPPED;
            default -> COLOR_UNLOADED;
        };
    }

    /**
     * 热力增强模式：根据区块密度生成渐变色。
     *
     * <p>适用于高缩放级别下查看整体进度。
     *
     * @param data        热力图数据
     * @param chunkX      区块 X
     * @param chunkZ      区块 Z
     * @param sampleRadius 采样半径
     * @return ARGB 颜色
     */
    public int getDensityColor(@Nonnull ChunkHeatmapData data,
                                int chunkX, int chunkZ, int sampleRadius) {
        int total = 0;
        int generated = 0;

        for (int dx = -sampleRadius; dx <= sampleRadius; dx++) {
            for (int dz = -sampleRadius; dz <= sampleRadius; dz++) {
                byte state = data.getState(chunkX + dx, chunkZ + dz);
                total++;
                if (state == ChunkHeatmapData.STATE_GENERATED) {
                    generated++;
                }
            }
        }

        float ratio = (float) generated / total;
        if (ratio == 0.0f) return COLOR_UNLOADED;
        if (ratio == 1.0f) return COLOR_GENERATED;

        // 插值：深灰 → 绿
        int r = (int) (0x40 + (0x00 - 0x40) * ratio);
        int g = (int) (0x40 + (0xAA - 0x40) * ratio);
        int b = (int) (0x40 + (0x00 - 0x40) * ratio);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ========== 配置方法 ==========

    public void setShowGrid(boolean showGrid) { this.showGrid = showGrid; }
    public boolean isShowGrid() { return showGrid; }

    public void setZoomLevel(int zoomLevel) { this.zoomLevel = zoomLevel; }
    public int getZoomLevel() { return zoomLevel; }

    public void setViewCenter(int viewX, int viewZ) {
        this.viewX = viewX;
        this.viewZ = viewZ;
    }

    public int getViewX() { return viewX; }
    public int getViewZ() { return viewZ; }

    public void pan(int deltaX, int deltaZ) {
        int step = getChunkPixelSize() + (showGrid ? 1 : 0);
        this.viewX += deltaX / step;
        this.viewZ += deltaZ / step;
    }

    public int getGridLineColor() { return COLOR_GRID_LINE; }
}
