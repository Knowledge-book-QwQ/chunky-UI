package com.chunkpregenerator.api.shape;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 区块在 Minecraft 世界中的二维坐标。
 *
 * <p>区块坐标 (chunkX, chunkZ) 与方块坐标的转换关系：
 * {@code chunkX = blockX >> 4}，{@code chunkZ = blockZ >> 4}。
 *
 * <p>这是一个不可变值对象（record 风格），
 * 使用 {@link #of(int, int)} 工厂方法创建。
 *
 * <p><b>为什么用类而不是 record：</b>为了兼容 Java 17 以下版本
 * （Forge 1.20.1 使用 Java 17）。
 */
public final class ChunkPos {

    private final int x;
    private final int z;

    private ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    /**
     * 创建区块坐标。推荐使用此工厂方法而非构造器。
     *
     * @param x 区块 X 坐标
     * @param z 区块 Z 坐标
     * @return 新的 ChunkPos 实例
     */
    @Nonnull
    public static ChunkPos of(int x, int z) {
        return new ChunkPos(x, z);
    }

    public int x() { return x; }
    public int z() { return z; }

    /**
     * 将方块坐标转换为区块坐标。
     *
     * @param blockX 方块 X 坐标
     * @param blockZ 方块 Z 坐标
     * @return 对应的区块坐标
     */
    @Nonnull
    public static ChunkPos fromBlockPos(int blockX, int blockZ) {
        return new ChunkPos(blockX >> 4, blockZ >> 4);
    }

    /**
     * 计算两个区块坐标之间的曼哈顿距离。
     */
    public int manhattanDistance(@Nonnull ChunkPos other) {
        return Math.abs(this.x - other.x) + Math.abs(this.z - other.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkPos chunkPos)) return false;
        return x == chunkPos.x && z == chunkPos.z;
    }

    @Override
    public int hashCode() {
        // 使用与 Minecraft 相同的哈希算法以保证一致性
        return 31 * x + z;
    }

    @Override
    public String toString() {
        return "ChunkPos(" + x + ", " + z + ")";
    }
}
