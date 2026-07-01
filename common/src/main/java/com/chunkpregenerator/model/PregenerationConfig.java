package com.chunkpregenerator.model;

/**
 * 预生成配置的不可变数据对象。
 * 所有参数由UI收集后构建此对象，传递给ChunkyBridge执行。
 *
 * @param worldName 目标世界名称（如 "minecraft:overworld"）
 * @param shapeType 形状类型
 * @param centerX 中心X坐标（方块坐标）
 * @param centerZ 中心Z坐标（方块坐标）
 * @param radiusX 主半径（方块数）
 * @param radiusZ 副半径（仅矩形/椭圆使用，方块数）
 */
public record PregenerationConfig(
        String worldName,
        ShapeType shapeType,
        double centerX,
        double centerZ,
        double radiusX,
        double radiusZ
) {
    /** 创建针对方形/圆形的配置（单一半径） */
    public static PregenerationConfig symmetric(
            String worldName, ShapeType shapeType,
            double centerX, double centerZ, double radius
    ) {
        return new PregenerationConfig(worldName, shapeType, centerX, centerZ, radius, radius);
    }

    /** 以玩家位置为中心创建配置 */
    public PregenerationConfig withCenter(double newCenterX, double newCenterZ) {
        return new PregenerationConfig(worldName, shapeType, newCenterX, newCenterZ, radiusX, radiusZ);
    }

    @Override
    public String toString() {
        return String.format("PregenerationConfig{world=%s, shape=%s, center=(%.0f, %.0f), radius=(%.0f, %.0f)}",
                worldName, shapeType.chunkyName(), centerX, centerZ, radiusX, radiusZ);
    }
}
