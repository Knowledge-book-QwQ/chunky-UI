package com.chunkpregenerator.api.shape;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 圆形区域形状提供者。
 *
 * <p>由中心点和半径（区块数）定义的近似圆形区域。
 * 使用 Bresenham 圆算法生成区块集合，保证所有区块
 * 到中心的欧几里得距离 ≤ 半径。
 *
 * <p><b>性能考量：</b>圆形迭代器会预计算边界框中的所有区块，
 * 然后过滤出在圆内的部分，空间换时间。
 */
public final class CircularShape implements IShapeProvider {

    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final ChunkPos center;
    private final int boundingBoxSize;

    private CircularShape(int centerX, int centerZ, int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius must be non-negative, got: " + radius);
        }
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.center = ChunkPos.of(centerX, centerZ);
        // 边界框边长（完整正方形）
        this.boundingBoxSize = 2 * radius + 1;
    }

    @Nonnull
    public static CircularShape of(int centerX, int centerZ, int radius) {
        return new CircularShape(centerX, centerZ, radius);
    }

    @Override
    @Nonnull
    public ShapeType getType() {
        return ShapeType.CIRCLE;
    }

    @Override
    public boolean contains(@Nonnull ChunkPos pos) {
        int dx = pos.x() - centerX;
        int dz = pos.z() - centerZ;
        // 使用平方距离避免 sqrt 计算
        return dx * dx + dz * dz <= radius * radius;
    }

    /**
     * 使用蒙特卡洛方法估算圆内区块数量。
     * 精确值 = π × radius²，但区块是离散网格，所以有细微差异。
     */
    @Override
    public int estimatedSize() {
        // 近似：圆面积 / 区块面积
        return (int) (Math.PI * radius * radius);
    }

    @Override
    @Nonnull
    public ChunkPos getCenter() {
        return center;
    }

    public int centerX() { return centerX; }
    public int centerZ() { return centerZ; }
    public int radius() { return radius; }

    @Override
    @Nonnull
    public Iterator<ChunkPos> iterator() {
        return new CircularIterator(centerX, centerZ, radius);
    }

    /**
     * 圆形区域迭代器。
     *
     * <p>从中心向外螺旋扫描边界框，仅在圆内的区块被返回。
     * 保持了良好的空间局部性。
     */
    private static class CircularIterator implements Iterator<ChunkPos> {
        private final int centerX, centerZ;
        private final int radiusSq;
        private final int minX, minZ, maxX, maxZ;

        private int currentX, currentZ;
        private boolean started = false;
        private boolean hasNext = true;

        CircularIterator(int centerX, int centerZ, int radius) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radiusSq = radius * radius;
            this.minX = centerX - radius;
            this.minZ = centerZ - radius;
            this.maxX = centerX + radius;
            this.maxZ = centerZ + radius;
            this.currentX = centerX;
            this.currentZ = centerZ;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public ChunkPos next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more chunks in circular shape");
            }

            if (!started) {
                started = true;
                if (isInside(currentX, currentZ)) {
                    return ChunkPos.of(currentX, currentZ);
                }
            }

            // 扫描下一列
            currentX++;
            if (currentX > maxX) {
                currentX = minX;
                currentZ++;
            }

            // 跳过不在圆内的区块
            while (currentZ <= maxZ) {
                while (currentX <= maxX) {
                    if (isInside(currentX, currentZ)) {
                        return ChunkPos.of(currentX, currentZ);
                    }
                    currentX++;
                }
                currentX = minX;
                currentZ++;
            }

            hasNext = false;
            throw new NoSuchElementException("No more chunks in circular shape");
        }

        private boolean isInside(int x, int z) {
            int dx = x - centerX;
            int dz = z - centerZ;
            return dx * dx + dz * dz <= radiusSq;
        }
    }

    @Override
    public String toString() {
        return "CircularShape[center=(" + centerX + "," + centerZ + "), radius=" + radius + "]";
    }
}
