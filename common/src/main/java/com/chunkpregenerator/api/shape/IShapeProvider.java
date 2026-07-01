package com.chunkpregenerator.api.shape;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * 区域形状提供者接口。
 *
 * <p>定义了世界区块的选择形状。支持多种形状：
 * <ul>
 *   <li>矩形 — 由两个角点定义</li>
 *   <li>圆形 — 由圆心和半径定义</li>
 *   <li>自定义多边形 — 由顶点列表定义</li>
 * </ul>
 *
 * <p>所有实现必须提供 {@link #iterator()} 来迭代形状覆盖的所有区块。
 * 迭代顺序应尽量优化以减少区块加载时的磁盘寻道（推荐螺旋顺序）。
 *
 * @see RectangularShape
 * @see CircularShape
 */
public interface IShapeProvider extends Iterable<ChunkPos> {

    /**
     * 获取形状类型。
     */
    @Nonnull
    ShapeType getType();

    /**
     * 检查给定区块是否在此形状范围内。
     *
     * <p>用于运行时判断，比迭代器检查更快。
     *
     * @param pos 区块位置
     * @return true 如果在该形状内
     */
    boolean contains(@Nonnull ChunkPos pos);

    /**
     * 获取形状覆盖的总区块数量（估算）。
     *
     * @return 估算的区块数
     */
    int estimatedSize();

    /**
     * 获取形状的中心区块坐标。
     */
    @Nonnull
    ChunkPos getCenter();

    /**
     * 形状类型枚举。
     */
    enum ShapeType {
        RECTANGLE,
        CIRCLE,
        POLYGON,
        /** 整个世界的所有已加载区块 */
        WORLD_BORDER
    }
}
