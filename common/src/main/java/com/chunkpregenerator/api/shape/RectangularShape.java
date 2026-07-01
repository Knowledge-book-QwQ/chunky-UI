package com.chunkpregenerator.api.shape;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 矩形区域形状提供者。
 *
 * <p>由两个角点 (minX, minZ) 和 (maxX, maxZ) 定义的矩形区域。
 * 包含边界。区块以螺旋顺序迭代，优化磁盘加载性能。
 *
 * <p><b>螺旋顺序的好处：</b>从中心向外螺旋扫描可以减少
 * 区块文件读取时的磁盘寻道时间，因为连续区块的
 * 数据在磁盘上物理相邻的可能性更高。
 */
public final class RectangularShape implements IShapeProvider {

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final int sizeX;
    private final int sizeZ;
    private final ChunkPos center;

    private RectangularShape(int minX, int minZ, int maxX, int maxZ) {
        // 规范化：确保 min < max
        this.minX = Math.min(minX, maxX);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxZ = Math.max(minZ, maxZ);
        this.sizeX = this.maxX - this.minX + 1;
        this.sizeZ = this.maxZ - this.minZ + 1;
        this.center = ChunkPos.of(
            this.minX + this.sizeX / 2,
            this.minZ + this.sizeZ / 2
        );
    }

    /**
     * 创建矩形形状。
     *
     * @param x1 第一个角点 X
     * @param z1 第一个角点 Z
     * @param x2 第二个角点 X
     * @param z2 第二个角点 Z
     * @return 矩形形状实例
     */
    @Nonnull
    public static RectangularShape of(int x1, int z1, int x2, int z2) {
        return new RectangularShape(x1, z1, x2, z2);
    }

    /**
     * 以中心点和半径创建正方形。
     *
     * @param centerX 中心 X
     * @param centerZ 中心 Z
     * @param radius  半径（区块数）
     * @return 正方形形状
     */
    @Nonnull
    public static RectangularShape squareCentered(int centerX, int centerZ, int radius) {
        return new RectangularShape(
            centerX - radius, centerZ - radius,
            centerX + radius, centerZ + radius
        );
    }

    @Override
    @Nonnull
    public ShapeType getType() {
        return ShapeType.RECTANGLE;
    }

    @Override
    public boolean contains(@Nonnull ChunkPos pos) {
        return pos.x() >= minX && pos.x() <= maxX
            && pos.z() >= minZ && pos.z() <= maxZ;
    }

    @Override
    public int estimatedSize() {
        return sizeX * sizeZ;
    }

    @Override
    @Nonnull
    public ChunkPos getCenter() {
        return center;
    }

    public int minX() { return minX; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxZ() { return maxZ; }

    /**
     * 以螺旋顺序迭代区块。
     *
     * <p>从中心开始向外顺时针螺旋，减少磁盘寻道。
     */
    @Override
    @Nonnull
    public Iterator<ChunkPos> iterator() {
        return new SpiralIterator(center, sizeX, sizeZ, minX, minZ, maxX, maxZ);
    }

    private static class SpiralIterator implements Iterator<ChunkPos> {
        private int currentX;
        private int currentZ;
        private int stepSize = 1;
        private int stepCount = 0;
        private int directionIndex = 0;
        private boolean started = false;
        private boolean hasNext = true;

        // 方向：右、下、左、上
        private static final int[] DX = {1, 0, -1, 0};
        private static final int[] DZ = {0, 1, 0, -1};

        private final int minX, minZ, maxX, maxZ;
        private final int sizeX, sizeZ;
        private int visitedCount = 0;

        SpiralIterator(ChunkPos start, int sizeX, int sizeZ,
                       int minX, int minZ, int maxX, int maxZ) {
            this.currentX = start.x();
            this.currentZ = start.z();
            this.sizeX = sizeX;
            this.sizeZ = sizeZ;
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }

        @Override
        public boolean hasNext() {
            return hasNext && visitedCount < sizeX * sizeZ;
        }

        @Override
        public ChunkPos next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more chunks in shape");
            }

            if (!started) {
                started = true;
                visitedCount++;
                return ChunkPos.of(currentX, currentZ);
            }

            // 螺旋步进
            ChunkPos pos;
            do {
                if (directionIndex % 2 == 0) stepSize++;
                // 奇数次方向变更后增加步长
                if (stepCount < stepSize) {
                    stepCount++;
                } else {
                    stepCount = 1;
                    directionIndex = (directionIndex + 1) % 4;
                    if (directionIndex % 2 == 0) stepSize++;
                }
                currentX += DX[directionIndex];
                currentZ += DZ[directionIndex];

                // 如果超出边界，尝试找下一个在范围内的点
                if (currentX < minX || currentX > maxX ||
                    currentZ < minZ || currentZ > maxZ) {
                    pos = null;
                    continue;
                }
                pos = ChunkPos.of(currentX, currentZ);
                break;
            } while (true);

            visitedCount++;
            if (visitedCount >= sizeX * sizeZ) {
                hasNext = false;
            }
            return pos;
        }
    }

    @Override
    public String toString() {
        return "RectangularShape[" + minX + "," + minZ + " -> " + maxX + "," + maxZ + "]";
    }
}
