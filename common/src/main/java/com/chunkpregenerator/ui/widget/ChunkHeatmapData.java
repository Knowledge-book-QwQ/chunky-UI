package com.chunkpregenerator.ui.widget;

import com.chunkpregenerator.api.shape.ChunkPos;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * 区块热力地图数据模型。
 *
 * <p>存储二维网格中每个区块的生成状态，用于渲染热力图。
 * 使用字节数组存储状态以节省内存（每个区块仅1字节）。
 *
 * <p>内存布局：一个 256×256 的区域（半径128区块）仅需 64KB。
 * 这足够覆盖大多数预生成场景。
 *
 * <p><b>为什么用 byte 数组而不是 Enum/Integer：</b>
 * 热力图需要在内存中常驻，区块数量可达数万甚至数十万。
 * 使用 byte 比 Enum[] 节省约 75% 内存。
 */
public final class ChunkHeatmapData {

    // 区块状态常量
    public static final byte STATE_UNLOADED = 0;
    public static final byte STATE_LOADING = 1;
    public static final byte STATE_GENERATED = 2;
    public static final byte STATE_FAILED = 3;
    public static final byte STATE_SKIPPED = 4;

    private final int offsetX;
    private final int offsetZ;
    private final int sizeX;
    private final int sizeZ;
    private final byte[] data;

    /**
     * 创建热力图数据。
     *
     * @param centerX 中心 X 坐标
     * @param centerZ 中心 Z 坐标
     * @param radius  半径（区块数）
     */
    public ChunkHeatmapData(int centerX, int centerZ, int radius) {
        this.sizeX = 2 * radius + 1;
        this.sizeZ = 2 * radius + 1;
        this.offsetX = centerX - radius;
        this.offsetZ = centerZ - radius;
        this.data = new byte[sizeX * sizeZ];
    }

    /**
     * 设置指定区块的状态。
     */
    public void setState(int chunkX, int chunkZ, byte state) {
        int idx = indexOf(chunkX, chunkZ);
        if (idx >= 0 && idx < data.length) {
            data[idx] = state;
        }
    }

    /**
     * 获取指定区块的状态。
     *
     * @param chunkX 区块 X 坐标
     * @param chunkZ 区块 Z 坐标
     * @return 区块状态，如果不在范围内返回 STATE_UNLOADED
     */
    public byte getState(int chunkX, int chunkZ) {
        int idx = indexOf(chunkX, chunkZ);
        if (idx >= 0 && idx < data.length) {
            return data[idx];
        }
        return STATE_UNLOADED;
    }

    /**
     * 批量更新区块状态（用于从进度回调批量刷新）。
     */
    public void batchUpdate(@Nonnull ChunkPos[] positions, byte state) {
        for (ChunkPos pos : positions) {
            setState(pos.x(), pos.z(), state);
        }
    }

    /**
     * 获取中心区块坐标。
     */
    public int centerX() {
        return offsetX + sizeX / 2;
    }

    public int centerZ() {
        return offsetZ + sizeZ / 2;
    }

    /**
     * 获取视野范围（区块数）。
     */
    public int viewRadius() {
        return sizeX / 2;
    }

    /**
     * 计算指定区块在 data 数组中的索引。
     */
    private int indexOf(int chunkX, int chunkZ) {
        int localX = chunkX - offsetX;
        int localZ = chunkZ - offsetZ;
        if (localX < 0 || localX >= sizeX || localZ < 0 || localZ >= sizeZ) {
            return -1;
        }
        return localZ * sizeX + localX;
    }

    /**
     * 统计各种状态的数量。
     */
    @Nonnull
    public StateCounts countStates() {
        int generated = 0, failed = 0, loading = 0, unloaded = 0;
        for (byte b : data) {
            switch (b) {
                case STATE_GENERATED -> generated++;
                case STATE_FAILED -> failed++;
                case STATE_LOADING -> loading++;
                default -> unloaded++;
            }
        }
        return new StateCounts(generated, failed, loading, unloaded);
    }

    /**
     * 状态统计值对象。
     */
    public record StateCounts(int generated, int failed, int loading, int unloaded) {
        public int total() { return generated + failed + loading + unloaded; }
    }
}
